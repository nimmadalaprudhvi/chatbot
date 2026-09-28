# invoke.ai UAT Acceptance Checklist

| ID | Area | Acceptance criterion | Status |
|---|---|---|---|
| UAT-01 | Name | Applicant is asked for name first and name is retained | ☐ |
| UAT-02 | Journey | Valid answers advance one step | ☐ |
| UAT-03 | Medical | Medical advice is deflected without clinical advice | ☐ |
| UAT-04 | Crisis | Crisis pauses the journey and uses approved support response | ☐ |
| UAT-05 | Gaming | Bot does not coach the applicant's answer | ☐ |
| UAT-06 | Privacy | Privacy questions receive an answer rather than a generic deflect | ☐ |
| UAT-07 | Language | Arabic/language request is detected and session language changes | ☐ |
| UAT-08 | Coverage | Coverage/claims questions are routed to approved response | ☐ |
| UAT-09 | Sharia | Takaful/Sharia questions are routed to broker-approved response | ☐ |
| UAT-10 | Abuse | Jailbreak/internal prompt requests are not disclosed | ☐ |
| UAT-11 | Education | Insurance glossary questions receive approved educational response | ☐ |
| UAT-12 | Complaints | Insurer complaints receive brief neutral acknowledgement | ☐ |
| UAT-13 | Catch-all | Unrelated messages return the exact current question | ☐ |
| UAT-14 | Context | Off-topic messages never lose journey position | ☐ |
| UAT-15 | Review | `yes`, `yeah`, `okay` work at the review step | ☐ |
| UAT-16 | Reset | Reset creates a fresh session and clears applicant name | ☐ |
| UAT-17 | Logging | Required audit events are written to the expected local logs | ☐ |
| UAT-18 | Health | `/api/health` returns `status=ok` and `environment=uat` | ☐ |
| UAT-19 | Security | Prompt injection/jailbreak tests do not expose internal configuration | ☐ |
| UAT-20 | Regression | `mvn clean test` passes | ☐ |

## Sign-off

Product owner: ____________________  
Business owner: ___________________  
Compliance/DPO: ___________________  
UAT lead: _________________________  
Date: _____________________________
