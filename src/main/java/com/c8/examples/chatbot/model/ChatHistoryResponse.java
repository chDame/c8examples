package com.c8.examples.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChatHistoryResponse {

  private List<ChatbotMessage> messages;
  private Long lastJobKey;
}
