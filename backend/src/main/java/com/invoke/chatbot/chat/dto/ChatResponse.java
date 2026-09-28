package com.invoke.chatbot.chat.dto;

import java.util.Map;

public record ChatResponse(
        String reply,
        boolean advance,
        Classification classification,
        Map<String, Object> snapshot) {
}
