import { ApiError } from '@/lib/api-client'

const ALIASES: Record<string, readonly string[]> = {
  'season begin date is invalid': ['beginDate'],
  'season end date is invalid': ['endDate'],
  'season week begin date is invalid': ['beginDate'],
  'season week end date is invalid': ['endDate'],
  'Week Begin Date is invalid': ['beginDate'],
  'week number is invalid': ['weekNumber'],
  'week number must be unique for the season': ['weekNumber'],
  'Season not found': ['seasonId'],
  'Season week not found': ['seasonWeekId'],
  'team name must be unique': ['teamName'],
  'teams cannot be equal': ['awayTeamId', 'team2Id'],
  'Venue not found': ['venueId', 'homeVenueId'],
  'Team not found': ['homeTeamId', 'awayTeamId'],
  'matchup date is invalid': ['matchupDate'],
  'season week does not belong to the season': ['seasonId'],
  'week number does not match the season week': ['seasonWeekId'],
  'scores must both be set or both be empty': ['homeTeamScore'],
  'tied scores are invalid': ['homeTeamScore'],
  'theme path must not begin with a slash': ['themePath'],
  'theme key is not one of the 18 compiled palettes': ['themePath', 'themeId'],
  'theme name must be unique': ['themeName'],
  'theme path must be unique': ['themePath'],
  'User must belong to at least one group.': ['roles'],
  'role is invalid': ['roles'],
  'email address must be unique': ['emailAddr'],
}

export type FieldErrors = Record<string, string>

export function apiProblem(error: unknown): string {
  if (error instanceof ApiError) {
    const { body } = error
    if (typeof body === 'string' && body.trim() !== '') {
      return body
    }
    if (body && typeof body === 'object' && 'detail' in body) {
      const detail = body.detail
      if (typeof detail === 'string' && detail.trim() !== '') {
        return detail
      }
    }
  }
  return 'The request failed.'
}

export function assignFieldErrors(
  detail: string,
  fields: readonly string[],
): { fieldErrors: FieldErrors; formError?: string } {
  const fieldErrors: FieldErrors = {}
  const form: string[] = []
  for (const part of detail
    .split(';')
    .map((item) => item.trim())
    .filter((item) => item !== '')) {
    const alias = ALIASES[part]?.find((field) => fields.includes(field))
    const prefixed = fields.find(
      (field) => part === field || part.startsWith(`${field} `),
    )
    const field = alias ?? prefixed
    if (!field || !fields.includes(field)) {
      form.push(part)
      continue
    }
    const message = alias || !prefixed ? part : part.slice(field.length).trim()
    const prior = fieldErrors[field]
    fieldErrors[field] = prior ? `${prior}; ${message}` : message
  }
  return {
    fieldErrors,
    formError: form.length > 0 ? form.join('; ') : undefined,
  }
}
