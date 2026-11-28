import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'

import { MatchupForm } from '@/components/admin/matchup-form'
import { MatchupList } from '@/components/admin/matchup-list'
import { ThemeForm } from '@/components/admin/theme-form'
import { ThemeList } from '@/components/admin/theme-list'
import { UserForm } from '@/components/admin/user-form'
import { UserList } from '@/components/admin/user-list'
import { QueryProvider } from '@/components/query-provider'
import { searchStorageKey } from '@/lib/admin/search-memory'

const navigation = vi.hoisted(() => ({
  push: vi.fn<(path: string) => void>(),
  replace: vi.fn<(path: string) => void>(),
  pathname: '/matchups',
  params: new URLSearchParams(),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: navigation.push,
    replace: navigation.replace,
  }),
  usePathname: () => navigation.pathname,
  useSearchParams: () => navigation.params,
}))

vi.mock('next/link', () => ({
  default: ({
    href,
    children,
    className,
  }: {
    href: string
    children: ReactNode
    className?: string
  }) => (
    <a href={href} className={className}>
      {children}
    </a>
  ),
}))

const seasons = [
  {
    id: 'season-1',
    season: '2026',
    beginDate: '2026-08-27',
    endDate: '2026-12-02',
    isCurrent: true,
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
]

const weeks = [
  {
    id: 'week-1',
    seasonId: 'season-1',
    weekNumber: 1,
    beginDate: '2026-08-27',
    endDate: '2026-09-02',
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
  {
    id: 'week-2',
    seasonId: 'season-1',
    weekNumber: 2,
    beginDate: '2026-09-03',
    endDate: '2026-09-09',
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
]

const venues = [
  {
    id: 'venue-1',
    venueName: 'Sanford Stadium',
    cityState: 'Athens, GA',
    cfbdVenueId: 44,
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
  {
    id: 'venue-2',
    venueName: 'Bryant-Denny Stadium',
    cityState: 'Tuscaloosa, AL',
    cfbdVenueId: null,
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
]

const georgia = {
  id: 'georgia',
  teamName: 'Georgia',
  squadName: 'Bulldogs',
  conferenceMember: true,
  homeVenueId: 'venue-1',
  homeVenue: {
    id: 'venue-1',
    name: 'Sanford Stadium',
    cityState: 'Athens, GA',
  },
  cfbdTeamId: 61,
  version: 3,
  lastUpdateDate: null,
  lastUpdateUser: null,
}

const alabama = {
  id: 'alabama',
  teamName: 'Alabama',
  squadName: 'Crimson Tide',
  conferenceMember: true,
  homeVenueId: 'venue-2',
  homeVenue: {
    id: 'venue-2',
    name: 'Bryant-Denny Stadium',
    cityState: 'Tuscaloosa, AL',
  },
  cfbdTeamId: 333,
  version: 1,
  lastUpdateDate: null,
  lastUpdateUser: null,
}

const teams = [georgia, alabama]

const themes = [
  {
    id: 'theme-light',
    themeName: 'Light',
    themePath: 'light',
    active: true,
    primary: '#1F2937',
    secondary: '#2563EB',
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
  {
    id: 'theme-dark',
    themeName: 'Dark',
    themePath: 'dark',
    active: false,
    primary: '#0F172A',
    secondary: '#38BDF8',
    version: 2,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
]

const player = {
  id: 'ada',
  uid: 'ada',
  emailAddr: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Lovelace',
  nickName: 'Ada',
  themeId: 'light',
  roles: ['player'],
  version: 4,
  lastUpdateDate: '2026-08-01T15:04:05Z',
  lastUpdateUser: 'boss',
}

function renderScreen(ui: ReactNode) {
  return render(<QueryProvider>{ui}</QueryProvider>)
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

async function chooseOption(label: string, option: string) {
  fireEvent.click(await screen.findByRole('combobox', { name: label }))
  const item = await screen.findByRole('option', { name: option })
  fireEvent.pointerDown(item, { pointerType: 'mouse', button: 0 })
  fireEvent.click(item)
  await waitFor(() => {
    expect(
      screen
        .getByRole('combobox', { name: label })
        .getAttribute('aria-expanded'),
    ).toBe('false')
  })
}

describe('MatchupList', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    navigation.replace.mockReset()
    navigation.pathname = '/matchups'
    navigation.params = new URLSearchParams()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('loads weeks for the chosen season and searches with the helper query', async () => {
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        calls.push(url)
        if (url === '/api/manager/seasons') {
          return jsonResponse(seasons)
        }
        if (url === '/api/manager/teams') {
          return jsonResponse(teams)
        }
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        if (url === '/api/manager/season-weeks?seasonId=season-1') {
          return jsonResponse(weeks)
        }
        if (url.startsWith('/api/manager/matchups')) {
          return jsonResponse([
            {
              id: 'game-1',
              seasonId: 'season-1',
              seasonWeekId: 'week-1',
              weekNumber: 1,
              matchupDate: '2026-08-29',
              homeTeamId: 'georgia',
              awayTeamId: 'alabama',
              homeTeam: { id: 'georgia', name: 'Georgia', squad: 'Bulldogs' },
              awayTeam: {
                id: 'alabama',
                name: 'Alabama',
                squad: 'Crimson Tide',
              },
              homeTeamScore: 21,
              awayTeamScore: null,
              venueId: 'venue-1',
              venue: {
                id: 'venue-1',
                name: 'Sanford Stadium',
                cityState: 'Athens, GA',
              },
              cfbdGameId: null,
              version: 1,
              lastUpdateDate: null,
              lastUpdateUser: 'ada',
            },
          ])
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<MatchupList />)
    expect(await screen.findByRole('combobox', { name: 'Season' })).toBeTruthy()
    expect(calls.some((url) => url.startsWith('/api/manager/matchups'))).toBe(
      false,
    )

    await chooseOption('Season', '2026')
    await waitFor(() => {
      expect(calls).toContain('/api/manager/season-weeks?seasonId=season-1')
    })
    await chooseOption('Week Number', '1')
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('Georgia Bulldogs')).toBeTruthy()
    expect(screen.getByText('21')).toBeTruthy()
    expect(calls).toContain(
      '/api/manager/matchups?seasonId=season-1&seasonWeekId=week-1',
    )
    expect(sessionStorage.getItem(searchStorageKey('matchups'))).toBe(
      'seasonId=season-1&seasonWeekId=week-1',
    )
  })
})

describe('MatchupForm', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    vi.unstubAllGlobals()
  })

  it('constrains the date to the week and fills the home venue from the team', async () => {
    const bodies: unknown[] = []
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        calls.push(url)
        if (url === '/api/manager/seasons') {
          return jsonResponse(seasons)
        }
        if (url === '/api/manager/teams') {
          return jsonResponse([
            { ...georgia, homeVenueId: '', homeVenue: null },
            alabama,
          ])
        }
        if (url === '/api/manager/teams/georgia') {
          return jsonResponse(georgia)
        }
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        if (url === '/api/manager/season-weeks?seasonId=season-1') {
          return jsonResponse(weeks)
        }
        if (init?.method === 'POST') {
          bodies.push(JSON.parse(String(init.body)))
          return jsonResponse({ id: 'game-2' }, 201)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<MatchupForm mode='add' />)
    expect(await screen.findByRole('combobox', { name: 'Season' })).toBeTruthy()
    const date = screen.getByLabelText('Date')
    expect(date.hasAttribute('disabled')).toBe(true)

    await chooseOption('Season', '2026')
    await waitFor(() => {
      expect(calls).toContain('/api/manager/season-weeks?seasonId=season-1')
    })
    await chooseOption('Week Number', '1')
    expect(date.getAttribute('min')).toBe('2026-08-27')
    expect(date.getAttribute('max')).toBe('2026-09-02')
    expect((date as HTMLInputElement).value).toBe('2026-08-29')

    fireEvent.change(date, { target: { value: '2026-10-01' } })
    expect((date as HTMLInputElement).value).toBe('2026-08-29')

    await chooseOption('Home Team', 'Georgia Bulldogs')
    await waitFor(() => {
      expect(calls).toContain('/api/manager/teams/georgia')
    })
    expect(
      screen.getByRole('combobox', { name: 'Venue' }).textContent,
    ).toContain('Sanford Stadium')
    await chooseOption('Venue', 'Bryant-Denny Stadium')
    await chooseOption('Away Team', 'Alabama Crimson Tide')
    fireEvent.change(screen.getByLabelText('Home Team Score'), {
      target: { value: '24' },
    })
    fireEvent.change(screen.getByLabelText('Away Team Score'), {
      target: { value: '17' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/matchups?resume=1')
    })
    expect(bodies[0]).toEqual({
      seasonId: 'season-1',
      seasonWeekId: 'week-1',
      matchupDate: '2026-08-29',
      homeTeamId: 'georgia',
      awayTeamId: 'alabama',
      homeTeamScore: 24,
      awayTeamScore: 17,
      venueId: 'venue-2',
      cfbdGameId: null,
    })
  })

  it('keeps a venue chosen before the team detail returns', async () => {
    let releaseTeam: (response: Response) => void = () => {}
    const teamDetail = new Promise<Response>((resolve) => {
      releaseTeam = resolve
    })
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        if (url === '/api/manager/seasons') {
          return jsonResponse(seasons)
        }
        if (url === '/api/manager/teams') {
          return jsonResponse(teams)
        }
        if (url === '/api/manager/teams/georgia') {
          return teamDetail
        }
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<MatchupForm mode='add' />)
    await screen.findByRole('combobox', { name: 'Season' })
    await chooseOption('Home Team', 'Georgia Bulldogs')
    expect(
      screen.getByRole('combobox', { name: 'Venue' }).textContent,
    ).toContain('Sanford Stadium')
    await chooseOption('Venue', 'Bryant-Denny Stadium')
    await act(async () => {
      releaseTeam(jsonResponse(georgia))
      await teamDetail
    })
    expect(
      screen.getByRole('combobox', { name: 'Venue' }).textContent,
    ).toContain('Bryant-Denny Stadium')
  })

  it('shows an API date error on the date field', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url === '/api/manager/seasons') {
          return jsonResponse(seasons)
        }
        if (url === '/api/manager/teams') {
          return jsonResponse(teams)
        }
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        if (url === '/api/manager/season-weeks?seasonId=season-1') {
          return jsonResponse(weeks)
        }
        if (init?.method === 'POST') {
          return jsonResponse({ detail: 'matchup date is invalid' }, 400)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<MatchupForm mode='add' />)
    await screen.findByRole('combobox', { name: 'Season' })
    await chooseOption('Season', '2026')
    await chooseOption('Week Number', '1')
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('matchup date is invalid')).toBeTruthy()
  })

  it('shows a failed week load and keeps the week select disabled', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        if (url === '/api/manager/seasons') {
          return jsonResponse(seasons)
        }
        if (url === '/api/manager/teams') {
          return jsonResponse(teams)
        }
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        if (url.startsWith('/api/manager/season-weeks')) {
          return jsonResponse({ detail: 'weeks unavailable' }, 400)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<MatchupForm mode='add' />)
    await chooseOption('Season', '2026')
    expect(await screen.findByText('weeks unavailable')).toBeTruthy()
    const week = screen.getByRole('combobox', { name: 'Week Number' })
    expect(
      week.hasAttribute('disabled') ||
        week.getAttribute('aria-disabled') === 'true',
    ).toBe(true)
    expect(screen.getByLabelText('Date').hasAttribute('disabled')).toBe(true)
  })
})

