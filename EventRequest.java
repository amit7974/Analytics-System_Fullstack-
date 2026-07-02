package com.example.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EventRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "eventType is required")
    private String eventType;

    /**
     * Arbitrary key/value metadata about the event, e.g. {"page": "/checkout", "device": "mobile"}
     */
    private Map<String, Object> metadata;
}
