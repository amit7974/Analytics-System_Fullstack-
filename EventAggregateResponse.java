package com.example.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class EventAggregateResponse {
    private Instant from;
    private Instant to;
    private long totalEvents;
    private Map<String, Long> eventsByType;
    private Map<Long, Long> eventsByUser;
}