describe('UserList', () => {
  beforeEach(() => {
    navigation.pathname = '/users'
    navigation.params = new URLSearchParams()
    navigation.replace.mockReset()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('searches by name and does not offer add', async () => {
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        calls.push(url)
        if (url === '/api/manager/themes') {
          return jsonResponse(themes)
        }
        return jsonResponse([player])
      }),
    )

    renderScreen(<UserList />)
    expect(await screen.findByLabelText('Email Address')).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Add' })).toBeNull()
    fireEvent.change(screen.getByLabelText('Last Name'), {
      target: { value: 'Love' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByText('Light')).toBeTruthy()
    expect(calls).toContain('/api/manager/users?lastName=Love')
    expect(
      screen.getByRole('link', { name: 'Edit' }).getAttribute('href'),
    ).toBe('/users/ada')
  })
})

describe('UserForm', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    vi.unstubAllGlobals()
  })

  it('saves a manager role and refuses an empty role list', async () => {
    const bodies: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url === '/api/manager/themes') {
          return jsonResponse(themes)
        }
        if (url === '/api/manager/users/ada') {
          if (init?.method === 'PUT') {
            bodies.push(JSON.parse(String(init.body)))
            return jsonResponse({ ...player, roles: ['player', 'manager'] })
          }
          return jsonResponse(player)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<UserForm id='ada' />)
    expect(await screen.findByLabelText('Nickname')).toBeTruthy()
    expect(
      screen.getByText(
        'A manager role applies the next time this user signs in.',
      ),
    ).toBeTruthy()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Manager' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/users?resume=1')
    })
    expect(bodies[0]).toMatchObject({
      uid: 'ada',
      emailAddr: 'ada@example.com',
      firstName: 'Ada',
      lastName: 'Lovelace',
      nickName: 'Ada',
      themeId: 'light',
      roles: ['player', 'manager'],
      version: 4,
    })

    fireEvent.click(screen.getByRole('checkbox', { name: 'Player' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Manager' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(
      await screen.findByText('User must belong to at least one group.'),
    ).toBeTruthy()
    expect(bodies).toHaveLength(1)
  })
})

