package com.c8.examples.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatbotForwardController {

    @GetMapping("/chatbot")
    public String redirect() {
        return "forward:/chatbot/index.html";
    }

    @GetMapping("/chatbot/")
    public String redirectSlash() {
        return "forward:/chatbot/index.html";
    }
}