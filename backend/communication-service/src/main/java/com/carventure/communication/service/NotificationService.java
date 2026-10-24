package com.carventure.communication.service;

import com.carventure.communication.entity.Notification;
import com.carventure.communication.exception.NotFoundException;
import com.carventure.communication.repository.NotificationRepository;
import com.carventure.communication.sse.EmitterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final EmitterRegistry emitterRegistry;

    public NotificationService(NotificationRepository repository, EmitterRegistry emitterRegistry) {
        this.repository = repository;
        this.emitterRegistry = emitterRegistry;
    }

    @Transactional
    public Notification create(String recipientId, String type, String title,
                               String message, String referenceId, String referenceType) {

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = repository.save(notification);

        emitterRegistry.push(recipientId, "notification", saved);
        emitterRegistry.push(recipientId, "unreadCount", unreadCount(recipientId));

        return saved;
    }

    public Page<Notification> list(String userId, boolean unreadOnly, Pageable pageable) {
        return unreadOnly
                ? repository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : repository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long unreadCount(String userId) {
        return repository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification markRead(String notificationId) {

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            repository.save(notification);
            emitterRegistry.push(notification.getRecipientId(), "unreadCount",
                    unreadCount(notification.getRecipientId()));
        }

        return notification;
    }

    @Transactional
    public void markAllRead(String userId) {
        repository.markAllRead(userId, LocalDateTime.now());
        emitterRegistry.push(userId, "unreadCount", 0L);
    }

    @Transactional
    public void delete(String notificationId) {

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        String recipientId = notification.getRecipientId();

        repository.delete(notification);

        emitterRegistry.push(recipientId, "unreadCount", unreadCount(recipientId));
    }
}
