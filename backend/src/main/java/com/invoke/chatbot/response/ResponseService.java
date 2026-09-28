package com.invoke.chatbot.response;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ResponseService {

    private final Map<String, Category> categories = new HashMap<>();

    public ResponseService(List<Category> categoryList) {
        categoryList.forEach(category -> categories.put(category.id(), category));
    }

    public String response(String categoryId, String message) {
        Category category = categories.get(categoryId);

        if (category == null || category.qa() == null || category.qa().isEmpty()) {
            return "I’m here to help you with your health insurance application. Shall we carry on?";
        }

        String normalizedMessage = normalize(message);

        for (QAPair pair : category.qa()) {
            if (normalize(pair.user()).equals(normalizedMessage)) {
                return pair.response();
            }
        }

        QAPair bestMatch = findBestMatch(category.qa(), normalizedMessage);

        return bestMatch != null
                ? bestMatch.response()
                : category.qa().get(0).response();
    }

    private QAPair findBestMatch(List<QAPair> questions, String message) {
        QAPair bestMatch = null;
        int bestScore = 0;

        for (QAPair pair : questions) {
            int score = keywordScore(message, normalize(pair.user()));

            if (score > bestScore) {
                bestScore = score;
                bestMatch = pair;
            }
        }

        return bestMatch;
    }

    private int keywordScore(String message, String reference) {
        int score = 0;

        for (String word : reference.split("\\s+")) {
            if (word.length() > 3 && message.contains(word)) {
                score++;
            }
        }

        return score;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
