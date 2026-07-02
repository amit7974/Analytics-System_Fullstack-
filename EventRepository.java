package com.example.analytics.repository;

import com.example.analytics.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndEventType(Long userId, String eventType);

    @Query("SELECT e.eventType AS eventType, COUNT(e) AS total " +
           "FROM Event e WHERE e.userId = :userId GROUP BY e.eventType ORDER BY total DESC")
    List<EventTypeCount> countEventsByTypeForUser(@Param("userId") Long userId);

    @Query("SELECT e.eventType AS eventType, COUNT(e) AS total " +
           "FROM Event e WHERE e.createdAt BETWEEN :from AND :to " +
           "GROUP BY e.eventType ORDER BY total DESC")
    List<EventTypeCount> countEventsByTypeInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT e.userId AS userId, COUNT(e) AS total " +
           "FROM Event e WHERE e.createdAt BETWEEN :from AND :to " +
           "GROUP BY e.userId ORDER BY total DESC")
    List<UserEventCount> countEventsByUserInRange(@Param("from") Instant from, @Param("to") Instant to);

    interface EventTypeCount {
        String getEventType();
        Long getTotal();
    }

    interface UserEventCount {
        Long getUserId();
        Long getTotal();
    }
}
