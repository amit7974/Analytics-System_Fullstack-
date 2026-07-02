package com.example.analytics.controller;

import com.example.analytics.dto.EventAggregateResponse;
import com.example.analytics.dto.UserAnalyticsResponse;
import com.example.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/users/{userId}")
    public UserAnalyticsResponse getUserAnalytics(@PathVariable Long userId) {
        return analyticsService.getUserAnalytics(userId);
    }

    @GetMapping("/events/aggregate")
    public EventAggregateResponse aggregate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return analyticsService.aggregate(from, to);
    }
}
