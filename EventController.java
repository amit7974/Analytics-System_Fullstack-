package com.example.analytics.controller;

import com.example.analytics.dto.EventRequest;
import com.example.analytics.dto.EventResponse;
import com.example.analytics.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> trackEvent(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.trackEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
