import { HABITS, habitKey, type HabitLog } from "../types";

export default function DayDetail({ date, logs }: { date: string; logs: HabitLog[] }) {
    const latest = new Map<string, HabitLog>();
    for (const l of [...logs].sort((a, b) => a.id - b.id)) latest.set(habitKey(l), l);

    return (
        <div className="card day-detail">
            <h2>{new Date(date + "T00:00").toDateString()}</h2>
            <ul>
                {HABITS.map((h) => {
                    const log = latest.get(h.key);
                    return (
                        <li key={h.key}>
                            <span className={`dot ${statusColor(log?.status)}`} />
                            <span className="habit">{h.label}</span>
                            <span className="muted">{log ? log.status.toLowerCase() : "not logged"}</span>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
}

function statusColor(s?: string) {
    return s === "DONE" ? "green" : s === "PARTIAL" ? "yellow" : s === "SKIPPED" ? "red" : "grey";
}
