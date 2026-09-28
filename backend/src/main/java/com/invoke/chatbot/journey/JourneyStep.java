package com.invoke.chatbot.journey;

import java.util.List;

public record JourneyStep(
        String id,
        String section,
        String question,
        String type,
        List<String> options) {
}
