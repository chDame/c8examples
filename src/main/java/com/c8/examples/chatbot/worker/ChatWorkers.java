package com.c8.examples.chatbot.worker;

import com.c8.examples.chatbot.facade.ChatbotFacade;
import com.c8.examples.chatbot.model.ChatbotMessage;
import com.c8.examples.chatbot.service.ZeebeService;
import io.camunda.client.annotation.JobKey;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.annotation.VariablesAsType;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ChatWorkers {

    @Autowired
    private ChatbotFacade chatbotFacade;
    @Autowired
    private ZeebeService zeebeService;

    @JobWorker
    public void chatbotThinking(@Variable String botThinking, @Variable String sessionId) throws IOException {
        ChatbotMessage response =
                new ChatbotMessage()
                        .setThinking(true)
                        .setMessage(botThinking)
                        .setAuthor("C8 Banking chat")
                        .setUser(false)
                        .setTimestamp(System.currentTimeMillis());
        chatbotFacade.sendResponse(sessionId, response);
    }


    @JobWorker(autoComplete = false)
    public void chatbotReply(final JobClient client, final ActivatedJob job, @Variable String chatbotMessage, @Variable String sessionId, @Variable Boolean waitResponse) throws IOException {
        ChatbotMessage response =
                new ChatbotMessage()
                        .setThinking(false)
                        .setMessage(chatbotMessage)
                        .setJobKey(job.getKey())
                        .setAuthor("C8 Banking chat")
                        .setUser(false)
                        .setTimestamp(System.currentTimeMillis());
        chatbotFacade.sendResponse(sessionId, response);
        if (!waitResponse) {
            client.newCompleteCommand(job.getKey()).send();
        }
    }
}
