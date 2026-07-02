package com.example.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_user_id", columnList = "user_id"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Free-form JSON payload describing the event (page, device, custom properties, etc).
     * Stored as TEXT for portability across MySQL versions.
     */
    @Lob
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
