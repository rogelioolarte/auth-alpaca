# Scheduled Cleanup for Revoked Tokens & Sessions

> [README](../README.md) — [Backend Architecture](backend-architecture.md) — **Scheduled Cleanup**

The system runs automatic scheduled jobs to purge stale revoked data from the database. This prevents unbounded growth of `refresh_tokens` and `sessions` tables and ensures revoked credentials are removed within a predictable window.

---

## Why cleanup matters

| Risk | Impact |
|------|--------|
| **Table bloat** | Every login rotation creates 1–2 new rows in `refresh_tokens`. Without cleanup, revoked rows accumulate and degrade query performance over time. |
| **Stale credentials** | Revoked tokens still stored in the database are a forensic liability. A dump or SQL injection could expose hashes that, while not directly usable, should not persist. |
| **Audit noise** | Revoked sessions that remain indefinitely clutter administrative views and make active session monitoring harder. |

---

## Schedules

| Job | Cron (UTC) | Frequency | Target |
|-----|-----------|-----------|--------|
| `purgeRevokedRefreshTokens` | `0 0 0 * * ?` | **Daily** at midnight | `DELETE FROM refresh_tokens WHERE revoked = true` |
| `purgeRevokedSessions` | `0 0 0 * * 0` | **Weekly** on Sunday at midnight | `DELETE FROM sessions WHERE revoked = true` |

### Why different schedules?

- **Refresh tokens daily** — They are created on every token rotation (potentially multiple times per login), so the table grows faster. A daily purge keeps the table lean and limits the exposure window of any compromised but revoked token.
- **Sessions weekly** — Sessions are more stable (one per login + device). A weekly purge is sufficient and reduces unnecessary I/O on the `sessions` table, which is queried more frequently during authentication.

---

## How it works

The job runs inside the application process using Spring's `@Scheduled` annotation — no external cron daemon or database extension (`pg_cron`) is required.

```
Application startup
  → @EnableScheduling (SchedulingConfig)
    → ThreadPoolTaskScheduler picks up cron expressions
      → CleanupScheduler.purgeRevokedRefreshTokens()  [daily]
      → CleanupScheduler.purgeRevokedSessions()        [weekly]
```

Each method executes a **single bulk JPQL `DELETE`** — no entity loading, no row-by-row deletion:

```java
@Modifying(clearAutomatically = true)
@Query("DELETE FROM RefreshToken r WHERE r.revoked = true")
int deleteRevoked();
```

The `clearAutomatically = true` flag ensures the JPA persistence context is refreshed after the bulk delete, so subsequent queries in the same transaction see the correct state.

---

## What gets deleted

**Refresh tokens:** Any row where `revoked = true`, regardless of age — tokens revoked 5 minutes ago are cleaned up alongside tokens revoked months ago. The entire revoked set is purged every daily run.

**Sessions:** Any row where `revoked = true`, purged weekly.

> If a retention period is needed (e.g., keep revoked tokens for 30 days for audit), the `WHERE` clause can be extended to `revoked = true AND revoked_at < :cutoff`.

---

## Monitoring

The scheduler logs at `INFO` level whenever it deletes at least one row:

```
Purged 5 revoked refresh tokens
Purged 3 revoked sessions
```

When there is nothing to clean up, the job runs silently — no log noise.

---

## Disabling or customizing

To **disable** the scheduler entirely, set in `application.properties`:

```properties
spring.task.scheduling.pool.size=0
```

Or remove the `@EnableScheduling` annotation from `SchedulingConfig`.

To **change the schedule**, edit the `cron` expression in `CleanupScheduler.java`. The `zone` attribute is set to `"UTC"` — adjust if your deployment uses a different timezone.

---

## Source files

| File | Role |
|------|------|
| `config/SchedulingConfig.java` | Enables `@Scheduled` support |
| `config/CleanupScheduler.java` | Scheduled job definitions |
| `repository/RefreshTokenRepo.java` | Bulk delete query for refresh tokens |
| `repository/SessionRepo.java` | Bulk delete query for sessions |
