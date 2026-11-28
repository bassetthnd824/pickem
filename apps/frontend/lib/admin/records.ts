import { buildQuery, readQuery } from '@/lib/admin/query-string'

const INVALID_SEASONS = 'Season response was invalid'
const INVALID_WEEKS = 'Season week response was invalid'
const INVALID_VENUES = 'Venue response was invalid'
const INVALID_TEAMS = 'Team response was invalid'
const INVALID_RIVALRIES = 'Rivalry response was invalid'

export type AuditFields = {
  id: string
  lastUpdateDate: string | null
  lastUpdateUser: string | null
  version: number | null
}

export type SeasonRecord = AuditFields & {
  season: string
  beginDate: string
  endDate: string
  current: boolean
}

export type SeasonWeekRecord = AuditFields & {
  seasonId: string
  weekNumber: number
  beginDate: string
  endDate: string
}

export type VenueRecord = AuditFields & {
  venueName: string
  cityState: string
  cfbdVenueId: number | null
}

export type VenueSnapshot = {
  id: string
  name: string
  cityState: string
}

export type TeamRecord = AuditFields & {
  teamName: string
  squadName: string
  conferenceMember: boolean
  homeVenueId: string
  homeVenue: VenueSnapshot | null
  cfbdTeamId: number | null
}

export type TeamSnapshot = {
  id: string
  name: string
}

export type RivalryRecord = AuditFields & {
  rivalryName: string
  team1Id: string
  team2Id: string
  team1: TeamSnapshot | null
  team2: TeamSnapshot | null
}

export type SeasonCriteria = {
  season: string
  beginDate: string
  endDate: string
}

export type SeasonWeekCriteria = {
  seasonId: string
  weekNumber: string
  beginDate: string
  endDate: string
}

export type VenueCriteria = {
  venueName: string
  cityState: string
}

export type TeamCriteria = {
  teamName: string
  squadName: string
  homeVenueId: string
}

export type RivalryCriteria = {
  rivalryName: string
  team1Id: string
}

export const EMPTY_SEASON_CRITERIA: SeasonCriteria = {
  season: '',
  beginDate: '',
  endDate: '',
}

export const EMPTY_SEASON_WEEK_CRITERIA: SeasonWeekCriteria = {
  seasonId: '',
  weekNumber: '',
  beginDate: '',
  endDate: '',
}

export const EMPTY_VENUE_CRITERIA: VenueCriteria = {
  venueName: '',
  cityState: '',
}

export const EMPTY_TEAM_CRITERIA: TeamCriteria = {
  teamName: '',
  squadName: '',
  homeVenueId: '',
}

export const EMPTY_RIVALRY_CRITERIA: RivalryCriteria = {
  rivalryName: '',
  team1Id: '',
}

const SEASON_KEYS = ['season', 'beginDate', 'endDate'] as const
const WEEK_KEYS = ['seasonId', 'weekNumber', 'beginDate', 'endDate'] as const
const VENUE_KEYS = ['venueName', 'cityState'] as const
const TEAM_KEYS = ['teamName', 'squadName', 'homeVenueId'] as const
const RIVALRY_KEYS = ['rivalryName', 'team1Id'] as const

export function seasonSearchQuery(criteria: SeasonCriteria): string {
  return buildQuery(criteria)
}

export function seasonCriteriaFromQuery(query: string): SeasonCriteria {
  return readQuery(query, SEASON_KEYS)
}

export function seasonWeekSearchQuery(criteria: SeasonWeekCriteria): string {
  return buildQuery(criteria)
}

export function seasonWeekCriteriaFromQuery(query: string): SeasonWeekCriteria {
  return readQuery(query, WEEK_KEYS)
}

export function venueSearchQuery(criteria: VenueCriteria): string {
  return buildQuery(criteria)
}

export function venueCriteriaFromQuery(query: string): VenueCriteria {
  return readQuery(query, VENUE_KEYS)
}

export function teamSearchQuery(criteria: TeamCriteria): string {
  return buildQuery(criteria)
}

export function teamCriteriaFromQuery(query: string): TeamCriteria {
  return readQuery(query, TEAM_KEYS)
}

export function rivalrySearchQuery(criteria: RivalryCriteria): string {
  return buildQuery(criteria)
}

export function rivalryCriteriaFromQuery(query: string): RivalryCriteria {
  return readQuery(query, RIVALRY_KEYS)
}

export function teamOptionLabel(team: {
  teamName: string
  squadName: string
}): string {
  return `${team.teamName} ${team.squadName}`.trim()
}

export function parseSeasons(value: unknown): SeasonRecord[] {
  return readList(value, INVALID_SEASONS, parseSeason)
}

