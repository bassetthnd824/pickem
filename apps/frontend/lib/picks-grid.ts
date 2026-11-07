export type GridTeam = {
  id: string
  name: string
  squad: string
}

export type GridVenue = {
  name: string
  cityState: string
}

export type GridMatchup = {
  matchupId: string
  matchupDate: string
  awayTeam: GridTeam
  homeTeam: GridTeam
  awayTeamScore: number | null
  homeTeamScore: number | null
  venue: GridVenue | null
  rivalryName: string | null
  pickedTeamId: string | null
  rank: number | null
  winningTeamId: string | null
}

export type GridWeek = {
  id: string
  weekNumber: number
  currentOrPast: boolean
  matchups: GridMatchup[]
}

export type GridMain = {
  season: string
  numberOfConferenceTeams: number
  weeks: GridWeek[]
}

export type WeekDraft = {
  order: string[]
  pickedTeamId: Record<string, string>
  rank: Record<string, number>
}

export type PickResult = 'correct' | 'incorrect'

export function parseGameMain(value: unknown): GridMain {
  if (!value || typeof value !== 'object') {
    throw new Error('Game main response was invalid')
  }
  const record = value as Record<string, unknown>
  if (typeof record.season !== 'string') {
    throw new Error('Game main response was invalid')
  }
  if (
    typeof record.numberOfConferenceTeams !== 'number' ||
    !Number.isInteger(record.numberOfConferenceTeams)
  ) {
    throw new Error('Game main response was invalid')
  }
  if (!Array.isArray(record.weeks)) {
    throw new Error('Game main response was invalid')
  }
  return {
    season: record.season,
    numberOfConferenceTeams: record.numberOfConferenceTeams,
    weeks: record.weeks.map(parseWeek),
  }
}

export function draftsFromMain(main: GridMain): Record<string, WeekDraft> {
  const drafts: Record<string, WeekDraft> = {}
  for (const week of main.weeks) {
    drafts[week.id] = draftFromWeek(week, main.numberOfConferenceTeams)
  }
  return drafts
}

/** A stored rank is kept. A missing rank takes the next unused value from N downward. */
export function draftFromWeek(
  week: GridWeek,
  conferenceTeams: number,
): WeekDraft {
  const used = new Set<number>()
  for (const matchup of week.matchups) {
    if (matchup.rank != null) {
      used.add(matchup.rank)
    }
  }
  let cursor = conferenceTeams
  const order: string[] = []
  const pickedTeamId: Record<string, string> = {}
  const rank: Record<string, number> = {}
  for (const matchup of week.matchups) {
    order.push(matchup.matchupId)
    pickedTeamId[matchup.matchupId] = matchup.pickedTeamId ?? ''
    if (matchup.rank != null) {
      rank[matchup.matchupId] = matchup.rank
      continue
    }
    while (used.has(cursor)) {
      cursor -= 1
    }
    rank[matchup.matchupId] = cursor
    used.add(cursor)
    cursor -= 1
  }
  return { order, pickedTeamId, rank }
}

export function moveMatchup(
  draft: WeekDraft,
  matchupId: string,
  direction: -1 | 1,
  conferenceTeams: number,
): WeekDraft {
  const index = draft.order.indexOf(matchupId)
  const next = index + direction
  if (index < 0 || next < 0 || next >= draft.order.length) {
    return draft
  }
  const order = draft.order.slice()
  const neighbor = order[next]
  if (neighbor === undefined) {
    return draft
  }
  order[index] = neighbor
  order[next] = matchupId
  return {
    order,
    pickedTeamId: draft.pickedTeamId,
    rank: ranksFromTop(order, conferenceTeams),
  }
}

export function ranksFromTop(
  order: readonly string[],
  conferenceTeams: number,
): Record<string, number> {
  const rank: Record<string, number> = {}
  let value = conferenceTeams
  for (const id of order) {
    rank[id] = value
    value -= 1
  }
  return rank
}

export function saveBody(
  seasonWeekId: string,
  draft: WeekDraft,
): {
  seasonWeekId: string
  picks: {
    matchupId: string
    pickedTeamId: string | null
    rank: number | null
  }[]
} {
  return {
    seasonWeekId,
    picks: draft.order.map((matchupId) => {
      const pickedTeamId = draft.pickedTeamId[matchupId]?.trim() ?? ''
      const rank = draft.rank[matchupId]
      const picked = pickedTeamId !== ''
      return {
        matchupId,
        pickedTeamId: picked ? pickedTeamId : null,
        rank: picked && typeof rank === 'number' ? rank : null,
      }
    }),
  }
}

export function pickResult(matchup: GridMatchup): PickResult | null {
  if (!matchup.winningTeamId || !matchup.pickedTeamId) {
    return null
  }
  if (
    matchup.homeTeamScore != null &&
    matchup.awayTeamScore != null &&
    matchup.homeTeamScore === matchup.awayTeamScore
  ) {
    return null
  }
  return matchup.pickedTeamId === matchup.winningTeamId
    ? 'correct'
    : 'incorrect'
}

export function venueLabel(venue: GridVenue | null): string {
  if (!venue) {
    return ''
  }
  return [venue.name, venue.cityState].filter((part) => part !== '').join(' ')
}

function parseWeek(value: unknown): GridWeek {
  if (!value || typeof value !== 'object') {
    throw new Error('Game main response was invalid')
  }
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'string' || typeof record.weekNumber !== 'number') {
    throw new Error('Game main response was invalid')
  }
  if (!Array.isArray(record.matchups)) {
    throw new Error('Game main response was invalid')
  }
  return {
    id: record.id,
    weekNumber: record.weekNumber,
    currentOrPast: record.currentOrPast === true,
    matchups: record.matchups.map(parseMatchup),
  }
}

function parseMatchup(value: unknown): GridMatchup {
  if (!value || typeof value !== 'object') {
    throw new Error('Game main response was invalid')
  }
  const record = value as Record<string, unknown>
  if (typeof record.matchupId !== 'string') {
    throw new Error('Game main response was invalid')
  }
  return {
    matchupId: record.matchupId,
    matchupDate:
      typeof record.matchupDate === 'string' ? record.matchupDate : '',
    awayTeam: parseTeam(record.awayTeam),
    homeTeam: parseTeam(record.homeTeam),
    awayTeamScore: parseScore(record.awayTeamScore),
    homeTeamScore: parseScore(record.homeTeamScore),
    venue: parseVenue(record.venue),
    rivalryName:
      typeof record.rivalryName === 'string' ? record.rivalryName : null,
    pickedTeamId:
      typeof record.pickedTeamId === 'string' ? record.pickedTeamId : null,
    rank: typeof record.rank === 'number' ? record.rank : null,
    winningTeamId:
      typeof record.winningTeamId === 'string' ? record.winningTeamId : null,
  }
}

function parseTeam(value: unknown): GridTeam {
  if (!value || typeof value !== 'object') {
    throw new Error('Game main response was invalid')
  }
  const record = value as Record<string, unknown>
  if (typeof record.id !== 'string' || typeof record.name !== 'string') {
    throw new Error('Game main response was invalid')
  }
  return {
    id: record.id,
    name: record.name,
    squad: typeof record.squad === 'string' ? record.squad : '',
  }
}

function parseVenue(value: unknown): GridVenue | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const record = value as Record<string, unknown>
  if (typeof record.name !== 'string') {
    return null
  }
  return {
    name: record.name,
    cityState: typeof record.cityState === 'string' ? record.cityState : '',
  }
}

function parseScore(value: unknown): number | null {
  return typeof value === 'number' ? value : null
}
