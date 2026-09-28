package com.invoke.chatbot.journey;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Owns the approved journey structure and determines whether a message
 * is a valid answer for the current journey step.
 */
@Service
public class JourneyService {

    private static final Pattern DOB_PATTERN =
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}][\\p{L}\\s.'-]{1,59}$");

    private static final Set<String> AFFIRMATIVE_RESPONSES = Set.of(
            "yes",
            "y",
            "yeah",
            "yep",
            "sure",
            "ok",
            "okay"
    );

    private final List<JourneyStep> journey;

    public JourneyService(List<JourneyStep> journey) {
        this.journey = journey;
    }

    public List<JourneyStep> steps() {
        return journey;
    }

    public int size() {
        return journey.size();
    }

    public JourneyStep firstQuestion() {
        return journey.get(0);
    }

    public JourneyStep stepAt(int questionIndex) {
        return journey.get(Math.min(questionIndex, journey.size() - 1));
    }

    public boolean isExpectedAnswer(JourneyStep question, String message) {
        String text = normalize(message);

        if (text.isBlank()) {
            return false;
        }

        if ("name".equals(question.type())) {
            return NAME_PATTERN.matcher(message.trim()).matches();
        }

        if ("choice".equals(question.type())) {
            if (question.options().stream()
                    .anyMatch(option -> normalize(option).equals(text))) {
                return true;
            }

            if ("review".equals(question.id())) {
                return Set.of(
                        "review",
                        "review it",
                        "review my answers",
                        "submit",
                        "submit application"
                ).contains(text) || isAffirmative(text);
            }

            return isAffirmative(text)
                    && question.options().stream()
                    .anyMatch(option -> normalize(option).equals("yes"));
        }

        if ("terminal".equals(question.type())) {
            return "start again".equals(text);
        }

        if ("dob".equals(question.type())) {
            return DOB_PATTERN.matcher(text).find();
        }

        if ("residency".equals(question.type())) {
            return text.split("\\s+").length <= 8
                    && !containsAny(
                    text,
                    "weather",
                    "movie",
                    "football",
                    "stock",
                    "joke",
                    "politics"
            );
        }

        return text.split("\\s+").length <= 12;
    }

    public String replaceQuestionPlaceholder(
            String response,
            JourneyStep question) {

        return response
                .replace("[CURRENT QUESTION]", question.question())
                .replace("[current question]", question.question())
                .replace("[Repeat current question]", question.question());
    }

    private boolean isAffirmative(String text) {
        return AFFIRMATIVE_RESPONSES.contains(text);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }

        return false;
    }
}
