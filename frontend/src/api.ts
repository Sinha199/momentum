import type { HabitLog, HabitLogRequest } from "./types";

// Relative URLs: the Vite proxy (dev) or nginx (docker) forwards /logs to Spring Boot.
const BASE = "";
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

export async function fetchLogs(month: string): Promise<HabitLog[]> {
  if (USE_MOCK) return mock.list(month);
  const res = await fetch(`${BASE}/logs?month=${month}`);
  if (!res.ok) throw new Error(`GET /logs failed: ${res.status}`);
  return res.json();
}

export async function createLog(body: HabitLogRequest): Promise<HabitLog> {
  if (USE_MOCK) return mock.create(body);
  const res = await fetch(`${BASE}/logs`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST /logs failed: ${res.status} ${await res.text()}`);
  return res.json();
}

// In-browser stand-in for the backend, for trying the UI without Spring Boot running.
const mock = {
  all(): HabitLog[] {
    try {
      return JSON.parse(localStorage.getItem("momentum-logs") ?? "[]");
    } catch {
      return [];
    }
  },
  async list(month: string) {
    return this.all().filter((l) => l.date.startsWith(month));
  },
  async create(body: HabitLogRequest) {
    const logs = this.all();
    const log = { ...body, id: Date.now(), categoryId: 0 };
    localStorage.setItem("momentum-logs", JSON.stringify([...logs, log]));
    return log;
  },
};