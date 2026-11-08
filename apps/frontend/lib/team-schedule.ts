/** Conference teams and one team's schedule, in API order. */

export type ConferenceTeam = {
  id: string
  teamName: string
  squadName: string
}

export type ScheduleGame = {
  matchupDate: string
  opponentName: string
  scoreResult: string
  winLoss: string
}

const INVALID_TEAMS = 'Team list response was invalid'
const INVALID_SCHEDULE = 'Team schedule response was invalid'

export function teamOptionLabel(team: ConferenceTeam): string {
  return `${team.teamName} ${team.squadName}`
}

export function parseConferenceTeams(value: unknown): ConferenceTeam[] {
  if (!Array.isArray(value)) {
    throw new Error(INVALID_TEAMS)
  }
  return value.map(parseConferenceTeam)
}

export function parseTeamSchedule(value: unknown): ScheduleGame[] {
  if (!Array.isArray(value)) {
    throw new Error(INVALID_SCHEDULE)
  }
  return value.map(parseScheduleGame)
}

function parseConferenceTeam(value: unknown): ConferenceTeam {
  if (!value || typeof value !== 'object') {
    throw new Error(INVALID_TEAMS)
  }
  const record = value as Record<string, unknown>
  if (
    typeof record.id !== 'string' ||
    record.id.trim() === '' ||
    typeof record.teamName !== 'string' ||
    typeof record.squadName !== 'string'
  ) {
    throw new Error(INVALID_TEAMS)
  }
  return {
    id: record.id,
    teamName: record.teamName,
    squadName: record.squadName,
  }
}

function parseScheduleGame(value: unknown): ScheduleGame {
  if (!value || typeof value !== 'object') {
    throw new Error(INVALID_SCHEDULE)
  }
  const record = value as Record<string, unknown>
  if (
    typeof record.matchupDate !== 'string' ||
    record.matchupDate.trim() === ''
  ) {
    throw new Error(INVALID_SCHEDULE)
  }
  if (typeof record.opponentName !== 'string') {
    throw new Error(INVALID_SCHEDULE)
  }
  return {
    matchupDate: record.matchupDate,
    opponentName: record.opponentName,
    scoreResult: optionalText(record.scoreResult),
    winLoss: optionalResult(record.winLoss),
  }
}

function optionalText(value: unknown): string {
  if (value == null) {
    return ''
  }
  if (typeof value !== 'string') {
    throw new Error(INVALID_SCHEDULE)
  }
  return value
}

function optionalResult(value: unknown): string {
  if (value == null) {
    return ''
  }
  if (value !== 'W' && value !== 'L') {
    throw new Error(INVALID_SCHEDULE)
  }
  return value
}