export function parseSeason(value: unknown): SeasonRecord {
  const record = readObject(value, INVALID_SEASONS)
  return {
    ...readAudit(record, INVALID_SEASONS),
    season: readString(record.season, INVALID_SEASONS),
    beginDate: readString(record.beginDate, INVALID_SEASONS),
    endDate: readString(record.endDate, INVALID_SEASONS),
    current: readBoolean(record.isCurrent, INVALID_SEASONS),
  }
}

export function parseSeasonWeeks(value: unknown): SeasonWeekRecord[] {
  return readList(value, INVALID_WEEKS, parseSeasonWeek)
}

export function parseSeasonWeek(value: unknown): SeasonWeekRecord {
  const record = readObject(value, INVALID_WEEKS)
  return {
    ...readAudit(record, INVALID_WEEKS),
    seasonId: readString(record.seasonId, INVALID_WEEKS),
    weekNumber: readInteger(record.weekNumber, INVALID_WEEKS),
    beginDate: readString(record.beginDate, INVALID_WEEKS),
    endDate: readString(record.endDate, INVALID_WEEKS),
  }
}

export function parseVenues(value: unknown): VenueRecord[] {
  return readList(value, INVALID_VENUES, parseVenue)
}

export function parseVenue(value: unknown): VenueRecord {
  const record = readObject(value, INVALID_VENUES)
  return {
    ...readAudit(record, INVALID_VENUES),
    venueName: readString(record.venueName, INVALID_VENUES),
    cityState: readString(record.cityState, INVALID_VENUES),
    cfbdVenueId: readOptionalNumber(record.cfbdVenueId, INVALID_VENUES),
  }
}

export function parseTeams(value: unknown): TeamRecord[] {
  return readList(value, INVALID_TEAMS, parseTeam)
}

export function parseTeam(value: unknown): TeamRecord {
  const record = readObject(value, INVALID_TEAMS)
  return {
    ...readAudit(record, INVALID_TEAMS),
    teamName: readString(record.teamName, INVALID_TEAMS),
    squadName: readString(record.squadName, INVALID_TEAMS),
    conferenceMember: readBoolean(record.conferenceMember, INVALID_TEAMS),
    homeVenueId: readString(record.homeVenueId, INVALID_TEAMS),
    homeVenue: readVenueSnapshot(record.homeVenue),
    cfbdTeamId: readOptionalNumber(record.cfbdTeamId, INVALID_TEAMS),
  }
}

export function parseRivalries(value: unknown): RivalryRecord[] {
  return readList(value, INVALID_RIVALRIES, parseRivalry)
}

export function parseRivalry(value: unknown): RivalryRecord {
  const record = readObject(value, INVALID_RIVALRIES)
  return {
    ...readAudit(record, INVALID_RIVALRIES),
    rivalryName: readString(record.rivalryName, INVALID_RIVALRIES),
    team1Id: readString(record.team1Id, INVALID_RIVALRIES),
    team2Id: readString(record.team2Id, INVALID_RIVALRIES),
    team1: readTeamSnapshot(record.team1),
    team2: readTeamSnapshot(record.team2),
  }
}

function readList<T>(
  value: unknown,
  message: string,
  parse: (item: unknown) => T,
): T[] {
  if (!Array.isArray(value)) {
    throw new Error(message)
  }
  return value.map(parse)
}

function readObject(value: unknown, message: string): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(message)
  }
  return value as Record<string, unknown>
}

function readAudit(
  record: Record<string, unknown>,
  message: string,
): AuditFields {
  return {
    id: readString(record.id, message),
    lastUpdateDate: readOptionalString(record.lastUpdateDate, message),
    lastUpdateUser: readOptionalString(record.lastUpdateUser, message),
    version: readOptionalNumber(record.version, message),
  }
}

function readString(value: unknown, message: string): string {
  if (typeof value !== 'string') {
    throw new Error(message)
  }
  return value
}

function readOptionalString(value: unknown, message: string): string | null {
  if (value == null) {
    return null
  }
  return readString(value, message)
}

function readBoolean(value: unknown, message: string): boolean {
  if (typeof value !== 'boolean') {
    throw new Error(message)
  }
  return value
}

function readInteger(value: unknown, message: string): number {
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    throw new Error(message)
  }
  return value
}

function readOptionalNumber(value: unknown, message: string): number | null {
  if (value == null) {
    return null
  }
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(message)
  }
  return value
}

function readVenueSnapshot(value: unknown): VenueSnapshot | null {
  if (value == null) {
    return null
  }
  const record = readObject(value, INVALID_TEAMS)
  return {
    id: readString(record.id, INVALID_TEAMS),
    name: readString(record.name, INVALID_TEAMS),
    cityState: readString(record.cityState, INVALID_TEAMS),
  }
}

function readTeamSnapshot(value: unknown): TeamSnapshot | null {
  if (value == null) {
    return null
  }
  const record = readObject(value, INVALID_RIVALRIES)
  return {
    id: readString(record.id, INVALID_RIVALRIES),
    name: readString(record.name, INVALID_RIVALRIES),
  }
}
