package org.backend.domain.repository;

import org.backend.domain.entity.NotificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {
    Optional<NotificationRequest> findByIdempotencyKey(String idempotencyKey);

    Page<NotificationRequest> findByRecipientId(Long recipientId, Pageable pageable);
}
