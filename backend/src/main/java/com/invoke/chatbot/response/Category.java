package com.invoke.chatbot.response;

import java.util.List;

public record Category(
        String id,
        String title,
        String priority,
        List<QAPair> qa) {
}
