package com.c8.examples.synchronous;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResponseWorker {

    @Autowired
    private PendingRequestRegistry pendingRequestRegistry;
    @JobWorker
    public void catchProcessEnd(ActivatedJob job, @VariablesAsType Map<String, Object> variables) {
        pendingRequestRegistry.complete(job.getProcessInstanceKey(), variables);
    }
}
