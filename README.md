# Invoke.AI Health Insurance Chatbot — Spring Boot UAT UI

This package contains the Spring Boot UAT chatbot with a customer-facing interface inspired by the existing Invoke.AI website chatbot widget.

## Customer flow

1. The opening screen shows two actions:
   - Start application
   - Ask a question about insurance
2. Start application creates a session and begins the underwriting journey.
3. The applicant name is collected first.
4. The date-of-birth step uses the browser's native calendar/date picker.
5. Choice questions render clickable options.
6. Text questions remain conversational through the chat input.
7. Off-topic questions during an application continue through the documented classifier order and return to the exact current journey question.
8. Ask a question about insurance uses a dedicated `/api/insurance-question` endpoint so education, coverage, privacy, medical, Sharia/takaful, language, abuse and complaint questions are classified independently of underwriting answers.
9. In question mode, documented insurance education answers are returned directly without accidentally advancing the underwriting journey.
10. In application mode, underwriting answers still use `/api/chat` and the current journey state is preserved after off-topic handling.

## Run

From the project root:

```powershell
.\run.bat
```

Then open:

```text
http://localhost:8000
```

## Notes

- No AWS Bedrock or external LLM is used.
- No database is used; sessions remain in memory for this UAT build.
- The UI uses the existing Spring Boot API and journey configuration.
- The website's chatbot visual language was reused: compact header, primary action card, secondary action card, restrained typography, rounded controls, and a compact chat composer.

## Documentation alignment

The implementation follows the supplied Off-Topic Handling Specification for the classifier order, the 10 categories, context preservation, and the documented Q&A mappings. The supplied mind map is treated as documentation rather than customer-facing UI.

This remains a local UAT implementation: it does not include Bedrock or an external LLM, persistent database storage, production access controls, legal/DPO approval, or verified production crisis-contact configuration. Those items must be completed before production release where required by the specification.
