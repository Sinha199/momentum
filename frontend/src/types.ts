// Assumed JSON contract with the Spring Boot backend. Adjust to match your DTOs.

export type Category = "HEALTH" | "CAREER";
export type Status = "DONE" | "PARTIAL" | "SKIPPED";
export type Habit = "SLEEP" | "WORKOUT" | "SKINCARE" | "DSA" | "MOCKS" | "SYSTEM DESIGN";

export const HABITS: { key: Habit; label: string; category: Category }[] = [
  { key: "SLEEP", label: "Sleep", category: "HEALTH" },
  { key: "WORKOUT", label: "Workout", category: "HEALTH" },
  { key: "SKINCARE", label: "SKINCARE", category: "HEALTH" },
  { key: "DSA", label: "DSA", category: "CAREER" },
  { key: "MOCKS", label: "Mocks", category: "CAREER" },
  { key: "SYSTEM DESIGN", label: "SYSTEM DESIGN", category: "CAREER" },
];

/** Body of POST /logs */
export interface HabitLogRequest {
  date: string; // "YYYY-MM-DD"
  categoryName: string;
  status: Status;
}

/** One element of GET /logs?month=YYYY-MM (and the POST response) */
export interface HabitLog {
  id: number;
  categoryId: number;
  categoryName: string;
  date: string;
  status: Status;
}
/** Key a log by habit, so "System Design" from the backend matches HABITS key "SYSTEM DESIGN". */
export const habitKey = (log: HabitLog) => log.categoryName.toUpperCase();
