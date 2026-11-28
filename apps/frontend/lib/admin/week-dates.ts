const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

export function parseWeekNumber(value: string): number | null {
  const trimmed = value.trim()
  if (!/^[1-9]\d*$/.test(trimmed)) {
    return null
  }
  const parsed = Number(trimmed)
  if (!Number.isSafeInteger(parsed)) {
    return null
  }
  return parsed
}

export function defaultWeekDates(
  seasonBegin: string,
  weekNumber: number,
): { beginDate: string; endDate: string } | null {
  if (!Number.isInteger(weekNumber) || weekNumber < 1) {
    return null
  }
  const beginDate = addDays(seasonBegin, (weekNumber - 1) * 7)
  if (!beginDate) {
    return null
  }
  const endDate = addDays(beginDate, 6)
  if (!endDate) {
    return null
  }
  return { beginDate, endDate }
}

function addDays(iso: string, days: number): string | null {
  const match = ISO_DATE.exec(iso)
  if (!match) {
    return null
  }
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month - 1, day))
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null
  }
  date.setUTCDate(date.getUTCDate() + days)
  const nextYear = String(date.getUTCFullYear()).padStart(4, '0')
  const nextMonth = String(date.getUTCMonth() + 1).padStart(2, '0')
  const nextDay = String(date.getUTCDate()).padStart(2, '0')
  return `${nextYear}-${nextMonth}-${nextDay}`
}
