package com.c8.examples.synchronous;

import com.c8.examples.chatbot.service.ZeebeService;
import io.camunda.client.api.response.ProcessInstanceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/synchronous")
public class SynchronousController {

    @Autowired
    private PendingRequestRegistry pendingRequestRegistry;
    @Autowired
    private ZeebeService zeebeService;

    @PostMapping("/simulateSynchronousCall")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> simulateSynchronousCall(@RequestBody Map<String, Object> payload) {
        ProcessInstanceEvent instanceEvent = zeebeService.startInstance("fakeSynchronousProcess", payload);

        CompletableFuture<Map<String, Object>> future = pendingRequestRegistry.register(instanceEvent.getProcessInstanceKey());
        return future
                .orTimeout(30, TimeUnit.SECONDS)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> ResponseEntity.status(504).body(Map.of("Status","Timeout")));
    }
}
