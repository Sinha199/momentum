# Momentum: Redis, read replica, weekly insight and CI/CD

Everything here was built on your latest upload (`tracker/` + `frontend/`) and run end to end:
`./mvnw verify` passes all 10 tests, and the full `docker compose` stack passes all 12 checks in
`scripts/smoke-test.sh`.

**Files**
- `momentum-next/`: only the files that are new or changed. Copy it over your `momentum/` folder;
  the paths match.
- `next-phase.patch`: the same changes as one diff, for reading or `git apply`.

**Before you start:** apply `momentum-review/frontend-fix.patch` from the earlier thread
(`cd frontend && git apply ../../momentum-review/frontend-fix.patch`, or copy the files by hand).
Your uploaded frontend doesn't compile yet (`DayDetail.tsx:18` still reads `log.note`), so
`npm run build`, the frontend Docker image and the CI pipeline would all fail on it.

---

## 1. Your draft docker-compose.yml

| Line in your draft | Problem | Fixed version |
|---|---|---|
| `pg_isready -U momentum -d momentum` | The env creates user/db `postgres`, so the healthcheck never passes and the backend never starts | Checks the same user/db the env creates |
| `SPRING_DATASOURCE_URL: ...127.0.0.1:5434...` | Inside a container, 127.0.0.1 is the container itself, not Windows or WSL. Connection refused | `postgres-primary:5432`, the compose service name |
| `build: ./tracker` with no Dockerfile in `tracker/` | Build fails | `tracker/Dockerfile` added (Java 17, matches your pom) |
| `ports: "5432:5432"` | Fine, but it's a third Postgres next to your WSL one | Primary on host port 5435, replica on 5436, so neither clashes with WSL's 5434 |

## 2. WSL Postgres vs compose: which database when

Keep two setups. They don't share data and don't interfere.

**Day-to-day in IntelliJ (unchanged):** `application.properties` still defaults to your WSL
Postgres on `127.0.0.1:5434` (Windows reaches WSL through localhost forwarding). With no replica
URL set, reads go to a second pool on the same database, so the routing code runs without
needing a replica. For Redis, start one plain node:
- Docker inside WSL or Docker Desktop: `docker run -d --name momentum-redis -p 6379:6379 redis:7.4`
- No Docker: `sudo apt install redis-server && sudo service redis-server start` inside WSL. Windows reaches it on `localhost:6379` the same way it reaches Postgres.

**Replica and cluster work (compose):** `docker compose up -d --build` starts its own primary and
streaming replica from empty volumes, plus a 3-node Redis Cluster. Run it from your WSL shell if
Docker Engine lives in WSL, or from PowerShell with Docker Desktop. Ports show up on Windows
`localhost` either way.

Why the replica pair lives in compose, not on your WSL instance: a replica needs a replication
role, a `pg_hba.conf` entry and a slot on the primary, plus a second Postgres cloned with
`pg_basebackup`. Scripting that in compose means CI gets the identical setup, and you can wipe it
with `docker compose down -v` without touching your dev data.

