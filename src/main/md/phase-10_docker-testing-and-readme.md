You are a senior DevOps and Java engineer.

Context: Spring Boot 3.2, PostgreSQL 15, Maven, Java 21

## Task
Generate deployment configuration, tests, and documentation.

## 1. Dockerfile
Multi-stage build:
Stage 1 (build): maven:3.9-eclipse-temurin-21, copy pom.xml first, download deps, then copy src, build JAR
Stage 2 (runtime): eclipse-temurin:21-jre-alpine, copy JAR, create non-root user, EXPOSE 8080, health check

## 2. docker-compose.yml
Services:
- app: build from Dockerfile, env from .env, depends_on postgres, health check
- postgres: postgres:15-alpine, volume for data persistence, health check
- (optional) redis: redis:7-alpine, for future session store

Volumes: postgres_data
Networks: app-network (bridge)

## 3. Unit Tests
Generate tests for critical business logic:

SM2AlgorithmTest.java:
- Test all quality levels (0-5)
- Test EF floor (never < 1.3)
- Test interval progression for perfect recall (q=5)
- Test reset on failure (q < 3)

ExerciseGraderTest.java:
- Test MULTIPLE_CHOICE exact match
- Test FILL_BLANK case insensitive
- Test FILL_BLANK with Levenshtein tolerance
- Test SENTENCE_REORDER correct/incorrect order

PromptBuilderTest.java:
- Test variable replacement
- Test prompt injection detection
- Test sanitization of malicious input

DashboardServiceTest.java:
- Test streak calculation (consecutive days)
- Test streak reset on gap
- Test today = day 1

Use: JUnit 5, Mockito, @ExtendWith(MockitoExtension.class)
No Spring context in unit tests (pure unit tests).

## 4. Integration Test skeleton
EnglishAiApplicationTests.java:
- @SpringBootTest with TestContainers (PostgreSQL)
- Basic smoke test: context loads
- AuthController IT: register → login → access dashboard → logout

## 5. README.md
Sections:
- Project overview (what it is, main features)
- Tech stack table
- Prerequisites (Java 21, Docker, PostgreSQL, OpenAI API key)
- Quick start with Docker Compose
- Quick start without Docker (manual setup steps)
- Environment variables table (name, description, required, default)
- Project structure explanation
- API key setup (OpenAI)
- Flyway migration notes
- Known limitations / TODO list
- License

## Constraints
- Dockerfile must produce image < 300MB (use JRE not JDK in runtime stage)
- docker-compose must work with `docker compose up` (no additional steps)
- Tests must be runnable with `mvn test` without external dependencies
- README must be accurate — no placeholder sections