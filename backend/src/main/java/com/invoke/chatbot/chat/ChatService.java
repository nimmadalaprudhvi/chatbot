package com.invoke.chatbot.chat;

import com.invoke.chatbot.audit.AuditLogService;
import com.invoke.chatbot.chat.dto.ChatRequest;
import com.invoke.chatbot.chat.dto.ChatResponse;
import com.invoke.chatbot.chat.dto.InsuranceQuestionResponse;
import com.invoke.chatbot.chat.dto.Classification;
import com.invoke.chatbot.classification.IntentClassifier;
import com.invoke.chatbot.journey.JourneyService;
import com.invoke.chatbot.journey.JourneyStep;
import com.invoke.chatbot.response.Category;
import com.invoke.chatbot.response.ResponseService;
import com.invoke.chatbot.session.SessionService;
import com.invoke.chatbot.session.SessionState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Orchestrates a single chat turn: classifies the message, decides whether it is
 * an off-topic redirect, a crisis pause, or a normal journey answer, and delegates
 * the mechanics of each to the journey, session, response and audit services.
 */
@Service
public class ChatService {

    private static final Pattern ARABIC_PATTERN = Pattern.compile("[\\u0600-\\u06FF]");

    private final JourneyService journeyService;
    private final IntentClassifier classifier;
    private final ResponseService responseService;
    private final SessionService sessionService;
    private final AuditLogService auditLogService;

