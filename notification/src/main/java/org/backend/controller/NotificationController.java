package org.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.dto.request.CreateNotificationRequest;
import org.backend.dto.response.CreateNotificationResponse;
import org.backend.dto.response.NotificationDetailResponse;
import org.backend.dto.response.NotificationListResponse;
import org.backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // 알림 발송 요청 등록
    @PostMapping
    public ResponseEntity<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        CreateNotificationResponse response = notificationService.createNotification(request);
        HttpStatus status = response.isDuplicate() ? HttpStatus.CONFLICT : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    // 알림 상태 조회
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    // 사용자 알림 목록 조회
    @GetMapping
    public ResponseEntity<Page<NotificationListResponse>> getNotifications(
            @RequestHeader("X-User-Id") Long recipientId,
            @RequestParam(defaultValue = "false") Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(recipientId, unreadOnly, page, size));
    }
}
