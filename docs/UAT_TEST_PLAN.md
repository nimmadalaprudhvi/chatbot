# invoke.ai UAT Test Plan

## Purpose

Validate the Spring Boot UAT build against the documented off-topic handling behavior and the representative journey.

## Scope

- Normal journey progression
- Applicant name capture
- Context preservation
- Categories 1–10
- Classifier priority
- Crisis pause
- Gaming prevention
- Privacy responses
- Language detection
- Catch-all
- Review/reset behavior
- Local audit logging
- API health and session creation

## Out of scope

- Production insurer APIs
- Final underwriting rules
- Production database
- Production authentication
- Production cloud deployment
- Final legal/compliance approval

## Execution order

1. Start Spring Boot.
2. Run `uat-smoke-test.ps1`.
3. Complete the manual tests in `README.md`.
4. Record actual results in the UAT checklist.
5. Raise defects for failures.
6. Re-run regression tests after fixes.
7. Obtain business/compliance sign-off.

## Severity guidance

**Critical**
- Crisis flow does not pause.
- Internal prompt/configuration is exposed.
- Applicant data is disclosed incorrectly.
- Journey advances after a high-risk off-topic request.

**High**
- Wrong classifier category for a launch-blocking scenario.
- Journey context is lost.
- Privacy response is materially incorrect.
- Session data is lost unexpectedly during the active UAT session.

**Medium**
- Incorrect wording or wrong non-blocking FAQ mapping.
- Language indicator does not update.

**Low**
- Cosmetic or copy issues that do not affect the workflow.
