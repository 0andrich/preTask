package org.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateNotificationRequest(
        @NotNull(message = "recipientId는 필수입니다")
        Long recipientId,

        @NotNull(message = "notificationType은 필수입니다")
        NotificationType notificationType,

        @NotNull(message = "channel은 필수입니다")
        NotificationChannel channel,

        String referenceId,

        String referenceType,

        Map<String, Object> extraData,

        LocalDateTime scheduledAt
) {}
