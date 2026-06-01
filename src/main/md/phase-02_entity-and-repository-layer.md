You are a senior Java Spring Boot engineer.

Context:
- Spring Boot 3.2, Java 21, Hibernate 6, Lombok, MapStruct
- All entities use UUID PK
- Soft delete via @Where(clause = "deleted_at IS NULL") + @SQLDelete
- Audit via Spring Data @CreatedDate/@LastModifiedDate + @EnableJpaAuditing
- Fetch strategy: LAZY by default, EAGER only when justified

## Task
Generate the complete entity and repository layer.

## Step 1: BaseEntity
Generate abstract class BaseEntity with:
- @Id UUID id (pre-generated via UUID.randomUUID())
- @CreatedDate Instant createdAt
- @LastModifiedDate Instant updatedAt
- Instant deletedAt
- String createdBy, updatedBy (via AuditorAware)
- void softDelete() method

## Step 2: Enums
Generate all enums:
- Role: USER, ADMIN
- Level: BEGINNER, INTERMEDIATE, ADVANCED
- ContentType: TEXT, VIDEO, AUDIO, IMAGE
- ExerciseType: MULTIPLE_CHOICE, FILL_BLANK, SENTENCE_REORDER, WRITING, LISTENING
- ExerciseSource: MANUAL, AI_GENERATED
- AttemptStatus: IN_PROGRESS, COMPLETED, ABANDONED
- MessageRole: USER, ASSISTANT, SYSTEM
- ConversationStatus: ACTIVE, ARCHIVED
- AiLogStatus: SUCCESS, ERROR
- ActivityType: LESSON, EXERCISE, VOCABULARY, CONVERSATION, GRAMMAR
- MetricType: VOCABULARY_MASTERED, EXERCISES_COMPLETED, STREAK_DAYS, XP_TOTAL, LESSONS_COMPLETED
- SubscriptionPlan: FREE, PRO, PREMIUM
- SubscriptionStatus: ACTIVE, CANCELLED, EXPIRED

## Step 3: Entities
Generate full entity classes for all 21 tables.
For JSONB columns, use @Type(JsonBinaryType.class) from hibernate-types.
For collections mapped from JSONB, use List<String> or custom record classes.

## Step 4: Repositories
For each entity, generate Spring Data JPA repository interface with:
- Extends JpaRepository<Entity, UUID>
- Custom @Query methods needed for business logic:
  * UserRepository: findByEmail, existsByEmail
  * UserVocabularyRepository: findDueForReview(userId, now) — spaced repetition query
  * ExerciseAttemptRepository: findRecentByUser(userId, limit)
  * ConversationRepository: findActiveByUser(userId)
  * LearningProgressRepository: findByUserAndDateRange(userId, from, to)
  * AiLogRepository: sumTokensByUserAndMonth(userId, month)

## Constraints
- No @Data on entities (use @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor)
- bidirectional relationships must have proper mappedBy + helper methods
- toString() must NOT include lazy collections
- equals/hashCode based on id only