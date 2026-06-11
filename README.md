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
