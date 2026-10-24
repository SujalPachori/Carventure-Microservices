package com.carventure.communication.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(EmitterRegistry.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String userId) {

        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));

        return emitter;
    }

    public void push(String userId, String eventName, Object payload) {

        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Dropping dead emitter for {}", userId);
                remove(userId, emitter);
            }
        }
    }

    public int connectionCount(String userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters == null ? 0 : userEmitters.size();
    }

    private void remove(String userId, SseEmitter emitter) {

        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()){
                emitters.remove(userId, userEmitters);
            }
        }
    }
}
