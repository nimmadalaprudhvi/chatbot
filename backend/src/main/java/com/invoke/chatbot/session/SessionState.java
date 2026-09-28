package com.invoke.chatbot.session;

import com.invoke.chatbot.chat.dto.Classification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionState {

    public final String session_id;
    public int question_index;
    public String applicant_name;
    public final Map<String, String> answers;
    public String language;
    public String status;
    public int abuse_count;
    public Classification last_classification;
    public final List<Map<String, Object>> events;

    public SessionState(String sessionId) {
        this.session_id = sessionId;
        this.question_index = 0;
        this.applicant_name = null;
        this.answers = new LinkedHashMap<>();
        this.language = "en";
        this.status = "active";
        this.abuse_count = 0;
        this.events = new ArrayList<>();
    }
}
