package org.backend.domain.repository;

import org.backend.domain.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {
    List<NotificationHistory> findByNotificationRequestIdOrderByAttemptedAtAsc(Long requestId);
}
