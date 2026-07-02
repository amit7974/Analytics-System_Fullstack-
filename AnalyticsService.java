package com.example.analytics.service;

import com.example.analytics.dto.EventAggregateResponse;
import com.example.analytics.dto.EventResponse;
import com.example.analytics.dto.UserAnalyticsResponse;
import com.example.analytics.exception.ResourceNotFoundException;
import com.example.analytics.model.Event;
import com.example.analytics.repository.AppUserRepository;
import com.example.analytics.repository.EventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final int RECENT_EVENTS_LIMIT = 20;

    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final EventService eventService;

    public AnalyticsService(EventRepository eventRepository,
                             AppUserRepository appUserRepository,
                             EventService eventService) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.eventService = eventService;
    }

    public UserAnalyticsResponse getUserAnalytics(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ResourceNotFoundException("No user found with id " + userId);
        }

        long totalEvents = eventRepository.countByUserId(userId);

        Map<String, Long> eventsByType = eventRepository.countEventsByTypeForUser(userId).stream()
                .collect(Collectors.toMap(
                        EventRepository.EventTypeCount::getEventType,
                        EventRepository.EventTypeCount::getTotal,
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<Event> recent = eventRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, RECENT_EVENTS_LIMIT))
                .getContent();

        List<EventResponse> recentResponses = recent.stream()
                .map(eventService::toResponse)
                .collect(Collectors.toList());

        Instant lastEventAt = recent.isEmpty() ? null : recent.get(0).getCreatedAt();
        Instant firstEventAt = recent.isEmpty() ? null : recent.get(recent.size() - 1).getCreatedAt();

        return UserAnalyticsResponse.builder()
                .userId(userId)
                .totalEvents(totalEvents)
                .eventsByType(eventsByType)
                .firstEventAt(firstEventAt)
                .lastEventAt(lastEventAt)
                .recentEvents(recentResponses)
                .build();
    }

    /**
     * Aggregates events across all users within [from, to]. If either bound is omitted,
     * defaults to the last 30 days up to now.
     */
    public EventAggregateResponse aggregate(Instant from, Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);

        Map<String, Long> eventsByType = eventRepository
                .countEventsByTypeInRange(effectiveFrom, effectiveTo).stream()
                .collect(Collectors.toMap(
                        EventRepository.EventTypeCount::getEventType,
                        EventRepository.EventTypeCount::getTotal,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<Long, Long> eventsByUser = eventRepository
                .countEventsByUserInRange(effectiveFrom, effectiveTo).stream()
                .collect(Collectors.toMap(
                        EventRepository.UserEventCount::getUserId,
                        EventRepository.UserEventCount::getTotal,
                        (a, b) -> a,
                        LinkedHashMap::new));

        long totalEvents = eventsByType.values().stream().mapToLong(Long::longValue).sum();

        return EventAggregateResponse.builder()
                .from(effectiveFrom)
                .to(effectiveTo)
                .totalEvents(totalEvents)
                .eventsByType(eventsByType)
                .eventsByUser(eventsByUser)
                .build();
    }
}
