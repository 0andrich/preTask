package org.backend.dto.response;

import org.backend.domain.entity.NotificationHistory;
import org.backend.domain.entity.NotificationRequest;
import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationStatus;
import org.backend.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationDetailResponse(
        Long id,
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        String referenceId,
        String referenceType,
        NotificationStatus status,
        int retryCount,
        int maxRetryCount,
        LocalDateTime retryAt,
        String failureReason,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        List<HistoryEntry> histories
) {
    public record HistoryEntry(
            int attemptNumber,
            String status,
            String failureReason,
            LocalDateTime attemptedAt
    ) {}

    public static NotificationDetailResponse from(NotificationRequest req, List<NotificationHistory> histories) {
        return new NotificationDetailResponse(
                req.getId(),
                req.getRecipientId(),
                req.getNotificationType(),
                req.getChannel(),
                req.getReferenceId(),
                req.getReferenceType(),
                req.getStatus(),
                req.getRetryCount(),
                req.getMaxRetryCount(),
                req.getRetryAt(),
                req.getFailureReason(),
                req.getScheduledAt(),
                req.getSentAt(),
                req.getCreatedAt(),
                histories.stream()
                        .map(h -> new HistoryEntry(h.getAttemptNumber(), h.getStatus(), h.getFailureReason(), h.getAttemptedAt()))
                        .toList()
        );
    }
}

