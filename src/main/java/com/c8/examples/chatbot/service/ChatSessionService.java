package com.c8.examples.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.c8.examples.chatbot.model.ChatHistoryResponse;
import com.c8.examples.chatbot.model.ChatSessionEntity;
import com.c8.examples.chatbot.model.ChatbotMessage;
import com.c8.examples.chatbot.repository.ChatSessionRepository;
import com.c8.examples.chatbot.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatSessionService {

  private static final Logger LOG = LoggerFactory.getLogger(ChatSessionService.class);

  @Autowired
  private ChatSessionRepository repository;

  public void saveMessage(String sessionId, ChatbotMessage message) {
    try {
      ChatSessionEntity entity = repository.findById(sessionId)
          .orElseGet(() -> {
            ChatSessionEntity e = new ChatSessionEntity();
            e.setSessionId(sessionId);
            return e;
          });
      List<ChatbotMessage> messages = deserializeMessages(entity.getMessagesJson());
      messages.add(message);
      entity.setMessagesJson(JsonUtils.toJson(messages));
      if (message.getJobKey() != null) {
        entity.setLastJobKey(message.getJobKey());
      }
      repository.save(entity);
    } catch (Exception e) {
      LOG.error("Error saving message for session {}", sessionId, e);
    }
  }

  public ChatHistoryResponse getHistory(String sessionId) {
    return repository.findById(sessionId)
        .map(entity -> new ChatHistoryResponse(
            deserializeMessages(entity.getMessagesJson()),
            entity.getLastJobKey()))
        .orElse(new ChatHistoryResponse(new ArrayList<>(), null));
  }

  private List<ChatbotMessage> deserializeMessages(String json) {
    if (json == null || json.isBlank()) {
      return new ArrayList<>();
    }
    try {
      return JsonUtils.toParametrizedObject(json, new TypeReference<List<ChatbotMessage>>() {});
    } catch (Exception e) {
      LOG.error("Error deserializing messages", e);
      return new ArrayList<>();
    }
  }
}
