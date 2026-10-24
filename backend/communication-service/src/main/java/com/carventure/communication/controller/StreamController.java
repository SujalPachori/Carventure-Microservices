package com.carventure.communication.controller;

import com.carventure.communication.service.NotificationService;
import com.carventure.communication.sse.EmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class StreamController {

    private final EmitterRegistry emitterRegistry;
    private final NotificationService notificationService;

    public StreamController(EmitterRegistry emitterRegistry, NotificationService notificationService) {
        this.emitterRegistry = emitterRegistry;
        this.notificationService = notificationService;
    }

    @GetMapping(value = "/api/v1/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String userId) {

        SseEmitter emitter = emitterRegistry.register(userId);

        emitterRegistry.push(userId, "connected", userId);
        emitterRegistry.push(userId, "unreadCount", notificationService.unreadCount(userId));

        return emitter;
    }
}
