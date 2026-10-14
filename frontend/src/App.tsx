import { useCallback, useEffect, useMemo, useState } from "react";
import { createLog, fetchLogs } from "./api";
import type { HabitLog, HabitLogRequest } from "./types";
import CalendarGrid from "./components/CalendarGrid";
import LogForm from "./components/LogForm";
import DayDetail from "./components/DayDetail";

const pad = (n: number) => String(n).padStart(2, "0");
const toMonth = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
const toDate = (d: Date) => `${toMonth(d)}-${pad(d.getDate())}`;

export default function App() {
  const [cursor, setCursor] = useState(() => new Date());
  const [logs, setLogs] = useState<HabitLog[]>([]);
  const [selected, setSelected] = useState(() => toDate(new Date()));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const month = toMonth(cursor);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setLogs(await fetchLogs(month));
    } catch (e) {
      setError((e as Error).message);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, [month]);

  useEffect(() => {
    load();
  }, [load]);

  const byDate = useMemo(() => {
    const m = new Map<string, HabitLog[]>();
    for (const l of logs) m.set(l.date, [...(m.get(l.date) ?? []), l]);
    return m;
  }, [logs]);

  const shiftMonth = (delta: number) =>
    setCursor((c) => new Date(c.getFullYear(), c.getMonth() + delta, 1));

  const handleSubmit = async (req: HabitLogRequest) => {
    await createLog(req);
    // Jump to the month of the logged date so the change is visible, then refetch.
    const [y, m] = req.date.split("-").map(Number);
    if (req.date.slice(0, 7) !== month) setCursor(new Date(y, m - 1, 1));
    else await load();
    setSelected(req.date);
  };

    return (
    <div className="app">
      <header>
        <h1>Momentum</h1>
        <div className="month-nav">
          <button onClick={() => shiftMonth(-1)} aria-label="Previous month">‹</button>
          <span>
            {cursor.toLocaleString(undefined, { month: "long", year: "numeric" })}
          </span>
          <button onClick={() => shiftMonth(1)} aria-label="Next month">›</button>
          <button className="ghost" onClick={() => setCursor(new Date())}>Today</button>
        </div>
      </header>

      {error && <div className="error">Couldn't load logs: {error}</div>}

      <main>
        <section className="calendar-panel">
          <CalendarGrid
            year={cursor.getFullYear()}
            month={cursor.getMonth()}
            byDate={byDate}
            selected={selected}
            onSelect={setSelected}
            loading={loading}
          />
          <Legend />
        </section>
        <aside>
          <LogForm defaultDate={selected} onSubmit={handleSubmit} />
          <DayDetail date={selected} logs={byDate.get(selected) ?? []} />
        </aside>
      </main>
    </div>
  );
}

function Legend() {
  return (
    <ul className="legend">
      <li><span className="dot green" /> All done</li>
      <li><span className="dot yellow" /> Partial</li>
      <li><span className="dot red" /> Skipped</li>
      <li><span className="dot grey" /> No data</li>
    </ul>
  );
}
