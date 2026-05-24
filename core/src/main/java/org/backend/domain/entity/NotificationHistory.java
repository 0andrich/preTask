package org.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_request_id", nullable = false)
    private Long notificationRequestId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SENT | FAILED

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        this.attemptedAt = LocalDateTime.now();
    }

    public static NotificationHistory success(Long requestId, int attemptNumber) {
        NotificationHistory h = new NotificationHistory();
        h.notificationRequestId = requestId;
        h.attemptNumber = attemptNumber;
        h.status = "SENT";
        return h;
    }

    public static NotificationHistory failure(Long requestId, int attemptNumber, String reason) {
        NotificationHistory h = new NotificationHistory();
        h.notificationRequestId = requestId;
        h.attemptNumber = attemptNumber;
        h.status = "FAILED";
        h.failureReason = reason;
        return h;
    }
}
