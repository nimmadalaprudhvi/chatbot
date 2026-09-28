package com.invoke.chatbot.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoke.chatbot.chat.dto.Classification;
import com.invoke.chatbot.session.SessionState;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class AuditLogService {

    private static final Set<String> MESSAGE_LOGGED_CATEGORIES = Set.of("medical_advice", "gaming", "abuse");
    private static final Set<String> SUBTOPIC_LOGGED_CATEGORIES = Set.of("coverage", "insurance_faq");

    private final Path sessionLog = Paths.get("data/session_logs.jsonl");
    private final Path crisisLog = Paths.get("data/restricted_crisis_audit.jsonl");
    private final ObjectMapper objectMapper;

    public AuditLogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void log(
            SessionState session,
            Classification classification,
            String message,
            String action,
            String questionId) {

        try {
            Files.createDirectories(sessionLog.getParent());

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("timestamp", Instant.now().toString());
            record.put("session_id", session.session_id);
            record.put("category", classification.category());
            record.put("action", action);
            record.put("question_id", questionId);

            if (MESSAGE_LOGGED_CATEGORIES.contains(classification.category())) {
                record.put("message", message);
            }

            if (SUBTOPIC_LOGGED_CATEGORIES.contains(classification.category())) {
                record.put("subtopic", classification.subtopic());
            }

            appendJsonLine(sessionLog, record);

            if ("mental_health".equals(classification.category())) {
                Files.createDirectories(crisisLog.getParent());
                Map<String, Object> crisisRecord = new LinkedHashMap<>(record);
                crisisRecord.put("message", message);
                appendJsonLine(crisisLog, crisisRecord);
            }
        } catch (Exception ignored) {
            // Logging must not interrupt the applicant journey.
        }
    }

    private void appendJsonLine(Path path, Map<String, Object> record) throws Exception {
        Files.writeString(
                path,
                objectMapper.writeValueAsString(record) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}
