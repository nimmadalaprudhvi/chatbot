package com.invoke.chatbot.session;

import com.invoke.chatbot.journey.JourneyService;
import com.invoke.chatbot.journey.JourneyStep;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final JourneyService journeyService;

    public SessionService(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    public SessionState create() {
        SessionState session = new SessionState(UUID.randomUUID().toString());
        sessions.put(session.session_id, session);
        return session;
    }

    public SessionState get(String sessionId) {
        SessionState session = sessions.get(sessionId);
        if (session == null) {
            throw new NoSuchElementException("Session not found");
        }
        return session;
    }

    public boolean exists(String sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    public void remove(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public JourneyStep currentQuestion(SessionState session) {
        return journeyService.stepAt(session.question_index);
    }

    public void addEvent(SessionState session, String category, String action, String questionId) {
        session.events.add(Map.of(
                "time", Instant.now().toString(),
                "type", "classification",
                "category", category,
                "action", action,
                "question_id", questionId
        ));
    }

    public Map<String, Object> snapshot(SessionState session) {
        JourneyStep question = currentQuestion(session);
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("session_id", session.session_id);
        snapshot.put("status", session.status);
        snapshot.put("applicant_name", session.applicant_name);
        snapshot.put("current_question_index", session.question_index);
        snapshot.put("current_question_id", question.id());
        snapshot.put("current_question", question.question());
        snapshot.put("section", question.section());
        snapshot.put("journey", journeyService.steps());
        snapshot.put("answers", session.answers);
        snapshot.put("language", session.language);
        snapshot.put("last_classification", session.last_classification);
        snapshot.put(
                "events",
                new ArrayList<>(session.events.subList(
                        Math.max(0, session.events.size() - 12),
                        session.events.size()
                ))
        );

        return snapshot;
    }
}
