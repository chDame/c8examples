package com.c8.examples.synchronous;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingRequestRegistry {

    private final Map<Long, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<Map<String, Object>> register(Long processInstanceKey) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pending.put(processInstanceKey, future);
        return future;
    }

    public void complete(Long processInstanceKey, Map<String, Object> payload) {
        CompletableFuture<Map<String, Object>> future = pending.remove(processInstanceKey);
        if (future != null) {
            future.complete(payload);
        }
    }
}
