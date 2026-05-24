package org.backend.dto.response;

import org.backend.domain.entity.NotificationRequest;
import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationStatus;
import org.backend.domain.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationListResponse(
        Long id,
        Long recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        String title,
        String body,
        Boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationListResponse fromRequest(NotificationRequest req) {
        return new NotificationListResponse(
                req.getId(),
                req.getRecipientId(),
                req.getNotificationType(),
                req.getChannel(),
                req.getStatus(),
                null, null, null,
                req.getCreatedAt()
        );
    }
}

