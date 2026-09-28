@echo off
cd /d "%~dp0backend"
echo Starting invoke.ai Spring Boot chatbot on http://localhost:8000
where mvn >nul 2>nul
if errorlevel 1 (echo Maven was not found on PATH. Install Maven 3.9+ and ensure mvn works in PowerShell.& pause & exit /b 1)
mvn spring-boot:run
