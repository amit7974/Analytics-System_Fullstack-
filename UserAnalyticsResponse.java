package com.example.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class UserAnalyticsResponse {
    private Long userId;
    private long totalEvents;
    private Map<String, Long> eventsByType;
    private Instant firstEventAt;
    private Instant lastEventAt;
    private List<EventResponse> recentEvents;
}
