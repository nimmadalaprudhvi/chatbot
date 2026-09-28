package com.invoke.chatbot.chat;

import com.invoke.chatbot.chat.dto.ChatRequest;
import com.invoke.chatbot.chat.dto.ChatResponse;
import com.invoke.chatbot.chat.dto.InsuranceQuestionResponse;
import com.invoke.chatbot.chat.dto.SessionRequest;
import com.invoke.chatbot.response.Category;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final List<Category> categories;

    public ChatController(ChatService chatService, List<Category> categories) {
        this.chatService = chatService;
        this.categories = categories;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return chatService.health();
    }

    @GetMapping("/session")
    public Map<String, Object> createSession() {
        return chatService.newSessionResponse();
    }

    @PostMapping("/session")
    public Map<String, Object> createSessionPost() {
        return chatService.newSessionResponse();
    }

    @GetMapping("/session/{sessionId}")
    public Map<String, Object> getSession(@PathVariable String sessionId) {
        return chatService.session(sessionId);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset(@RequestBody(required = false) SessionRequest request) {
        String sessionId = request == null ? null : request.session_id();
        return chatService.reset(sessionId);
    }

    @GetMapping("/spec")
    public Map<String, Object> specification() {
        return chatService.spec(categories);
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping("/insurance-question")
    public InsuranceQuestionResponse insuranceQuestion(@RequestBody ChatRequest request) {
        return chatService.insuranceQuestion(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }
}
