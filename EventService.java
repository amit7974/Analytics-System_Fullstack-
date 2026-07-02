package com.example.analytics.service;

import com.example.analytics.dto.EventRequest;
import com.example.analytics.dto.EventResponse;
import com.example.analytics.exception.ResourceNotFoundException;
import com.example.analytics.model.AppUser;
import com.example.analytics.model.Event;
import com.example.analytics.repository.AppUserRepository;
import com.example.analytics.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository,
                         AppUserRepository appUserRepository,
                         ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventResponse trackEvent(EventRequest request) {
        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with id " + request.getUserId()));

        Event event = new Event();
        event.setUserId(user.getId());
        event.setEventType(request.getEventType());
        event.setMetadata(toJson(request.getMetadata()));

        Event saved = eventRepository.save(event);
        return toResponse(saved);
    }

    public EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .metadata(fromJson(event.getMetadata()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid metadata payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
