You are a senior Java Spring Boot engineer.

Context: Spring Boot 3.2, Java 21, JPA, Thymeleaf, HTMX, Bootstrap 5, Chart.js

## Task
Generate dashboard/ and progress/ modules.

## 1. DashboardService.java
Methods (all queries must be efficient — use JPQL aggregation, NOT N+1):
- DashboardSummaryDto getSummary(UUID userId)
  Fields:
  * todayStudyMinutes (from study_sessions WHERE date = today)
  * currentStreakDays (consecutive days with study_sessions)
  * vocabularyMastered (count user_vocabularies WHERE mastery_level >= 4)
  * vocabularyDueReview (count WHERE next_review_at <= NOW())
  * exercisesCompletedThisWeek (count exercise_attempts this week)
  * averageExerciseScore (avg score this week)
  * totalXp (sum xp from learning_progress)
  * activeConversations (count conversations WHERE status=ACTIVE)

- List<RecentActivityDto> getRecentActivity(UUID userId, int limit)
  Combine study_sessions + exercise_attempts into unified activity feed

- WeeklyProgressDto getWeeklyProgress(UUID userId)
  Study minutes per day for last 7 days (for bar chart)

- List<WeakTopicDto> getWeakGrammarTopics(UUID userId, int limit)
  Grammar topics with lowest exercise scores

- int calculateStreakDays(UUID userId)
  Count consecutive days backwards from today with at least one study_session

## 2. DashboardController.java
Endpoints:
- GET /dashboard — full dashboard page
- GET /dashboard/summary — HTMX partial: summary cards
- GET /dashboard/weekly-chart — HTMX partial: Chart.js data as JSON + canvas
- GET /dashboard/recent-activity — HTMX partial: activity feed
- GET /dashboard/weak-topics — HTMX partial: weak topics list

## 3. ProgressService.java
Methods:
- void recordStudySession(UUID userId, ActivityType type, UUID activityId, int durationMinutes, int xpEarned)
- void updateDailyProgress(UUID userId, MetricType metric, BigDecimal value)
  (UPSERT: INSERT ... ON CONFLICT (user_id, metric_type, recorded_date) DO UPDATE)
- ProgressHistoryDto getProgressHistory(UUID userId, MetricType metric, LocalDate from, LocalDate to)
- int calculateLevel(int totalXp) — XP thresholds: 0/100/300/600/1000/2000/5000

## 4. Thymeleaf templates
dashboard/index.html — main dashboard
dashboard/fragments/summary-cards.html — 4 stat cards (streak, vocab, exercises, XP)
dashboard/fragments/weekly-chart.html — Chart.js bar chart (study minutes)
dashboard/fragments/recent-activity.html — activity timeline
dashboard/fragments/weak-topics.html — grammar weakness list
progress/index.html — detailed progress page
progress/fragments/progress-chart.html — multi-metric line chart

## Chart.js integration
Pass data as Thymeleaf model attributes, render as JSON in <script> block:
```javascript
const weeklyData = /*[[${weeklyChartJson}]]*/ {};
new Chart(ctx, { type: 'bar', data: weeklyData });
```

## Constraints
- Streak calculation: must handle timezone correctly (use user's timezone, default Asia/Ho_Chi_Minh)
- DashboardSummaryDto must load in single DB round-trip per method (no lazy loading)
- UPSERT for learning_progress must use PostgreSQL ON CONFLICT syntax via @Query nativeQuery=true
- XP system: lesson complete=10, exercise complete=5*(score/maxScore)*10, vocabulary review=2