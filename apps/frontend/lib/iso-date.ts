import * as v from "valibot";

function isCalendarDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) {
    return false;
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

/** ISO-8601 calendar date (`YYYY-MM-DD`) from `<input type="date">`. */
export const isoDateSchema = v.pipe(
  v.string(),
  v.check(isCalendarDate, "Enter a date as YYYY-MM-DD"),
);

export type IsoDate = v.InferOutput<typeof isoDateSchema>;
