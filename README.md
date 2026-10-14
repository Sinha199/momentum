# Momentum

Habit tracker: Spring Boot + PostgreSQL backend, React (Vite + TypeScript) frontend, in one repo.

```
momentum/
├── docker-compose.yml        # db + backend + frontend in one command
├── backend/                  # your Spring Boot project goes here (pom.xml, src/...)
│   ├── Dockerfile
│   └── src/main/java/com/momentum/config/CorsConfig.java
└── frontend/                 # React UI
    ├── vite.config.ts        # dev proxy: /logs -> http://localhost:8080
    ├── nginx.conf            # prod proxy: /logs -> backend:8080
    ├── Dockerfile
    └── src/
        ├── types.ts          # JSON contract (edit to match your DTOs)
        ├── api.ts            # fetchLogs(month), createLog(body)
        ├── dayStatus.ts      # green / yellow / red / grey rule
        ├── App.tsx
        └── components/ CalendarGrid.tsx, LogForm.tsx, DayDetail.tsx
```

## Run it (dev)

1. Postgres, as you already do:
   `docker run -d --name momentum-db -e POSTGRES_DB=momentum -e POSTGRES_USER=momentum -e POSTGRES_PASSWORD=momentum -p 5432:5432 postgres:16`
2. Backend: `cd backend && ./mvnw spring-boot:run` (listens on :8080)
3. Frontend: `cd frontend && npm install && npm run dev`, then open http://localhost:5173

The UI calls relative URLs (`/logs`). Vite forwards them to :8080, so the browser
sees one origin and CORS never comes into play. `CorsConfig.java` is only needed if
you point the UI (or another tool) at `http://localhost:8080` directly.

No backend running yet? `VITE_USE_MOCK=true npm run dev` keeps logs in localStorage.

## Run it (everything in Docker)

`docker compose up --build`, then http://localhost:3000. nginx serves the built UI and
proxies `/logs` to the `backend` container; the backend reaches Postgres at `db:5432`.

## Assumed JSON contract

Habits are an enum, each tied to a category:
HEALTH = SLEEP, WORKOUT, WATER; CAREER = DSA, MOCKS, APPLICATIONS.
Status is one of `DONE`, `PARTIAL`, `SKIPPED`.

`POST /logs`
```json
{ "date": "2026-10-04", "habit": "WORKOUT", "category": "HEALTH", "status": "DONE", "note": "5k run" }
```
Response: the saved log, with `id` added.

`GET /logs?month=2026-10`
```json
[
  { "id": 1, "date": "2026-10-04", "habit": "WORKOUT", "category": "HEALTH", "status": "DONE", "note": "5k run" }
]
```

Matching Spring side (sketch):
```java
public enum Habit { SLEEP, WORKOUT, WATER, DSA, MOCKS, APPLICATIONS }
public enum Status { DONE, PARTIAL, SKIPPED }
public record HabitLogDto(Long id, LocalDate date, Habit habit, Category category, Status status, String note) {}

@GetMapping("/logs")
List<HabitLogDto> byMonth(@RequestParam YearMonth month) { ... } // "2026-10" binds to YearMonth
```

If your field names differ, change `src/types.ts` (and the `HABITS` list) only.

## Calendar colors

Per day, using the latest log for each habit:
- green: all 6 habits DONE
- red: something logged, but every entry is SKIPPED
- yellow: anything in between (some done/partial, or some habits not logged yet)
- grey: no logs that day
