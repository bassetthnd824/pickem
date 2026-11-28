import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'

import { RivalryForm } from '@/components/admin/rivalry-form'
import { SeasonForm } from '@/components/admin/season-form'
import { SeasonList } from '@/components/admin/season-list'
import { SeasonWeekForm } from '@/components/admin/season-week-form'
import { TeamForm } from '@/components/admin/team-form'
import { QueryProvider } from '@/components/query-provider'
import { searchStorageKey } from '@/lib/admin/search-memory'

const navigation = vi.hoisted(() => ({
  push: vi.fn<(path: string) => void>(),
  replace: vi.fn<(path: string) => void>(),
  pathname: '/seasons',
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
    isCurrent: false,
    version: 2,
    lastUpdateDate: '2026-08-01T15:04:05Z',
    lastUpdateUser: 'ada',
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
]

const teams = [
  {
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
  },
  {
    id: 'alabama',
    teamName: 'Alabama',
    squadName: 'Crimson Tide',
    conferenceMember: true,
    homeVenueId: 'venue-2',
    homeVenue: null,
    cfbdTeamId: null,
    version: 1,
    lastUpdateDate: null,
    lastUpdateUser: null,
  },
]

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

describe('SeasonList', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    navigation.replace.mockReset()
    navigation.pathname = '/seasons'
    navigation.params = new URLSearchParams()
    sessionStorage.clear()
    vi.unstubAllGlobals()
  })

  it('starts blank and searches with the criteria, then offers add and edit', async () => {
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        calls.push(url)
        return jsonResponse(seasons)
      }),
    )

    renderScreen(<SeasonList />)
    expect(await screen.findByRole('button', { name: 'Search' })).toBeTruthy()
    expect(screen.queryByText('No results found')).toBeNull()
    expect(screen.getByRole('link', { name: 'Add' }).getAttribute('href')).toBe(
      '/seasons/new',
    )
    expect(calls).toEqual([])

    fireEvent.change(screen.getByLabelText('Season'), {
      target: { value: '2026' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(await screen.findByText('2026-08-27')).toBeTruthy()
    expect(screen.getByText('ada')).toBeTruthy()
    expect(calls).toEqual(['/api/manager/seasons?season=2026'])
    expect(sessionStorage.getItem(searchStorageKey('seasons'))).toBe(
      'season=2026',
    )
    expect(navigation.replace).toHaveBeenCalledWith('/seasons?resume=1', {
      scroll: false,
    })
    expect(
      screen.getByRole('link', { name: 'Edit' }).getAttribute('href'),
    ).toBe('/seasons/season-1')

    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => {
      expect(calls).toEqual([
        '/api/manager/seasons?season=2026',
        '/api/manager/seasons?season=2026',
      ])
    })
  })

  it('does not load every row when resume has no stored search', async () => {
    navigation.params = new URLSearchParams('resume=1')
    const fetchMock = vi.fn<typeof fetch>()
    vi.stubGlobal('fetch', fetchMock)

    renderScreen(<SeasonList />)

    expect(await screen.findByRole('button', { name: 'Search' })).toBeTruthy()
    expect(screen.queryByText('No results found')).toBeNull()
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('reloads the stored search when returning from a detail screen', async () => {
    navigation.params = new URLSearchParams('resume=1')
    sessionStorage.setItem(searchStorageKey('seasons'), 'season=2026')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(seasons)),
    )

    renderScreen(<SeasonList />)

    expect(await screen.findByText('2026-08-27')).toBeTruthy()
    expect((screen.getByLabelText('Season') as HTMLInputElement).value).toBe(
      '2026',
    )
  })
})

describe('SeasonForm', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    vi.unstubAllGlobals()
  })

  it('shows API validation messages and returns to the last search after save', async () => {
    const bodies: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
        if (init?.method === 'POST') {
          bodies.push(JSON.parse(String(init.body)))
          if (bodies.length === 1) {
            return jsonResponse(
              {
                detail: 'season must not be blank; beginDate must not be blank',
              },
              400,
            )
          }
          return jsonResponse({ id: 'season-2' }, 201)
        }
        return jsonResponse(seasons[0])
      }),
    )

    renderScreen(<SeasonForm mode='add' />)
    expect(
      screen.getByRole('link', { name: 'Cancel' }).getAttribute('href'),
    ).toBe('/seasons?resume=1')
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findAllByText('must not be blank')).toHaveLength(2)

    fireEvent.change(screen.getByLabelText('Season'), {
      target: { value: '2027' },
    })
    fireEvent.change(screen.getByLabelText('Begin Date'), {
      target: { value: '2027-08-26' },
    })
    fireEvent.change(screen.getByLabelText('End Date'), {
      target: { value: '2027-12-01' },
    })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Current' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/seasons?resume=1')
    })
    expect(bodies[1]).toEqual({
      season: '2027',
      beginDate: '2027-08-26',
      endDate: '2027-12-01',
      isCurrent: true,
    })
  })

  it('keeps a blocked delete on the form', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
        if (init?.method === 'DELETE') {
          return jsonResponse({ detail: 'blocked by season weeks' }, 409)
        }
        return jsonResponse(seasons[0])
      }),
    )

    renderScreen(<SeasonForm mode='edit' id='season-1' />)
    expect(await screen.findByDisplayValue('2026')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    expect((await screen.findByRole('alert')).textContent).toContain(
      'blocked by season weeks',
    )
    expect(navigation.push).not.toHaveBeenCalled()
  })

  it('keeps the loaded version when a dirty edit conflicts', async () => {
    const bodies: unknown[] = []
    let reads = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
        if (init?.method === 'PUT') {
          bodies.push(JSON.parse(String(init.body)))
          return jsonResponse({ detail: 'version is stale' }, 409)
        }
        reads += 1
        return jsonResponse({ ...seasons[0], version: reads === 1 ? 2 : 9 })
      }),
    )

    renderScreen(<SeasonForm mode='edit' id='season-1' />)
    expect(await screen.findByDisplayValue('2026')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Season'), {
      target: { value: '2026b' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByRole('alert')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(bodies).toHaveLength(2)
    })
    expect(bodies[0]).toMatchObject({ season: '2026b', version: 2 })
    expect(bodies[1]).toMatchObject({ season: '2026b', version: 2 })
  })
})

