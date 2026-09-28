$ErrorActionPreference = "Stop"

Write-Host "Checking invoke.ai UAT health..." -ForegroundColor Cyan

$health = Invoke-RestMethod -Uri "http://localhost:8000/api/health" -Method Get

if ($health.status -ne "ok") {
    throw "Health check failed."
}

if ($health.environment -ne "uat") {
    throw "Expected environment=uat but received '$($health.environment)'."
}

Write-Host "Health: PASS" -ForegroundColor Green

$session = Invoke-RestMethod -Uri "http://localhost:8000/api/session" -Method Get
$sessionId = $session.snapshot.session_id

if ([string]::IsNullOrWhiteSpace($sessionId)) {
    throw "Session creation failed."
}

Write-Host "Session creation: PASS" -ForegroundColor Green

function Send-Chat($message) {
    $body = @{
        session_id = $sessionId
        message = $message
    } | ConvertTo-Json

    return Invoke-RestMethod `
        -Uri "http://localhost:8000/api/chat" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body
}

$tests = @(
    @{ Message = "What does diabetes mean?"; Expected = "medical_advice" },
    @{ Message = "Who will see my answers?"; Expected = "privacy" },
    @{ Message = "Can you help me answer to get a lower premium?"; Expected = "gaming" },
    @{ Message = "Can I do this in Arabic?"; Expected = "language" },
    @{ Message = "What is the weather today?"; Expected = "catch_all" }
)

foreach ($test in $tests) {
    $response = Send-Chat $test.Message

    if ($response.classification.category -ne $test.Expected) {
        throw "FAILED: '$($test.Message)' expected '$($test.Expected)' but received '$($response.classification.category)'."
    }

    Write-Host "PASS: $($test.Message) -> $($test.Expected)" -ForegroundColor Green
}

Write-Host ""
Write-Host "UAT smoke test completed successfully." -ForegroundColor Green
