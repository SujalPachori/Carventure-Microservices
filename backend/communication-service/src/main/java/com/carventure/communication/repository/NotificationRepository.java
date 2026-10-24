package com.carventure.communication.repository;

import com.carventure.communication.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    long countByRecipientIdAndIsReadFalse(String recipientId);

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :now "
            + "where n.recipientId = :recipientId and n.isRead = false")
    int markAllRead(@Param("recipientId") String recipientId, @Param("now") LocalDateTime now);
}
