You are a senior Java Spring Boot engineer.

Context: Spring Boot 3.2, Java 21, JPA, Thymeleaf, HTMX, Bootstrap 5, Server-Sent Events

## Task
Generate the complete conversation/ module — real-time AI English tutor chat.

## 1. ConversationService.java
Methods:
- ConversationDto createConversation(UUID userId, String title)
- Page<ConversationDto> getUserConversations(UUID userId, Pageable pageable)
- ConversationDetailDto getConversationWithMessages(UUID conversationId, UUID userId)
- MessageResponseDto sendMessage(UUID userId, UUID conversationId, String userMessage)
  * Save user message to DB
  * Build conversation history (last 20 messages for context window)
  * Call AiService.chat()
  * Save assistant message with grammar_corrections and vocabulary_suggestions
  * Update conversation total_tokens_used
  * Return assistant message DTO
- void archiveConversation(UUID conversationId, UUID userId)
- void deleteConversation(UUID conversationId, UUID userId)

## 2. System prompt for English tutor
Build a system prompt that instructs GPT-4o to:
- Act as a friendly English tutor named "Aria"
- User's level: {userLevel} (injected from user profile)
- After each user message: provide natural response, then optionally add:
  [CORRECTIONS]: point out grammar mistakes if any, explain briefly
  [VOCABULARY]: suggest 1-2 advanced words related to topic
- Keep corrections encouraging, not discouraging
- Respond in English, corrections can be bilingual (EN + VI explanation)
- Never break character

## 3. ConversationController.java
Endpoints:
- GET /conversation — conversation list page
- POST /conversation/new — create new, redirect to chat page
- GET /conversation/{id} — chat page (full page)
- POST /conversation/{id}/message — HTMX: send message, return assistant message partial
- DELETE /conversation/{id} — archive conversation
- GET /conversation/{id}/messages — HTMX: load more messages (pagination)

## 4. Thymeleaf templates
conversation/index.html — list of conversations with preview
conversation/chat.html — chat UI: message history + input box
conversation/fragments/message-bubble.html — single message (user/assistant styling)
conversation/fragments/correction-panel.html — shows [CORRECTIONS] block if present
conversation/fragments/typing-indicator.html — shown while waiting for AI response

## HTMX chat pattern:
1. User types message, clicks Send
2. hx-post to /conversation/{id}/message
3. hx-target="#message-list" hx-swap="beforeend"
4. Show typing indicator via hx-indicator
5. Response: render message-bubble partial for assistant message
6. Auto-scroll to bottom via custom JS after HTMX swap

## 5. MessageResponseDto
Fields:
- UUID messageId
- String content
- List<GrammarCorrectionDto> corrections (parsed from [CORRECTIONS] block)
- List<String> vocabularySuggestions (parsed from [VOCABULARY] block)
- Instant createdAt

## Constraints
- Max conversation history sent to AI: 20 messages (sliding window)
- If conversation has no messages, show welcome prompt suggestions
- User can only access their own conversations (check userId in service layer)
- Token count per conversation: warn user when approaching 80% of monthly limit