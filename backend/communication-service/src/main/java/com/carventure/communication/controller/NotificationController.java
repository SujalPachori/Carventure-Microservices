package com.carventure.communication.controller;

import com.carventure.communication.entity.Notification;
import com.carventure.communication.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public Page<Notification> list(@PathVariable String userId,
                                   @RequestParam(defaultValue = "false") boolean unreadOnly,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return service.list(userId, unreadOnly, PageRequest.of(page, size));
    }

    @GetMapping("/unreadCount/{userId}")
    public long unreadCount(@PathVariable String userId) {
        return service.unreadCount(userId);
    }

    @PutMapping("/{notificationId}/read")
    public Notification markRead(@PathVariable String notificationId) {
        return service.markRead(notificationId);
    }

    @PutMapping("/markAllRead/{userId}")
    public ResponseEntity<Void> markAllRead(@PathVariable String userId) {
        service.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable String notificationId) {
        service.delete(notificationId);
        return ResponseEntity.noContent().build();
    }
}
