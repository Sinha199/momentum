import type { HabitLog } from "../types";
import { dayColor } from "../dayStatus";

const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const pad = (n: number) => String(n).padStart(2, "0");

interface Props {
  year: number;
  month: number; // 0-based
  byDate: Map<string, HabitLog[]>;
  selected: string;
  onSelect: (date: string) => void;
  loading: boolean;
}

export default function CalendarGrid({ year, month, byDate, selected, onSelect, loading }: Props) {
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  // Monday-first: getDay() is 0 for Sunday.
  const leading = (new Date(year, month, 1).getDay() + 6) % 7;
  const today = new Date();
  const todayKey = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;

  const cells: (number | null)[] = [
    ...Array(leading).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  return (
      <div className={`calendar ${loading ? "loading" : ""}`}>
        {WEEKDAYS.map((d) => (
            <div key={d} className="weekday">{d}</div>
        ))}
        {cells.map((day, i) => {
          if (day === null) return <div key={`blank-${i}`} className="cell blank" />;
          const key = `${year}-${pad(month + 1)}-${pad(day)}`;
          const logs = byDate.get(key) ?? [];
          const color = dayColor(logs);
          return (
              <button
                  key={key}
                  className={`cell ${color} ${key === selected ? "selected" : ""} ${key === todayKey ? "today" : ""}`}
                  onClick={() => onSelect(key)}
                  title={`${key}: ${logs.length} log(s)`}
              >
                <span className="day-num">{day}</span>
                {logs.length > 0 && <span className="count">{logs.length}</span>}
              </button>
          );
        })}
      </div>
  );
}