describe('ThemeList', () => {
  beforeEach(() => {
    navigation.pathname = '/themes'
    navigation.params = new URLSearchParams()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('searches by path', async () => {
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        calls.push(String(input))
        return jsonResponse(themes)
      }),
    )

    renderScreen(<ThemeList />)
    fireEvent.change(await screen.findByLabelText('Path'), {
      target: { value: 'dark' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    expect(await screen.findByText('Dark')).toBeTruthy()
    expect(calls).toContain('/api/manager/themes?themePath=dark')
    expect(screen.getByRole('link', { name: 'Add' }).getAttribute('href')).toBe(
      '/themes/new',
    )
  })
})

describe('ThemeForm', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    vi.unstubAllGlobals()
  })

  it('rejects a leading slash and saves name, path, and active', async () => {
    const bodies: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        if (init?.method === 'POST') {
          bodies.push(JSON.parse(String(init.body)))
          return jsonResponse({ id: 'theme-3' }, 201)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<ThemeForm mode='add' />)
    fireEvent.change(screen.getByLabelText('Name'), {
      target: { value: 'Georgia Bulldogs' },
    })
    fireEvent.change(screen.getByLabelText('Path'), {
      target: { value: '/georgia' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(
      await screen.findByText('theme path must not begin with a slash'),
    ).toBeTruthy()
    expect(bodies).toEqual([])

    fireEvent.change(screen.getByLabelText('Path'), {
      target: { value: 'georgia' },
    })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Active' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/themes?resume=1')
    })
    expect(bodies[0]).toEqual({
      themeName: 'Georgia Bulldogs',
      themePath: 'georgia',
      active: true,
      primary: '',
      secondary: '',
    })
  })

  it('clears stored colors when the path changes', async () => {
    const bodies: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (
          url === '/api/manager/themes/theme-light' &&
          init?.method !== 'PUT'
        ) {
          return jsonResponse(themes[0])
        }
        if (init?.method === 'PUT') {
          bodies.push(JSON.parse(String(init.body)))
          return jsonResponse({ ...themes[0], themePath: 'dark' })
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<ThemeForm mode='edit' id='theme-light' />)
    fireEvent.change(await screen.findByLabelText('Path'), {
      target: { value: 'dark' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/themes?resume=1')
    })
    expect(bodies[0]).toMatchObject({
      themePath: 'dark',
      primary: '',
      secondary: '',
    })
  })
})
