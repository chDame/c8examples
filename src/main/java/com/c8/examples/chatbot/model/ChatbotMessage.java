package com.c8.examples.chatbot.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ChatbotMessage {
  private String author;
  private boolean isUser;
  private boolean thinking;
  private Long jobKey;
  private String message;
  private long timestamp;
}
