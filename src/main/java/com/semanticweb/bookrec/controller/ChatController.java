package com.semanticweb.bookrec.controller;

import com.semanticweb.bookrec.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Exercise 7 - chat endpoints
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    public record ChatRequest(String message, String context, String bookId) {
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String reply = chatService.answer(request.message(), request.context(), request.bookId());
        return Map.of("reply", reply);
    }

    @GetMapping("/starters")
    public Map<String, List<String>> starters(
            @RequestParam(defaultValue = "general") String context,
            @RequestParam(required = false) String bookId) {
        return Map.of("starters", chatService.conversationStarters(context, bookId));
    }
}