    public ChatService(
            JourneyService journeyService,
            IntentClassifier classifier,
            ResponseService responseService,
            SessionService sessionService,
            AuditLogService auditLogService) {
        this.journeyService = journeyService;
        this.classifier = classifier;
        this.responseService = responseService;
        this.sessionService = sessionService;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> newSessionResponse() {
        SessionState session = sessionService.create();
        return Map.of(
                "snapshot", sessionService.snapshot(session),
                "welcome", journeyService.firstQuestion().question()
        );
    }

    public Map<String, Object> reset(String sessionId) {
        sessionService.remove(sessionId);
        return newSessionResponse();
    }

    public Map<String, Object> session(String sessionId) {
        SessionState session = sessionService.get(sessionId);
        return Map.of("snapshot", sessionService.snapshot(session));
    }

    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "invoke-chatbot",
                "environment", "uat",
                "llm", false,
                "database", false
        );
    }

    public Map<String, Object> spec(List<Category> categories) {
        return Map.of(
                "categories", categories,
                "classifier_order", List.of(
                        "mental_health",
                        "medical_advice",
                        "gaming",
                        "abuse",
                        "privacy",
                        "language",
                        "coverage",
                        "sharia",
                        "insurance_faq",
                        "complaints",
                        "catch_all"
                )
        );
    }

    public InsuranceQuestionResponse insuranceQuestion(ChatRequest request) {
        validateRequest(request);

        SessionState session = sessionService.get(request.session_id());
        String message = request.message().trim();
        Classification classification = classifier.classify(message, session);
        session.last_classification = classification;
        updateLanguage(session, classification, message);

        String response;

        if ("mental_health".equals(classification.category())) {
            session.status = "paused_crisis";
            response = responseService.response("mental_health", message);
            auditLogService.log(session, classification, message, "PAUSE_JOURNEY", sessionService.currentQuestion(session).id());
            sessionService.addEvent(session, classification.category(), "PAUSE_JOURNEY", sessionService.currentQuestion(session).id());
            return new InsuranceQuestionResponse(response, classification);
        }

        if ("abuse".equals(classification.category())) {
            response = abuseResponse(classification, sessionService.currentQuestion(session));
        } else if ("catch_all".equals(classification.category())) {
            response = responseService.response("catch_all", message);
            response = removeCurrentQuestion(response);
        } else {
            response = responseService.response(classification.category(), message);
            response = removeCurrentQuestion(response);
        }

        auditLogService.log(session, classification, message,
                "catch_all".equals(classification.category()) ? "CATCH_ALL" : "QUESTION",
                sessionService.currentQuestion(session).id());
        sessionService.addEvent(session, classification.category(),
                "catch_all".equals(classification.category()) ? "CATCH_ALL" : "QUESTION",
                sessionService.currentQuestion(session).id());

        return new InsuranceQuestionResponse(response, classification);
    }

    private String removeCurrentQuestion(String response) {
        if (response == null) {
            return "";
        }

        return response
                .replace("[CURRENT QUESTION]", "")
                .replace("[current question]", "")
                .replace("[Repeat current question]", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    public ChatResponse chat(ChatRequest request) {
        validateRequest(request);

        SessionState session = sessionService.get(request.session_id());
        String message = request.message().trim();
        JourneyStep currentQuestion = sessionService.currentQuestion(session);

        if ("paused_crisis".equals(session.status)) {
            return new ChatResponse(
                    "Your application is currently paused. Please reach out to Tawasul on 800-4673. Your application is saved — there is no pressure to continue today.",
                    false,
                    new Classification("mental_health", "crisis", "SECURE"),
                    sessionService.snapshot(session)
            );
        }

        Classification classification = classifier.classify(message, session);
        session.last_classification = classification;

        updateLanguage(session, classification, message);

        if ("mental_health".equals(classification.category())) {
            return handleCrisis(session, classification, message, currentQuestion);
        }

        // High-priority intents are checked before normal journey answers.
        if (!"catch_all".equals(classification.category())) {
            String response = responseService.response(classification.category(), message);

            if ("abuse".equals(classification.category())) {
                response = abuseResponse(classification, currentQuestion);
            }

            response = journeyService.replaceQuestionPlaceholder(response, currentQuestion);
            auditLogService.log(session, classification, message, "REDIRECT", currentQuestion.id());
            sessionService.addEvent(session, classification.category(), "REDIRECT", currentQuestion.id());

            return new ChatResponse(response, false, classification, sessionService.snapshot(session));
        }

        if (journeyService.isExpectedAnswer(currentQuestion, message)) {
            return advance(session, message, currentQuestion);
        }

        String response = journeyService.replaceQuestionPlaceholder(
                responseService.response("catch_all", message),
                currentQuestion
        );

        auditLogService.log(session, classification, message, "CATCH_ALL", currentQuestion.id());
        sessionService.addEvent(session, "catch_all", "CATCH_ALL", currentQuestion.id());

        return new ChatResponse(response, false, classification, sessionService.snapshot(session));
    }

    private ChatResponse handleCrisis(
            SessionState session,
            Classification classification,
            String message,
            JourneyStep currentQuestion) {

        session.status = "paused_crisis";
        String response = responseService.response("mental_health", message);

        auditLogService.log(session, classification, message, "PAUSE_JOURNEY", currentQuestion.id());
        sessionService.addEvent(session, classification.category(), "PAUSE_JOURNEY", currentQuestion.id());

        return new ChatResponse(response, false, classification, sessionService.snapshot(session));
    }

    private ChatResponse advance(SessionState session, String message, JourneyStep currentQuestion) {
        session.answers.put(currentQuestion.id(), message);

        if ("applicant_name".equals(currentQuestion.id())) {
            session.applicant_name = message.trim();
        }

        if ("terminal".equals(currentQuestion.type())) {
            if ("start again".equalsIgnoreCase(message.trim())) {
                session.question_index = 0;
                session.applicant_name = null;
                session.status = "active";
                session.answers.clear();
                session.events.clear();
                session.last_classification = null;

                return new ChatResponse(
                        journeyService.firstQuestion().question(),
                        true,
                        null,
                        sessionService.snapshot(session)
                );
            }

            return new ChatResponse(
                    currentQuestion.question(),
                    false,
                    new Classification("normal_journey", "COMPLETE", "STANDARD"),
                    sessionService.snapshot(session)
            );
        }

        if (session.question_index < journeyService.size() - 1) {
            session.question_index++;
            Classification classification = new Classification("normal_journey", "ADVANCE", "STANDARD");
            session.last_classification = classification;
            sessionService.addEvent(session, "normal_journey", "ADVANCE", currentQuestion.id());

            String nextQuestion = sessionService.currentQuestion(session).question();

            if ("applicant_name".equals(currentQuestion.id())) {
                nextQuestion = "Thanks, " + session.applicant_name
                        + ". Welcome to invoke.ai. I’ll help you complete your health insurance application. "
                        + nextQuestion;
            }

            return new ChatResponse(
                    nextQuestion,
                    true,
                    classification,
                    sessionService.snapshot(session)
            );
        }

        return new ChatResponse(
                "Your application has reached the end of the available questions.",
                false,
                new Classification("normal_journey", "COMPLETE", "STANDARD"),
                sessionService.snapshot(session)
        );
    }

    private String abuseResponse(Classification classification, JourneyStep question) {
        if ("jailbreak".equals(classification.subtopic())) {
            return "I’m only able to help you with your health insurance application. Shall we carry on? "
                    + question.question();
        }

        if ("tier2".equals(classification.subtopic())) {
            return "I can only help with your insurance application. If you’d rather speak to someone directly, I can arrange that. Otherwise, "
                    + question.question();
        }

        return "Let’s keep it friendly — I’m here to help you through your application. "
                + question.question();
    }

    private void updateLanguage(SessionState session, Classification classification, String message) {
        if (!"language".equals(classification.category())) {
            return;
        }

        String text = normalize(message);
        if (text.contains("arabic") || ARABIC_PATTERN.matcher(message).find()) {
            session.language = "ar";
        } else if (text.contains("hindi")) {
            session.language = "hi";
        }
    }

    private void validateRequest(ChatRequest request) {
        if (request == null
                || request.session_id() == null
                || !sessionService.exists(request.session_id())) {
            throw new IllegalArgumentException("Invalid session_id");
        }

        if (request.message() == null || request.message().trim().isEmpty()) {
            throw new IllegalArgumentException("Message required");
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
