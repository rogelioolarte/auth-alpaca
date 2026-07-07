# Performance Test Suite

This suite is used to validate the performance, stability, and capacity of the `auth-alpaca` system.

## Prerequisites

- [k6](https://k6.io/docs/getting-started/installation/) installed on your system
- Docker Compose stack running (backend + PostgreSQL)
- Database seeded with test users

## Infrastructure Setup

Before running the tests, ensure your system is configured to handle a large number of concurrent connections to prevent socket exhaustion.

### OS Tuning
Run the following command to increase the open files limit:
```bash
ulimit -n 65535
```

## Execution Sequence

1. **Database Seeding**:
   Populate the database with 2000 users using the provided SQL script:
   ```bash
   # Adjust -h if the PostgreSQL container is on a different host
   psql -h 127.0.0.1 -U postgres -d auth-alpaca -f performance-tests/data/seeding-users.sql
   ```
   > Remember: The database name is `auth-alpaca` (with a hyphen), and the default user is `postgres`.

2. **Environment Configuration**:
   Ensure `performance-tests/.env` contains the correct `BASE_URL` (default: `http://localhost:8080`).

3. **Run Tests**:
   Execute the k6 scripts from the `performance-tests/scripts` directory. For example:
   ```bash
   k6 run performance-tests/scripts/baseline.js
   ```

## Data Distribution
The suite uses `performance-tests/data/users.csv` to distribute seeded users across k6 Virtual Users (VUs) to avoid account contention.
