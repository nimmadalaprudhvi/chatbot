package com.invoke.chatbot.classification;

import com.invoke.chatbot.chat.dto.Classification;
import com.invoke.chatbot.session.SessionState;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class IntentClassifier {

    private static final Pattern ARABIC_TEXT = Pattern.compile("[\\u0600-\\u06FF]");

    public Classification classify(String message, SessionState session) {
        String text = normalize(message);

        // 1. Crisis / mental health — always first.
        if (containsAny(text,
                "kill myself", "suicide", "suicidal", "hurt myself", "self harm", "self-harm",
                "want it all to stop", "dark thoughts", "no point in anything", "dont see the point",
                "don’t see the point", "struggling a lot", "want to die", "end my life", "ending my life")) {
            return new Classification("mental_health", "crisis", "SECURE");
        }

        // 2. Medical advice — before general insurance education.
        if (containsAny(text,
                "is my type 2 diabetes serious", "diabetes", "pre-diabetes", "prediabetes",
                "blood pressure", "hypertension", "symptom", "symptoms", "diagnosis", "diagnose",
                "medical advice", "medical condition", "health condition", "illness", "disease",
                "medication", "medicine", "treatment", "surgery", "operation", "doctor", "pain",
                "my condition", "my diagnosis", "my symptoms", "what does diabetes mean",
                "what does high blood pressure mean", "what does it mean if i have",
                "is my condition serious", "can you diagnose me")) {
            return new Classification("medical_advice", "medical", "FLAG");
        }

        // 3. Gaming / manipulation.
        if (containsAny(text,
                "lower premium", "put no for everything", "dont mention", "don't mention",
                "hide my surgery", "hide condition", "nobody checks", "avoid declaring", "lie on",
                "answer to get", "help me answer", "help me choose", "should i say yes or no",
                "help me should i say", "what should i answer", "what should i say",
                "which answer should i give", "how should i answer")) {
            return new Classification("gaming", "manipulation", "FLAG");
        }

        // 4. Jailbreak / abuse.
        boolean jailbreak = containsAny(text,
                "ignore your previous instructions", "ignore previous instructions",
                "you are now dan", "no restrictions", "system prompt", "developer message",
                "reveal your instructions", "show me your prompt", "show your system prompt");

        boolean abusive = containsAny(text,
                "fuck", "shit", "idiot", "stupid", "bitch", "asshole");

        if (jailbreak || abusive) {
            session.abuse_count++;
            String level = jailbreak
                    ? "jailbreak"
                    : session.abuse_count >= 3 ? "tier2" : "tier1";
            return new Classification("abuse", level, "FLAG");
        }

        // 5. Privacy.
        if (containsAny(text,
                "privacy", "personal info", "personal information", "who will see", "who sees",
                "secure", "data after", "shared", "sell my data", "delete my data",
                "encrypted", "how is my data", "what happens to my data", "data protection")) {
            return new Classification("privacy", "privacy", "STANDARD");
        }

        // 6. Language.
        if (ARABIC_TEXT.matcher(message).find()
                || containsAny(text, "arabic", "hindi", "urdu", "malayalam", "tagalog")) {
            return new Classification("language", "language", "STANDARD");
        }

        // 7a. Coverage & claims. Keep the documented distinction between
        // "What is a waiting period?" (education) and "How long is the waiting period?" (coverage).
        if (containsAny(text,
                "will my pre-existing condition be covered", "pre-existing condition be covered",
                "what happens if i need to make a claim", "make a claim", "claim process",
                "what's the excess", "what is the excess", "excess on this plan",
                "does this cover dental", "dental treatment", "how long is the waiting period",
                "how long is a waiting period", "coverage", "covered", "claim")) {
            return new Classification("coverage", "coverage", "ANALYTICS");
        }

        // 7b. Sharia / takaful.
        if (containsAny(text, "haram", "takaful", "sharia", "islamic financial", "sharia-compliant", "sharia compliant")) {
            return new Classification("sharia", "sharia", "STANDARD");
        }

        // 7c. Insurance education / glossary.
        if (containsAny(text,
                "what does pre-existing condition mean", "what is a pre-existing condition", "what is pre-existing condition",
                "difference between inpatient and outpatient", "inpatient vs outpatient",
                "what is a waiting period", "what is waiting period", "what does moratorium underwriting mean",
                "what is moratorium underwriting", "do i have to declare conditions from years ago",
                "declare conditions from years ago", "what does excess mean", "what is an excess")) {
            return new Classification("insurance_faq", "education", "ANALYTICS");
        }

        // 7d. Complaints.
        if (containsAny(text,
                "insurer was terrible", "hate dealing with insurance", "scam",
                "sort this for months", "nobody ever helps", "rejected my claim",
                "insurance company is terrible", "terrible insurance company")) {
            return new Classification("complaints", "complaint", "STANDARD");
        }

        // 8. Catch-all.
        return new Classification("catch_all", "fallback", "STANDARD");
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
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