describe('SeasonWeekForm', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('defaults begin and end when a season and week number are chosen', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse(seasons)),
    )

    renderScreen(<SeasonWeekForm mode='add' />)
    await chooseOption('Season', '2026')
    fireEvent.change(screen.getByLabelText('Week Number'), {
      target: { value: '2' },
    })

    await waitFor(() => {
      expect(
        (screen.getByLabelText('Begin Date') as HTMLInputElement).value,
      ).toBe('2026-09-03')
    })
    expect((screen.getByLabelText('End Date') as HTMLInputElement).value).toBe(
      '2026-09-09',
    )
  })

  it('keeps stored dates when the season and week number do not change', async () => {
    const week = {
      id: 'week-1',
      seasonId: 'season-1',
      weekNumber: 3,
      beginDate: '2026-09-20',
      endDate: '2026-09-26',
      version: 4,
      lastUpdateDate: null,
      lastUpdateUser: null,
    }
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        if (String(input) === '/api/manager/season-weeks/week-1') {
          return jsonResponse(week)
        }
        return jsonResponse(seasons)
      }),
    )

    renderScreen(<SeasonWeekForm mode='edit' id='week-1' />)
    expect(await screen.findByDisplayValue('2026-09-20')).toBeTruthy()
    await chooseOption('Season', '2026')
    expect(
      (screen.getByLabelText('Begin Date') as HTMLInputElement).value,
    ).toBe('2026-09-20')
    fireEvent.change(screen.getByLabelText('Week Number'), {
      target: { value: '3 ' },
    })
    expect(
      (screen.getByLabelText('Begin Date') as HTMLInputElement).value,
    ).toBe('2026-09-20')
    expect(
      (screen.getByLabelText('Week Number') as HTMLInputElement).value,
    ).toBe('3')
    fireEvent.change(screen.getByLabelText('Week Number'), {
      target: { value: '2' },
    })
    await waitFor(() => {
      expect(
        (screen.getByLabelText('Begin Date') as HTMLInputElement).value,
      ).toBe('2026-09-03')
    })
    expect((screen.getByLabelText('End Date') as HTMLInputElement).value).toBe(
      '2026-09-09',
    )
  })
})

describe('TeamForm', () => {
  beforeEach(() => {
    navigation.push.mockReset()
    vi.unstubAllGlobals()
  })

  it('saves the squad, conference flag, and home venue', async () => {
    const bodies: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url === '/api/manager/venues') {
          return jsonResponse(venues)
        }
        if (init?.method === 'POST') {
          bodies.push(JSON.parse(String(init.body)))
          return jsonResponse({ id: 'team-2' }, 201)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<TeamForm mode='add' />)
    expect(
      await screen.findByRole('combobox', { name: 'Home Venue' }),
    ).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Name'), {
      target: { value: 'Georgia' },
    })
    fireEvent.change(screen.getByLabelText('Squad Name'), {
      target: { value: 'Bulldogs' },
    })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Conference Member' }))
    await chooseOption('Home Venue', 'Sanford Stadium')
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(navigation.push).toHaveBeenCalledWith('/teams?resume=1')
    })
    expect(bodies[0]).toEqual({
      teamName: 'Georgia',
      squadName: 'Bulldogs',
      conferenceMember: true,
      homeVenueId: 'venue-1',
      cfbdTeamId: null,
    })
  })
})

describe('RivalryForm', () => {
  beforeEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the API error when the two teams are the same', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        if (String(input) === '/api/manager/teams') {
          return jsonResponse(teams)
        }
        if (init?.method === 'POST') {
          return jsonResponse({ detail: 'teams cannot be equal' }, 400)
        }
        return jsonResponse({ detail: 'missing' }, 404)
      }),
    )

    renderScreen(<RivalryForm mode='add' />)
    expect(await screen.findByRole('combobox', { name: 'Team 1' })).toBeTruthy()
    await chooseOption('Team 1', 'Georgia Bulldogs')
    await chooseOption('Team 2', 'Georgia Bulldogs')
    fireEvent.change(screen.getByLabelText('Name'), {
      target: { value: 'Deep South' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('teams cannot be equal')).toBeTruthy()
  })
})
