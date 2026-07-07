# Troubleshooting

> [README](README.md) — **Troubleshooting**

Common issues and how to resolve them.

---

## "Connection refused" or backend won't start

**Cause**: PostgreSQL is not running or the connection URL is wrong.

**Check**:
```bash
# Is the database container running?
docker ps | grep postgres

# Can you reach it?
nc -zv localhost 5432
```

**Fix**:
- Start the container: `docker compose up -d postgres-db`
- Or override `SPRING_DATASOURCE_URL` in your `.env` to point to the right host.

---

## Keys not found (`FileNotFoundException` for `.pem` files)

**Cause**: JWT key pairs were not generated before starting the backend.

**Fix**:
```bash
./generate_keys.sh -L secrets/
```

The backend expects the four `.pem` files in the directory mapped to `/keys` (Docker) or at `secrets/` (local dev).

---

## Flyway migration fails

**Symptoms**: `FlywayException`, "Migration failed", or "Schema history table already exists".

**Causes and fixes**:

| Situation | Fix |
|---|---|
| First run with an existing database from a different version | `docker compose down -v` to wipe volumes, then `docker compose up -d --build` |
| Local dev after schema changes | Drop and recreate the `public` schema: `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` |

---

## "Invalid Signature" / JWT errors in logs

**Cause**: The backend was started with one set of EC keys, then the keys were regenerated, invalidating existing tokens.

**Fix**: Log out and log in again. Tokens are short-lived (5 min access, 12 h refresh) so the issue resolves on its own.

---

## Port already in use

**Symptom**: `Address already in use` or `port is already allocated`.

**Check and fix**:
```bash
# Find what's on port 8080 (backend)
sudo lsof -i :8080

# Or 5432 (database)
sudo lsof -i :5432

# Kill the process or change the port in your .env and application.properties
```

---

## Docker build fails or is slow

**Common fixes**:
- Ensure Docker is running: `docker info`
- Check disk space: `df -h`
- Rebuild without cache: `docker compose build --no-cache`

---

## Tests fail locally

**Unit tests** (`-Punit-tests`): Should run without Docker. Make sure you have JDK 25 installed.

**Integration tests** (`-Pintegration-tests`): Require Docker for Testcontainers. Make sure the Docker daemon is running and your user has permissions.

```bash
# Verify Docker access
docker ps
```

---

## "Permission denied" on `mvnw`

**Fix**:
```bash
chmod +x backend/mvnw
```

---

<sub>Can't find your issue? Open a [GitHub issue](https://github.com/rogelioolarte/auth-alpaca/issues/new).</sub>
