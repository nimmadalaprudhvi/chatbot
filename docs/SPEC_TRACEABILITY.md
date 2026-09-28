# Spec traceability

| Source category | Local implementation | Implementation behavior |
|---|---|---|
| 1 Medical Advice | `medical_advice` classifier | Deflect; preserve current question |
| 2 Mental Health & Crisis | `mental_health` classifier | Pause journey; secure/restricted logging |
| 3 Gaming / Manipulation | `gaming` classifier | Firm factual redirect; log flagged event |
| 4 Privacy & Data | `privacy` classifier | Answer from approved mapping; return to current question |
| 5 Coverage & Claims | `coverage` classifier | Soft deflect; analytics log |
| 6 Language Switching | `language` classifier | Detect Arabic/Hindi and retain language state |
| 7 Sharia / Takaful | `sharia` classifier | Respectful broker/takaful redirect |
| 8 Bot Abuse / Jailbreak | `abuse` classifier | Tiered handling; jailbreak framing ignored |
| 9 Insurance Education | `insurance_faq` classifier + `/api/insurance-question` | Direct glossary answer; analytics log; question mode does not advance underwriting |
| 10 Insurer Complaints | `complaints` classifier | Brief acknowledgement + redirect |
| Catch-all | `catch_all` | Return to exact current question |

## Classifier order

1. Crisis / mental health
2. Medical advice
3. Gaming / manipulation
4. Jailbreak / abuse
5. Privacy
6. Language
7. Standard intents
8. Catch-all

## Important implementation note

The supplied source documents specify the off-topic handling system and Q&A mappings. They do **not** provide the complete official underwriting journey. Therefore the journey in `backend/src/main/java/com/invoke/chatbot/chat/ChatService.java` is a clearly marked representative journey and should be replaced with the manager/business-approved journey when provided.


## Question mode

The customer-facing **Ask a question about insurance** action uses a dedicated endpoint so an insurance question cannot accidentally be interpreted as an answer to the current underwriting step. In application mode, `/api/chat` remains responsible for journey answers and off-topic handling.