To point IntelliJ at the compose databases (for example to debug routing), add these to the run
configuration's environment variables:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/momentum?sslmode=disable
APP_DATASOURCE_REPLICA_URL=jdbc:postgresql://localhost:5436/momentum?sslmode=disable
```
Keep the single Redis for IntelliJ. Cluster nodes give clients their internal `172.28.0.x`
addresses, which Windows can't reach.

---

## 3. Redis layer (Day 4–5)

**What changed**
- `docker-compose.yml`: `redis-1..3` with `cluster-enabled`, plus a one-shot `redis-cluster-init`
  that runs `redis-cli --cluster create` (3 masters, 16384 slots split 3 ways). Nodes get fixed
  IPs, so the cluster map stays valid across restarts.
- `config/CacheConfig.java`: `RedisCacheManager` storing JSON. `logsByMonth` TTL is 24h and
  `weeklyInsight` TTL is 6h. If a cache call fails, it's logged and the request falls back to Postgres.
- `service/HabitLogService.java`
  - `getLogsForMonth`: `@Cacheable("logsByMonth", key = month)`. A hit never opens a DB transaction.
  - `saveAndUpdateLog`: after the upsert it **rebuilds the month from the primary and writes it
    into the cache**, instead of only evicting. With a replica, evicting is a bug: the UI's next
    GET would read the replica, which can be a few ms behind, and cache the stale month for 24h.
    This is a good interview point.
  - Redis writes run after the Postgres commit (`config/AfterCommit.java`), so Redis never holds
    rolled-back data, and a Redis outage can't turn a saved write into a 500.
- `stats/StreakService.java`, `StreakCalculator.java`, `StatsController.java`:
  `ZADD streaks:career <streak> <userId>` (and `streaks:health`), recomputed from Postgres on each
  write. A day counts when every habit of that type is DONE, the same rule as a green calendar
  day. If today isn't complete yet, the streak ending yesterday still counts.
  - `GET /stats/streak` returns `{"HEALTH":0,"CAREER":1}` (ZSCORE, re-seeded from the DB if the key is missing).
  - `GET /stats/leaderboard?type=CAREER` returns the ZREVRANGE WITHSCORES top 10.
- Config: `spring.data.redis.host/port` for one node in dev. Compose sets
  `SPRING_DATA_REDIS_CLUSTER_NODES`, and Spring switches to cluster mode with no code change.

**How to test**
```bash
curl "localhost:8080/logs?month=2026-10"
docker compose exec redis-1 redis-cli -c GET logsByMonth::2026-10    # JSON of the month
docker compose exec redis-1 redis-cli -c TTL logsByMonth::2026-10    # ~86400
docker compose exec redis-1 redis-cli CLUSTER KEYSLOT logsByMonth::2026-10
docker compose exec redis-1 redis-cli cluster nodes                 # 3 masters, slot ranges
docker compose exec redis-1 redis-cli -c ZREVRANGE streaks:career 0 -1 WITHSCORES
```
- **Cache hit proof:** `MomentumIntegrationTest.monthIsServedFromRedisAndRefreshedOnWrite`
  deletes the rows behind the app's back. The next GET still returns them (from Redis), and the
  next POST refreshes the month.
- **Outage:** `docker compose stop redis-1 redis-2 redis-3`, then GET and POST still return 200
  (about 2s slower: `spring.data.redis.timeout=2s`). I ran this.

## 4. Primary–replica + HikariCP (Day 6–7)

**What changed**
- `docker/postgres/primary-init.sh`: creates the `replicator` role, a `pg_hba` replication entry,
  and slot `replica_1`, so the primary keeps WAL until the replica has it.
- `docker/postgres/replica-entrypoint.sh`: on first start, runs
  `pg_basebackup -X stream -S replica_1 -R`. `-R` writes `standby.signal`, so Postgres starts as
  a read-only hot standby.
- `config/DataSourceConfig.java` + `ReadReplicaRoutingDataSource.java`: two Hikari pools behind
  an `AbstractRoutingDataSource`. `@Transactional(readOnly = true)` routes to the replica;
  everything else goes to the primary (writes, `ddl-auto`, `data.sql`).
  - **The non-obvious part:** it's wrapped in `LazyConnectionDataSourceProxy`. Spring only flags
    a transaction read-only after it has begun, and Hibernate takes a connection while beginning.
    Without the proxy every read goes to the primary, and nothing errors to tell you so.
- `spring.jpa.open-in-view=false`: connections are held only inside service transactions, which
  keeps routing predictable. DTO mapping moved into the service for that reason.
- HikariCP, documented inline in `application.properties`:
  - `maximum-pool-size=10`, `minimum-idle=2`.
  - `connection-timeout=3000`: fail in 3s instead of the 30s default.
  - `idle-timeout=5min`, `max-lifetime=30min`.
  - The replica pool is also `read-only=true`, so the driver refuses writes even if routing were wrong.

**How to test**
```bash
curl localhost:8080/debug/datasource
# {"readOnlyTransaction":"replica (172.28.0.3)","writeTransaction":"primary (172.28.0.2)"}
docker compose exec postgres-primary psql -U postgres -d momentum -c "select client_addr, state from pg_stat_replication"   # streaming
docker compose exec postgres-replica psql -U postgres -d momentum -c "select pg_is_in_recovery(), now() - pg_last_xact_replay_timestamp() as lag"
docker compose exec postgres-replica psql -U postgres -d momentum -c "insert into category(name,type) values('x','HEALTH')"
# ERROR: cannot execute INSERT in a read-only transaction
```
To see which pool each request uses, set `logging.level.com.zaxxer.hikari=DEBUG` and watch
`primary-pool` and `replica-pool`.

**Known limit (README "future improvements"):** if the replica is down, reads fail after 3s.
A fallback to the primary on replica failure is the natural next step.

## 5. LLM weekly insight (Day 8)

**What changed:** `insight/` package.
- `GET /insights/weekly` gathers the last 7 days of logs from the replica, counts DONE / PARTIAL /
  SKIPPED per habit, adds the streaks, and sends that JSON with a short coaching system prompt.
- `AnthropicLlmClient` uses the official `anthropic-java` SDK. Model `claude-opus-5-5` at low
  effort (a 2–3 sentence nudge doesn't need deep reasoning). Change it with `app.llm.model`.
- With no `ANTHROPIC_API_KEY`, or if the call fails or the model declines, it returns a rule-based
  sentence with `"source":"rule-based"`. Dev and CI need no key.
- Only real LLM answers are cached (`weeklyInsight::<date>`, 6h). Any new log evicts today's entry.

**How to test**
```bash
curl localhost:8080/insights/weekly            # "source":"rule-based" without a key
ANTHROPIC_API_KEY=sk-ant-... docker compose up -d backend
curl localhost:8080/insights/weekly            # "source":"llm"
docker compose exec redis-1 redis-cli -c GET weeklyInsight::$(date +%F)   # second call is served from here
```
I checked that the SDK runs inside Spring Boot 3.2 and reaches the API (a fake key returns a
clean 401 and falls back). I couldn't check a real answer without a key, so the `llm` path is
untested. Automated tests mock `LlmClient` and check that the prompt contains the habit counts.

## 6. Dockerize + CI/CD (Day 9–10)

- `tracker/Dockerfile`: multi-stage. The Maven + JDK 17 stage compiles, and `pom.xml` is copied
  first so dependency downloads are cached. The final stage is `eclipse-temurin:17-jre` + one jar,
  running as a non-root user.
- `.github/workflows/ci.yml`:
  1. `backend-test`: `./mvnw verify`. Unit tests, plus `MomentumIntegrationTest` on real
     Postgres 16 + Redis 7.4 via Testcontainers (GitHub runners have Docker).
  2. `frontend-build`: `npm ci && npm run build`. Type errors fail the build, so a UI/backend
     JSON mismatch is caught here.
  3. `smoke-test`: `docker compose up --build --wait`, then `scripts/smoke-test.sh` against the
     real primary/replica, Redis cluster and nginx.
  4. `publish` (push to main only): pushes `ghcr.io/<you>/momentum-backend` and
     `momentum-frontend`, tagged `latest` and the commit SHA.
  5. `deploy` (optional): POSTs to the `RENDER_DEPLOY_HOOK` repo secret if you set one, otherwise skipped.

**Repo setup this needs**
- Make `momentum/` the git repo root. `tracker/.git` is an empty nested repo (no commits): delete
  it, then `git init` in `momentum/`. Otherwise GitHub treats `tracker` as a broken submodule.
- Commit `frontend/package-lock.json` (`npm ci` requires it) and `tracker/.mvn/` + `mvnw`.
  The workflow runs `chmod +x mvnw`, because Windows drops the executable bit.
- `smoke-test.sh` is bash. Locally, run it from WSL or Git Bash.

**pom.xml fixes in the same change**
- Removed the duplicate `spring-boot-starter-test` that had no `<scope>test</scope>`. It was
  bundling JUnit and Mockito into your app jar.
- Pinned Testcontainers to 1.21.4. Boot 3.2's default (1.19) is rejected by current Docker
  Engine ("client version 1.32 is too old"); I hit this here.

## 7. Other small changes
- `data.sql`: the user insert now runs only once (it added a duplicate `anshu` row on every restart).
- `frontend/nginx.conf` and `vite.config.ts` also proxy `/stats` and `/insights`, so the UI can
  call the new endpoints.
- `spring.jpa.show-sql=false`: with routing logs it was too noisy. Turn it back on when you need it.

## 8. Full check, in order
```bash
cd momentum
cd tracker && ./mvnw verify && cd ..        # 10 tests; needs Docker running
docker compose up -d --build --wait
./scripts/smoke-test.sh                     # 12 PASS lines, "All checks passed."
docker compose down -v                      # wipe the compose databases
```
