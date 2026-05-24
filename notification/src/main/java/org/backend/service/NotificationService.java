package org.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.domain.entity.NotificationHistory;
import org.backend.domain.entity.NotificationRequest;
import org.backend.domain.repository.NotificationHistoryRepository;
import org.backend.domain.repository.NotificationRequestRepository;
import org.backend.dto.request.CreateNotificationRequest;
import org.backend.dto.response.CreateNotificationResponse;
import org.backend.dto.response.NotificationDetailResponse;
import org.backend.dto.response.NotificationListResponse;
import org.backend.exception.DuplicateNotificationException;
import org.backend.exception.NotificationNotFoundException;
import org.backend.util.IdempotencyKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRequestRepository requestRepository;
    private final NotificationHistoryRepository historyRepository;
    private final IdempotencyKeyGenerator keyGenerator;

    @Value("${notification.retry.max-attempts:3}")
    private int maxRetryCount;

    // ----------------------------------------------------------------
    // 1. 알림 발송 요청 등록
    // ----------------------------------------------------------------
    /**
     * 중복 방지 전략:
     *   1. idempotencyKey 계산 (SHA-256)
     *   2. DB unique constraint로 중복 INSERT 차단
     *   3. 동시 요청(race condition)은 DataIntegrityViolationException → 409 반환
     */
    @Transactional
    public CreateNotificationResponse createNotification(CreateNotificationRequest dto) {
        String idempotencyKey = keyGenerator.generate(
                dto.recipientId(),
                dto.notificationType(),
                dto.referenceId(),
                dto.channel()
        );

        // 기존 요청 확인
        var existing = requestRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            NotificationRequest req = existing.get();
            log.info("Duplicate notification request detected: idempotencyKey={}, existingId={}, status={}",
                    idempotencyKey, req.getId(), req.getStatus());
            return CreateNotificationResponse.duplicate(req.getId(), req.getStatus());
        }

        // 신규 요청 저장
        NotificationRequest request = NotificationRequest.create(
                idempotencyKey,
                dto.recipientId(),
                dto.notificationType(),
                dto.channel(),
                dto.referenceId(),
                dto.referenceType(),
                dto.extraData(),
                maxRetryCount,
                dto.scheduledAt()
        );

        try {
            request = requestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 - unique constraint 위반
            log.warn("Race condition on idempotencyKey={}", idempotencyKey);
            var saved = requestRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new DuplicateNotificationException(idempotencyKey));
            return CreateNotificationResponse.duplicate(saved.getId(), saved.getStatus());
        }

        log.info("Notification request created: id={}, type={}, channel={}, recipient={}",
                request.getId(), request.getNotificationType(), request.getChannel(), request.getRecipientId());

        return CreateNotificationResponse.accepted(request.getId());
    }

    // ----------------------------------------------------------------
    // 2. 알림 상태 조회
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotification(Long id) {
        NotificationRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        List<NotificationHistory> histories = historyRepository
                .findByNotificationRequestIdOrderByAttemptedAtAsc(id);

        return NotificationDetailResponse.from(req, histories);
    }

    // ----------------------------------------------------------------
    // 3. 사용자 알림 목록 조회
    // ----------------------------------------------------------------
    @Transactional(readOnly = true)
    public Page<NotificationListResponse> getNotifications(
            Long recipientId,
            Boolean unreadOnly,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationRequest> reqPage = requestRepository
                .findByRecipientId(recipientId, pageable);
        return reqPage.map(NotificationListResponse::fromRequest);
    }
}
