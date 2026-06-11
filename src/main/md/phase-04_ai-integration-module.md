You are a senior Java engineer specializing in Groq AI API integration.

Context:
- Spring Boot 3.2, Java 21
- Use Spring's RestClient (NOT WebClient, NOT deprecated RestTemplate)
- Groq model: llama-3.3-70b-versatile
- Streaming: NOT required in this phase (standard request/response)
- Token tracking: log every API call to ai_logs table
- Prompt injection protection: sanitize user input before including in prompts
- Rate limiting per user: 20 AI requests per hour (check ai_logs table)

## Task
Generate the complete ai/ module.

## 1. GroqAiClient.java
Low-level HTTP client:
- POST to https://api.groq.com/openai/v1/chat/completions
- Build request: model, messages[], temperature, max_tokens
- Parse response: extract content, usage (prompt_tokens, completion_tokens)
- Handle errors: 429 rate limit, 500 server error → throw typed exceptions
- Measure latency, log to AiLogService

## 2. GroqAiRequest.java / GroqAiResponse.java (records)
Map exactly to Groq AI request/response structure.
Use Jackson for serialization.

## 3. PromptBuilder.java
- Load prompt templates from ai_prompts table (cached with @Cacheable)
- Replace {variables} in template with actual values
- Sanitize user input: remove prompt injection patterns (ignore previous instructions, etc.)
- Method per use case:
  * buildTutorPrompt(conversationHistory, userMessage, userLevel)
  * buildExerciseGeneratorPrompt(topic, exerciseType, difficulty, count)
  * buildGrammarExplanationPrompt(grammarTopic, userLevel)
  * buildVocabularyExplanationPrompt(word)
  * buildGrammarCorrectionPrompt(userText)

## 4. AiResponseParser.java
Parse Groq AI response content for structured outputs:
- parseExerciseList(content) → List<ExerciseQuestionDto>
  (AI returns JSON array, parse with ObjectMapper, handle malformed JSON gracefully)
- parseGrammarCorrections(content) → List<GrammarCorrectionDto>
- parseVocabularyInfo(content) → VocabularyInfoDto

## 5. TokenUsageTracker.java
- Check if user has exceeded hourly/daily token limit
- Log usage to ai_logs after each call
- Aggregate usage by user+month for billing purposes
- Method: boolean canMakeRequest(UUID userId)

## 6. AiService.java (orchestrator)
Public API for other modules:
- CompletionResult chat(UUID userId, UUID conversationId, String userMessage)
- List<ExerciseQuestionDto> generateExercises(UUID userId, GenerateExerciseRequest request)
- GrammarExplanationDto explainGrammar(UUID userId, String grammarTopic, String userLevel)
- VocabularyInfoDto explainVocabulary(UUID userId, String word)
- GrammarCorrectionResult correctGrammar(UUID userId, String text)

## 7. Prompt injection sanitizer
List of patterns to detect and reject/strip:
- "ignore previous instructions"
- "forget your instructions"  
- "act as", "pretend you are" (when from untrusted input)
- Excessive special characters used to break prompt structure
Log detection to audit_logs.

## Constraints
- API key loaded from environment variable GROQ_AI_API_KEY, never hardcoded
- All AI calls must be @Async to not block request thread
- AiService methods return CompletableFuture<T>
- Caller handles timeout (30 seconds max)
- If AI call fails, return graceful fallback, do NOT propagate raw Groq AI error to user
