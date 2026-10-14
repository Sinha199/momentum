import { HABITS, habitKey, type HabitLog } from "./types";

export type DayColor = "green" | "yellow" | "red" | "grey";

/**
 * green  = every habit logged DONE
 * red    = something logged, but nothing DONE or PARTIAL (all skipped)
 * yellow = anything in between (some done/partial, or some habits not logged)
 * grey   = no logs for the day
 */
export function dayColor(logs: HabitLog[]): DayColor {
  if (logs.length === 0) return "grey";
  // If a habit was logged more than once, the latest entry wins.
  const latest = new Map<string, HabitLog>();
  for (const l of [...logs].sort((a, b) => a.id - b.id)) latest.set(habitKey(l), l);
  const statuses = [...latest.values()].map((l) => l.status);

  const allDone = HABITS.every((h) => latest.get(h.key)?.status === "DONE");
  if (allDone) return "green";
  if (statuses.every((s) => s === "SKIPPED")) return "red";
  return "yellow";
}
