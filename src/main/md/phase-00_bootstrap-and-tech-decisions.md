You are a senior Java Spring Boot architect.

Initialize a production-ready Spring Boot project with the following exact specifications:

## Tech Stack
- Java 21 (use virtual threads via Loom where applicable)
- Spring Boot 3.2.x
- Spring MVC (NOT WebFlux)
- Thymeleaf 3 + Layout Dialect
- HTMX 1.9.x (loaded via CDN in layout)
- Bootstrap 5.3.x (loaded via CDN)
- PostgreSQL 15
- Spring Security 6
- Spring Data JPA + Hibernate 6
- Flyway 9
- Groq AI / Groq REST API via Spring RestClient
- Maven (NOT Gradle)
- Lombok
- MapStruct
- Bucket4j (rate limiting)
- Spring Session (in-memory for dev, Redis-ready for prod)

## Deliverable for this phase
1. Complete pom.xml with all dependencies and versions pinned
2. application.yml (dev profile)
3. application-prod.yml (prod profile, no secrets hardcoded)
4. .env.example listing all required environment variables
5. Project directory tree (text format, all packages listed)

## Package root
com.englishai

## Module structure (domain-driven)
com.englishai
├── auth/         (controller, service, dto, entity, repository)
├── user/
├── ai/           (google ai client, prompt builder, response parser, token tracker)
├── course/
├── lesson/
├── vocabulary/
├── grammar/
├── exercise/
├── conversation/
├── dashboard/
├── progress/
├── common/       (base entity, audit, pagination, exception, response wrapper)
└── config/       (security, flyway, google ai, bucket4j, async, thymeleaf)

## Constraints
- Use UUID (not Long) for all primary keys
- All entities extend BaseEntity (id, createdAt, updatedAt, deletedAt for soft delete)
- No business logic in controllers
- No direct Groq AI call outside ai/ module

## Output format
- Show pom.xml in full
- Show application.yml in full
- Show .env.example in full
- Show directory tree as plain text
- Add short comment explaining non-obvious dependency choices
