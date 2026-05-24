package org.backend.dto.response;

import org.backend.domain.enums.NotificationStatus;

public record CreateNotificationResponse(
        Long notificationId,
        String message,
        boolean isDuplicate,
        NotificationStatus currentStatus
) {
    public static CreateNotificationResponse accepted(Long id) {
        return new CreateNotificationResponse(id, "알림 요청이 접수되었습니다.", false, NotificationStatus.PENDING);
    }

    public static CreateNotificationResponse duplicate(Long id, NotificationStatus status) {
        return new CreateNotificationResponse(id, "이미 접수된 알림 요청입니다.", true, status);
    }
}
