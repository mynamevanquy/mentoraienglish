You are a senior frontend engineer specializing in Thymeleaf + HTMX + Bootstrap 5.

Context: Spring Boot 3.2, Thymeleaf Layout Dialect, HTMX 1.9, Bootstrap 5.3

## Task
Generate complete layout system and common UI components.

## 1. Base layout: templates/layouts/main.html
Structure:
- <head>: Bootstrap 5.3 CDN, HTMX CDN, Chart.js CDN, custom CSS link, CSRF meta tag for HTMX
- <body>:
  * Navbar (fragment)
  * Sidebar (fragment, collapsible on mobile)
  * Main content area: layout:fragment="content"
  * Footer (fragment)
  * Toast notification area (for HTMX success/error responses)
- Include hx-headers meta tag to send CSRF token with every HTMX request

## 2. templates/layouts/auth.html
Minimal layout for login/register: centered card, no sidebar

## 3. Fragments
templates/fragments/navbar.html:
- Brand logo left
- Nav links: Dashboard, Vocabulary, Grammar, Exercises, AI Chat, Progress
- Right: user avatar dropdown (Profile, Settings, Logout)
- Bootstrap navbar-toggler for mobile

templates/fragments/sidebar.html:
- Vertical nav with icons (Bootstrap Icons)
- Active state based on current URL: th:classappend="${#httpServletRequest.requestURI.startsWith('/vocabulary')} ? 'active'"
- Collapse button for wide sidebar / icon-only mode

templates/fragments/footer.html: minimal

templates/fragments/pagination.html:
- Reusable pagination component
- Accepts: currentPage, totalPages, baseUrl
- Generate page links with HTMX support

templates/fragments/loading-spinner.html:
- HTMX indicator spinner
- CSS class: htmx-indicator (hidden by default, shown during requests)

templates/fragments/toast.html:
- Bootstrap toast for success/error messages
- HTMX response header: HX-Trigger → {"showToast": {"message": "...", "type": "success"}}
- JS listener on body for showToast event

## 4. Public pages
templates/public/home.html — landing page (hero, features, pricing preview, CTA)
templates/public/login.html — login form with remember-me
templates/public/register.html — register form with client-side validation
templates/public/pricing.html — 3 pricing tiers (FREE/PRO/PREMIUM)

## 5. Error pages
templates/error/404.html
templates/error/500.html
templates/error/403.html

## 6. HTMX global configuration in layout
```html
<meta name="csrf-token" th:content="${_csrf.token}">
<script>
  // Send CSRF token with every HTMX request
  document.addEventListener('htmx:configRequest', function(e) {
    e.detail.headers['X-CSRF-TOKEN'] = document.querySelector('meta[name="csrf-token"]').content;
  });
  // Show toast on HX-Trigger header
  document.addEventListener('showToast', function(e) {
    // Bootstrap toast logic
  });
</script>
```

## Constraints
- NO inline styles — use Bootstrap utility classes only
- All forms must have th:action and th:method (not hardcoded)
- Sidebar active state must work for nested URLs (/vocabulary/review → sidebar vocabulary active)
- Mobile responsive: sidebar collapses to bottom nav or hamburger
- Dark mode: NOT required in this phase