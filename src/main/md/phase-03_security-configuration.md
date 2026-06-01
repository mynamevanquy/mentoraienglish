You are a senior Spring Security 6 engineer.

Context:
- Spring Boot 3.2, Spring Security 6
- Session-based auth (NOT JWT)
- Thymeleaf + HTMX frontend (some requests are HTMX partial requests)
- Roles: USER, ADMIN
- Password hashing: BCrypt strength 12
- Rate limiting: Bucket4j (in-memory for dev)
- CSRF: enabled for forms, must handle HTMX requests correctly

## Task
Generate complete security configuration.

## 1. SecurityConfig.java
Configure SecurityFilterChain:
- Public: /, /login, /register, /pricing, /css/**, /js/**, /images/**
- Authenticated: /dashboard/**, /vocabulary/**, /grammar/**, /exercises/**, /conversation/**, /profile/**, /progress/**
- Admin only: /admin/**
- Login page: /login, default success: /dashboard, failure: /login?error
- Logout: /logout → redirect /login?logout, clear session
- Remember me: 30 days, use DB token store
- Session: max 1 session per user, expired redirect /login?expired
- CSRF: enabled, add CsrfToken to model for Thymeleaf, exempt /api/** if needed
- Security headers: X-Frame-Options DENY, X-Content-Type-Options nosniff, HSTS

## 2. UserDetailsServiceImpl.java
Load user by email, map roles to GrantedAuthority.

## 3. AuthController.java
Endpoints:
- GET /login → login page
- GET /register → register page
- POST /register → validate, create user, auto-login, redirect /dashboard
- Handle HTMX requests: if HX-Request header present, return fragment not full page

## 4. RegisterRequest.java (DTO)
Fields: fullName, email, password, confirmPassword
Validation: @NotBlank, @Email, @Size(min=8), custom @PasswordMatch constraint

## 5. RateLimitingFilter.java
- Apply to /login (POST) and /register (POST): max 10 req/min per IP
- Use Bucket4j with in-memory store
- Return 429 with proper message if exceeded

## 6. HtmxCsrfFilter or configuration
Ensure CSRF token is propagated correctly for HTMX requests
(HTMX sends HX-Request header, needs CSRF token in response header or meta tag)

## Constraints
- No @PreAuthorize on controllers in this phase (use URL-based security)
- BCryptPasswordEncoder bean must be in separate @Configuration to avoid circular dependency
- Show exact import statements for Spring Security 6 (package names changed from 5)