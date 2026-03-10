package com.c8.examples.chatbot.exception;

public class TaskListException extends RuntimeException {
  public TaskListException(String message) {
    super(message);
  }

  public TaskListException(Throwable cause) {
    super(cause);
  }

  public TaskListException(String message, Throwable cause) {
    super(message, cause);
  }
}
