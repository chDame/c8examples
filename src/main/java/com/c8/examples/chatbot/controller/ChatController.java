package com.c8.examples.chatbot.controller;

import com.c8.examples.chatbot.exception.TaskListException;
import com.c8.examples.chatbot.facade.ChatbotFacade;
import com.c8.examples.chatbot.model.ChatHistoryResponse;
import com.c8.examples.chatbot.model.ChatbotMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/chatbot")
public class ChatController {

  private final Logger logger = LoggerFactory.getLogger(ChatController.class);
  @Autowired private ChatbotFacade chatbotFacade;



  @GetMapping(value = "/history/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ChatHistoryResponse getChatHistory(@PathVariable String sessionId) {
    return chatbotFacade.getHistory(sessionId);
  }

  @PostMapping(
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> startChatSession(
      @RequestPart("body") ChatbotMessage body,
      @RequestPart(name = "documents", required = false) List<MultipartFile> files)
      throws IOException {
    return chatbotFacade.startChat(body, files);
  }

  @GetMapping("/chat-sse/{sessionId}")
  public SseEmitter chatbotSse(@PathVariable String sessionId) {
    return chatbotFacade.getSseEmitter(sessionId);
  }

  @PostMapping(
      value = "/userInput/{sessionId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> userInput(
      @PathVariable String sessionId,
      @RequestPart("body") ChatbotMessage body,
      @RequestPart(name = "documents", required = false) List<MultipartFile> files)
      throws IOException {
    return chatbotFacade.sendUserInput(sessionId, body, files);
  }
}
