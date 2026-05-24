package org.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationStatus;
import org.backend.domain.enums.NotificationType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notification_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "extraData")
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "JSON")
    private Map<String, Object> extraData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private int maxRetryCount;

    @Column(name = "retry_at")
    private LocalDateTime retryAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "processing_node", length = 255)
    private String processingNode;

    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 팩토리 메서드
    public static NotificationRequest create(
            String idempotencyKey,
            Long recipientId,
            NotificationType notificationType,
            NotificationChannel channel,
            String referenceId,
            String referenceType,
            Map<String, Object> extraData,
            int maxRetryCount,
            LocalDateTime scheduledAt
    ) {
        NotificationRequest req = new NotificationRequest();
        req.idempotencyKey = idempotencyKey;
        req.recipientId = recipientId;
        req.notificationType = notificationType;
        req.channel = channel;
        req.referenceId = referenceId;
        req.referenceType = referenceType;
        req.extraData = extraData;
        req.status = NotificationStatus.PENDING;
        req.retryCount = 0;
        req.maxRetryCount = maxRetryCount;
        req.scheduledAt = scheduledAt;
        return req;
    }

    // 상태 변경
    /**
     * PENDING → PROCESSING
     */
    public void markProcessing(String nodeId) {
        this.status = NotificationStatus.PROCESSING;
        this.processingNode = nodeId;
        this.processingAt = LocalDateTime.now();
    }

    /**
     * PROCESSING → SENT
     */
    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.processingNode = null;
        this.processingAt = null;
        this.failureReason = null;
    }

    /**
     * PROCESSING → PENDING (재시도 예약)
     */
    public void markRetry(String reason, LocalDateTime retryAt) {
        this.status = NotificationStatus.PENDING;
        this.retryCount++;
        this.failureReason = reason;
        this.retryAt = retryAt;
        this.processingNode = null;
        this.processingAt = null;
    }

    /**
     * → FAILED
     */
    public void markDeadLetter(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.processingNode = null;
        this.processingAt = null;
    }

    /**
     * FAILED → PENDING (수동 재시도)
     * @param resetRetryCount 가 true면 retryCount를 0으로 초기화
     */
    public void markManualRetry(boolean resetRetryCount) {
        this.status = NotificationStatus.PENDING;
        this.retryAt = null;
        this.failureReason = null;
        if (resetRetryCount) {
            this.retryCount = 0;
        }
    }

    /**
     * PROCESSING stuck → PENDING 복구
     */
    public void recoverFromStuck() {
        this.status = NotificationStatus.PENDING;
        this.retryAt = LocalDateTime.now(); // 즉시 재처리
        this.processingNode = null;
        this.processingAt = null;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount;
    }
}
