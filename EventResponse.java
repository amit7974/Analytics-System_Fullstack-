package com.example.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private Long userId;
    private String eventType;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
