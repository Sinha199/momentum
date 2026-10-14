import { useEffect, useState, type FormEvent } from "react";
import { HABITS, type Habit, type HabitLogRequest, type Status } from "../types";

interface Props {
  defaultDate: string;
  onSubmit: (req: HabitLogRequest) => Promise<void>;
}

export default function LogForm({ defaultDate, onSubmit }: Props) {
  const [date, setDate] = useState(defaultDate);
  const [habit, setHabit] = useState<Habit>("SLEEP");
  const [status, setStatus] = useState<Status>("DONE");
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null);

  // Clicking a day on the calendar pre-fills the form date.
  useEffect(() => setDate(defaultDate), [defaultDate]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setMsg(null);
    const category = HABITS.find((h) => h.key === habit)!.category;
    try {
      await onSubmit({ date, categoryName: habit, status });
      setMsg({ ok: true, text: "Saved" });
      setNote("");
    } catch (err) {
      setMsg({ ok: false, text: (err as Error).message });
    } finally {
      setBusy(false);
    }
  };

  return (
      <form className="card log-form" onSubmit={submit}>
        <h2>Log a habit</h2>
        <label>
          Date
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </label>
        <label>
          Habit
          <select value={habit} onChange={(e) => setHabit(e.target.value as Habit)}>
            <optgroup label="Health">
              {HABITS.filter((h) => h.category === "HEALTH").map((h) => (
                  <option key={h.key} value={h.key}>{h.label}</option>
              ))}
            </optgroup>
            <optgroup label="Career">
              {HABITS.filter((h) => h.category === "CAREER").map((h) => (
                  <option key={h.key} value={h.key}>{h.label}</option>
              ))}
            </optgroup>
          </select>
        </label>
        <fieldset className="status-group">
          <legend>Status</legend>
          {(["DONE", "PARTIAL", "SKIPPED"] as Status[]).map((s) => (
              <label key={s} className={`pill ${s.toLowerCase()} ${status === s ? "active" : ""}`}>
                <input
                    type="radio"
                    name="status"
                    value={s}
                    checked={status === s}
                    onChange={() => setStatus(s)}
                />
                {s[0] + s.slice(1).toLowerCase()}
              </label>
          ))}
        </fieldset>
        {/*<label>*/}
        {/*  Note*/}
        {/*  <textarea*/}
        {/*    value={note}*/}
        {/*    onChange={(e) => setNote(e.target.value)}*/}
        {/*    placeholder="Optional, e.g. 7h sleep, 2 LeetCode mediums"*/}
        {/*    rows={2}*/}
        {/*  />*/}
        {/*</label>*/}
        <button type="submit" disabled={busy}>{busy ? "Saving…" : "Save log"}</button>
        {msg && <p className={msg.ok ? "ok" : "error"}>{msg.text}</p>}
      </form>
  );
}
