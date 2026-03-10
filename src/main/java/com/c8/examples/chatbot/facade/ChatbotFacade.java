package com.c8.examples.chatbot.facade;

import com.c8.examples.chatbot.model.ChatbotMessage;
import com.c8.examples.chatbot.model.ChatHistoryResponse;
import com.c8.examples.chatbot.service.ChatSessionService;
import com.c8.examples.chatbot.service.ZeebeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Service
public class ChatbotFacade {

  @Autowired private ZeebeService zeebeService;
  @Autowired private ChatSessionService chatSessionService;
  private final Map<String, List<ChatbotMessage>> hospital = new HashMap<>();
  private final Map<String, SseEmitter> instanceEmitter = new HashMap<>();

  private Map<String, Object> buildPayload(String sessionId, ChatbotMessage message, List<MultipartFile> files) throws IOException {
    Map<String, Object> payload = new HashMap<>();
    payload.put("sessionId", sessionId);
    payload.put("userRequest", message);

    /** handle files */
    if (files != null && !files.isEmpty()) {
      payload.put("userDocuments", zeebeService.createFiles(files));
    }
    return payload;
  }

  private ChatbotMessage buildUserMessage(ChatbotMessage message) {
    return new ChatbotMessage()
        .setMessage(message.getMessage())
        .setAuthor(message.getAuthor())
        .setUser(true)
        .setThinking(false)
            .setTimestamp(System.currentTimeMillis());
  }

  public Map<String, Object> startChat(ChatbotMessage message, List<MultipartFile> files) throws IOException {
    String sessionId = UUID.randomUUID().toString().replaceAll("-", "");
    Map<String, Object> payload = buildPayload(sessionId, message, files);

    chatSessionService.saveMessage(sessionId, buildUserMessage(message));

    zeebeService.startInstance("chatbot", payload);
    return payload;
  }

  public SseEmitter getSseEmitter(String sessionId) {
    if (!instanceEmitter.containsKey(sessionId)) {
      SseEmitter emitter = new SseEmitter(-1L);
      instanceEmitter.put(sessionId, emitter);
      emitter.onTimeout(
          () -> {
            instanceEmitter.remove(sessionId);
          });
      emitter.onCompletion(
          () -> {
            instanceEmitter.remove(sessionId);
          });
      if (hospital.containsKey(sessionId) && !hospital.get(sessionId).isEmpty()) {
        new Thread(
                () -> {
                  List<ChatbotMessage> pending = hospital.get(sessionId);
                  for (ChatbotMessage r : pending) {
                    try {
                      Thread.sleep(500);
                      emitter.send(r);
                    } catch (InterruptedException | IOException e) {
                    }
                  }
                  hospital.remove(sessionId);
                })
            .start();
      }
    }
    return instanceEmitter.get(sessionId);
  }

  public void sendResponse(String sessionId, ChatbotMessage response) throws IOException {
    if (!response.isThinking()) {
      chatSessionService.saveMessage(sessionId, response);
    }
    if (instanceEmitter.containsKey(sessionId)) {
      try {
        instanceEmitter.get(sessionId).send(response);
      } catch (IOException | IllegalStateException e) {
        instanceEmitter.remove(sessionId);
        // chat is closed, we should do something else
      }
    } else {
      if (!hospital.containsKey(sessionId)) {
        hospital.put(sessionId, new ArrayList<>());
      }
      hospital.get(sessionId).add(response);
    }
  }

  public Map<String, Object> sendUserInput(
      String sessionId, ChatbotMessage message, List<MultipartFile> files)
      throws IOException {

    Map<String, Object> payload = buildPayload(sessionId, message, files);

    chatSessionService.saveMessage(sessionId, buildUserMessage(message));

    zeebeService
          .getCamundaClient()
          .newCompleteCommand(message.getJobKey())
          .variables(Map.of("toolCallResult", payload))
          .send();

    return payload;
  }

  public ChatHistoryResponse getHistory(String sessionId) {
    return chatSessionService.getHistory(sessionId);
  }

}
