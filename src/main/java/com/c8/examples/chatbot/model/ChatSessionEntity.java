package com.c8.examples.chatbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
public class ChatSessionEntity {

  @Id
  private String sessionId;

  @Column(columnDefinition = "TEXT")
  private String messagesJson;

  private Long lastJobKey;
}
