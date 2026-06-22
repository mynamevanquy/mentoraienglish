# mentoraienglish

## Groq AI

Chat, exercise generation, grammar explanation, vocabulary explanation, and grammar correction use Groq's OpenAI-compatible Chat Completions API.

Configure the API key with an environment variable. Do not commit real API keys.

```powershell
$env:GROQ_API_KEY="gsk_..."
mvn spring-boot:run
```

Default provider configuration:

```yaml
groq.ai.url: https://api.groq.com/openai/v1
groq.ai.model: llama-3.3-70b-versatile
```

Use separate Groq models for chat and exercise generation when needed:

```powershell
$env:GROQ_AI_MODEL="llama-3.3-70b-versatile"
$env:GROQ_AI_CHAT_MODEL="llama-3.3-70b-versatile"
$env:GROQ_AI_EXERCISE_MODEL="llama-3.3-70b-versatile"
```

## Adaptive learning

The application derives an internal recommendation level from the learner's latest credible result per exercise. It considers difficulty, score weight, recency, skill coverage, and minimum time spent. This recommendation personalizes AI chat, exercise generation, grammar, and vocabulary; it is not an official CEFR certificate.

```powershell
$env:ADAPTIVE_LEARNING_ENABLED="true"
$env:ADAPTIVE_INTERMEDIATE_MIN_ATTEMPTS="5"
$env:ADAPTIVE_INTERMEDIATE_MIN_SCORE="80"
$env:ADAPTIVE_ADVANCED_MIN_ATTEMPTS="8"
$env:ADAPTIVE_ADVANCED_MIN_SCORE="80"
```

## Password reset email

By default, local development does not require an SMTP server. The application
writes the one-time reset URL to the application log:

```text
Password reset mail is disabled. Development reset link for ...: http://localhost:8081/reset-password?token=...
```

To send real email through Mailpit/MailHog or a production provider, enable SMTP:

```powershell
$env:APP_BASE_URL="http://localhost:8081"
$env:PASSWORD_RESET_MAIL_ENABLED="true"
$env:MAIL_HOST="localhost"
$env:MAIL_PORT="1025"
$env:MAIL_FROM="no-reply@example.com"
$env:PASSWORD_RESET_TOKEN_VALIDITY="30m"
```

For authenticated SMTP, also set `MAIL_USERNAME`, `MAIL_PASSWORD`,
`MAIL_SMTP_AUTH=true`, and `MAIL_SMTP_STARTTLS=true`.
