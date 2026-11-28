import { formatAudit } from '@/lib/admin/audit'
import { assignFieldErrors, apiProblem } from '@/lib/admin/problem-detail'
import {
  parseSeason,
  parseSeasons,
  seasonCriteriaFromQuery,
  seasonSearchQuery,
} from '@/lib/admin/records'
import {
  recallSearch,
  rememberSearch,
  resumeHref,
  searchStorageKey,
} from '@/lib/admin/search-memory'
import { defaultWeekDates, parseWeekNumber } from '@/lib/admin/week-dates'
import { ApiError } from '@/lib/api-client'

describe('defaultWeekDates', () => {
  it('starts week 1 on the season begin date and ends six days later', () => {
    expect(defaultWeekDates('2026-08-27', 1)).toEqual({
      beginDate: '2026-08-27',
      endDate: '2026-09-02',
    })
  })

  it('steps later weeks by seven days from the season begin date', () => {
    expect(defaultWeekDates('2026-08-27', 2)).toEqual({
      beginDate: '2026-09-03',
      endDate: '2026-09-09',
    })
    expect(parseWeekNumber('2')).toBe(2)
    expect(parseWeekNumber('0')).toBeNull()
    expect(parseWeekNumber('2.5')).toBeNull()
    expect(defaultWeekDates('2026-08-27', 0)).toBeNull()
    expect(defaultWeekDates('August 27', 1)).toBeNull()
  })
})

describe('assignFieldErrors', () => {
  it('puts bean messages on the named fields and keeps other text on the form', () => {
    expect(
      assignFieldErrors(
        'season must not be blank; beginDate must not be blank; blocked by season weeks',
        ['season', 'beginDate', 'endDate'],
      ),
    ).toEqual({
      fieldErrors: {
        season: 'must not be blank',
        beginDate: 'must not be blank',
      },
      formError: 'blocked by season weeks',
    })
  })

  it('maps season and week calendar messages onto the date and week fields', () => {
    expect(
      assignFieldErrors('season begin date is invalid', [
        'season',
        'beginDate',
        'endDate',
      ]).fieldErrors.beginDate,
    ).toBe('season begin date is invalid')
    expect(
      assignFieldErrors('Week Begin Date is invalid', [
        'beginDate',
        'weekNumber',
      ]).fieldErrors.beginDate,
    ).toBe('Week Begin Date is invalid')
    expect(
      assignFieldErrors('teams cannot be equal', ['team1Id', 'team2Id'])
        .fieldErrors.team2Id,
    ).toBe('teams cannot be equal')
  })
})

describe('apiProblem', () => {
  it('reads a problem detail and falls back when the body has none', () => {
    expect(
      apiProblem(new ApiError(400, { detail: 'season must be unique' })),
    ).toBe('season must be unique')
    expect(apiProblem(new ApiError(409, { title: 'Conflict' }))).toBe(
      'The request failed.',
    )
    expect(apiProblem(new Error('nope'))).toBe('The request failed.')
  })
})

describe('admin records', () => {
  it('reads isCurrent and builds a season search query', () => {
    expect(
      parseSeason({
        id: 'season-1',
        season: '2026',
        beginDate: '2026-08-27',
        endDate: '2026-12-02',
        isCurrent: true,
        version: 2,
        lastUpdateDate: null,
        lastUpdateUser: null,
      }).current,
    ).toBe(true)
    expect(parseSeasons([])).toEqual([])
    expect(
      seasonSearchQuery({
        season: ' 2026 ',
        beginDate: '',
        endDate: '2026-12-02',
      }),
    ).toBe('season=2026&endDate=2026-12-02')
    expect(seasonCriteriaFromQuery('season=2026')).toEqual({
      season: '2026',
      beginDate: '',
      endDate: '',
    })
    expect(formatAudit('2026-08-01T15:04:05Z')).toBe('2026-08-01 03:04:05 PM')
    expect(formatAudit(null)).toBe('')
  })
})

describe('search memory', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('stores the last search for a later resume', () => {
    rememberSearch('seasons', 'season=2026')
    expect(sessionStorage.getItem(searchStorageKey('seasons'))).toBe(
      'season=2026',
    )
    expect(recallSearch('seasons')).toBe('season=2026')
    expect(resumeHref('/seasons')).toBe('/seasons?resume=1')
  })
})
