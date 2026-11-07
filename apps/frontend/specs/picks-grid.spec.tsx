import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'

import { AuthProvider } from '@/components/auth-provider'
import { PicksGrid } from '@/components/picks-grid'
import { QueryProvider } from '@/components/query-provider'
import type { AuthProfile } from '@/lib/auth-profile'

const profile: AuthProfile = {
  uid: 'player-1',
  emailAddr: 'pat@example.com',
  firstName: 'Pat',
  lastName: 'Player',
  nickName: 'Pat',
  themeId: 'light',
  roles: ['player'],
}

const main = {
  seasonId: 'season-1',
  season: '2026',
  numberOfConferenceTeams: 16,
  weeks: [
    {
      id: 'week-1',
      weekNumber: 1,
      beginDate: '2026-09-03',
      endDate: '2026-09-09',
      currentOrPast: true,
      matchups: [
        {
          matchupId: 'correct',
          matchupDate: '2026-09-05',
          awayTeam: { id: 'auburn', name: 'Auburn', squad: 'Tigers' },
          awayTeamScore: 14,
          homeTeam: { id: 'alabama', name: 'Alabama', squad: 'Crimson Tide' },
          homeTeamScore: 31,
          venue: { name: 'Bryant-Denny Stadium', cityState: 'Tuscaloosa, AL' },
          rivalryName: 'Iron Bowl',
          pickedTeamId: 'alabama',
          rank: 5,
          winningTeamId: 'alabama',
        },
        {
          matchupId: 'wrong',
          matchupDate: '2026-09-05',
          awayTeam: { id: 'georgia', name: 'Georgia', squad: 'Bulldogs' },
          awayTeamScore: 24,
          homeTeam: { id: 'florida', name: 'Florida', squad: 'Gators' },
          homeTeamScore: 10,
          venue: {
            name: 'Ben Hill Griffin Stadium',
            cityState: 'Gainesville, FL',
          },
          rivalryName: null,
          pickedTeamId: 'florida',
          rank: 1,
          winningTeamId: 'georgia',
        },
        {
          matchupId: 'unscored',
          matchupDate: '2026-09-06',
          awayTeam: { id: 'kentucky', name: 'Kentucky', squad: 'Wildcats' },
          awayTeamScore: null,
          homeTeam: { id: 'tennessee', name: 'Tennessee', squad: 'Volunteers' },
          homeTeamScore: null,
          venue: null,
          rivalryName: null,
          pickedTeamId: 'tennessee',
          rank: 2,
          winningTeamId: null,
        },
        {
          matchupId: 'tied',
          matchupDate: '2026-09-06',
          awayTeam: { id: 'missouri', name: 'Missouri', squad: 'Tigers' },
          awayTeamScore: 17,
          homeTeam: { id: 'arkansas', name: 'Arkansas', squad: 'Razorbacks' },
          homeTeamScore: 17,
          venue: null,
          rivalryName: null,
          pickedTeamId: 'arkansas',
          rank: 3,
          winningTeamId: 'arkansas',
        },
      ],
    },
    {
      id: 'week-2',
      weekNumber: 2,
      beginDate: '2026-09-10',
      endDate: '2026-09-16',
      currentOrPast: false,
      matchups: [
        {
          matchupId: 'future-a',
          matchupDate: '2026-09-12',
          awayTeam: { id: 'lsu', name: 'LSU', squad: 'Tigers' },
          awayTeamScore: 99,
          homeTeam: { id: 'ole-miss', name: 'Ole Miss', squad: 'Rebels' },
          homeTeamScore: 1,
          venue: { name: 'Vaught-Hemingway Stadium', cityState: 'Oxford, MS' },
          rivalryName: 'Magnolia Bowl',
          pickedTeamId: 'lsu',
          rank: 3,
          winningTeamId: 'ole-miss',
        },
        {
          matchupId: 'future-b',
          matchupDate: '2026-09-12',
          awayTeam: { id: 'texas', name: 'Texas', squad: 'Longhorns' },
          awayTeamScore: 40,
          homeTeam: { id: 'oklahoma', name: 'Oklahoma', squad: 'Sooners' },
          homeTeamScore: 21,
          venue: null,
          rivalryName: null,
          pickedTeamId: 'oklahoma',
          rank: 2,
          winningTeamId: 'texas',
        },
        {
          matchupId: 'future-c',
          matchupDate: '2026-09-13',
          awayTeam: { id: 'vandy', name: 'Vanderbilt', squad: 'Commodores' },
          awayTeamScore: null,
          homeTeam: {
            id: 'carolina',
            name: 'South Carolina',
            squad: 'Gamecocks',
          },
          homeTeamScore: null,
          venue: null,
          rivalryName: null,
          pickedTeamId: 'vandy',
          rank: 1,
          winningTeamId: null,
        },
      ],
    },
  ],
}

type Call = { url: string; method: string; body: string | null }

function renderGrid() {
  return render(
    <QueryProvider>
      <AuthProvider
        initialProfile={profile}
        sessionPresent
        sessionIsManager={false}
      >
        <PicksGrid />
      </AuthProvider>
    </QueryProvider>,
  )
}

function rankText(region: ReturnType<typeof within>, label: string): string {
  return region.getByLabelText(label).textContent ?? ''
}

describe('PicksGrid', () => {
  const calls: Call[] = []

  beforeEach(() => {
    calls.length = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'
        calls.push({
          url,
          method,
          body: typeof init?.body === 'string' ? init.body : null,
        })
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          return Response.json(main)
        }
        if (url === '/api/game/picks' && method === 'POST') {
          return Response.json([])
        }
        return new Response('missing', { status: 404 })
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("welcomes the player and lists the week's matchup columns", async () => {
    renderGrid()
    expect(
      await screen.findByRole('heading', { name: 'Welcome Pat, Season 2026' }),
    ).toBeTruthy()

    const begun = within(screen.getByRole('region', { name: 'Week 1' }))
    expect(begun.getByRole('columnheader', { name: 'Date' })).toBeTruthy()
    expect(begun.getByRole('columnheader', { name: 'Rivalry' })).toBeTruthy()
    expect(begun.getByRole('columnheader', { name: 'Away' })).toBeTruthy()
    expect(begun.getByRole('columnheader', { name: 'Home' })).toBeTruthy()
    expect(begun.getByRole('columnheader', { name: 'Venue' })).toBeTruthy()
    expect(begun.getByRole('columnheader', { name: 'Rank' })).toBeTruthy()
    expect(begun.getAllByText('2026-09-05').length).toBeGreaterThan(0)
    expect(begun.getByText('Iron Bowl')).toBeTruthy()
    expect(begun.getByText('Crimson Tide')).toBeTruthy()
    expect(begun.getByText('Bryant-Denny Stadium Tuscaloosa, AL')).toBeTruthy()
    expect(rankText(begun, 'Rank for Auburn at Alabama')).toBe('5')
  })

  it('locks the begun week and hides future scores and result icons', async () => {
    renderGrid()
    const begun = within(await screen.findByRole('region', { name: 'Week 1' }))
    const future = within(screen.getByRole('region', { name: 'Week 2' }))

    expect(begun.getByText('Picks are locked')).toBeTruthy()
    expectDisabled(begun.getByRole('radio', { name: 'Pick Alabama in week 1' }))
    expectDisabled(begun.getByRole('radio', { name: 'Pick Auburn in week 1' }))
    expectDisabled(
      begun.getByRole('button', { name: 'Move Auburn at Alabama up' }),
    )
    expectDisabled(
      begun.getByRole('button', { name: 'Move Auburn at Alabama down' }),
    )
    expect(begun.getByText('14')).toBeTruthy()
    expect(begun.getByText('31')).toBeTruthy()
    expect(begun.getByText('24')).toBeTruthy()
    expect(begun.getByText('10')).toBeTruthy()
    expect(begun.getByLabelText('Correct pick')).toBeTruthy()
    expect(begun.getByLabelText('Incorrect pick')).toBeTruthy()
    expect(begun.getAllByLabelText(/^(Correct|Incorrect) pick$/)).toHaveLength(
      2,
    )

    expect(future.getByText('Picks are open')).toBeTruthy()
    expectEnabled(future.getByRole('radio', { name: 'Pick LSU in week 2' }))
    expectEnabled(
      future.getByRole('radio', { name: 'Pick Ole Miss in week 2' }),
    )
    expectEnabled(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )
    expect(future.queryByText('99')).toBeNull()
    expect(future.queryByText('40')).toBeNull()
    expect(future.queryByText('21')).toBeNull()
    expect(future.queryByLabelText('Correct pick')).toBeNull()
    expect(future.queryByLabelText('Incorrect pick')).toBeNull()
    expect(future.queryByRole('columnheader', { name: 'Score' })).toBeNull()
    expect(future.queryByRole('columnheader', { name: 'Result' })).toBeNull()
    expectEnabled(screen.getByRole('button', { name: 'Save' }))
  })

  it('saves an open week and renumbers ranks from the conference count', async () => {
    renderGrid()
    const future = within(await screen.findByRole('region', { name: 'Week 2' }))

    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('3')
    expect(rankText(future, 'Rank for Texas at Oklahoma')).toBe('2')
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('1')

    fireEvent.click(
      future.getByRole('radio', { name: 'Pick Ole Miss in week 2' }),
    )
    fireEvent.click(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )

    expect(rankText(future, 'Rank for Texas at Oklahoma')).toBe('16')
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('15')
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('14')
    const lsuRank = future.getByLabelText('Rank for LSU at Ole Miss')
    const texasRank = future.getByLabelText('Rank for Texas at Oklahoma')
    expect(
      texasRank.compareDocumentPosition(lsuRank) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
    expectEnabled(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )

    fireEvent.click(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )
    expect(rankText(future, 'Rank for Texas at Oklahoma')).toBe('16')
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('15')
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('14')
    expectDisabled(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(calls.some((call) => call.method === 'POST')).toBe(true)
    })
    const posts = calls.filter((call) => call.method === 'POST')
    expect(posts).toHaveLength(1)
    expect(posts[0]?.url).toBe('/api/game/picks')
    expect(JSON.parse(posts[0]?.body ?? '{}')).toEqual({
      seasonWeekId: 'week-2',
      picks: [
        { matchupId: 'future-b', pickedTeamId: 'oklahoma', rank: 16 },
        { matchupId: 'future-c', pickedTeamId: 'vandy', rank: 15 },
        { matchupId: 'future-a', pickedTeamId: 'ole-miss', rank: 14 },
      ],
    })
  })

  it('fills an unpicked row from the conference count until a reorder', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'
        calls.push({
          url,
          method,
          body: typeof init?.body === 'string' ? init.body : null,
        })
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          return Response.json({
            ...main,
            weeks: [
              {
                ...main.weeks[1],
                matchups: [
                  main.weeks[1].matchups[0],
                  {
                    ...main.weeks[1].matchups[2],
                    pickedTeamId: null,
                    rank: null,
                  },
                ],
              },
            ],
          })
        }
        if (method === 'POST') {
          return Response.json([])
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderGrid()
    const future = within(await screen.findByRole('region', { name: 'Week 2' }))
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('3')
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('16')

    fireEvent.click(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('16')
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('15')

    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(calls.some((call) => call.method === 'POST')).toBe(true)
    })
    const post = calls.find((call) => call.method === 'POST')
    expect(JSON.parse(post?.body ?? '{}')).toEqual({
      seasonWeekId: 'week-2',
      picks: [
        { matchupId: 'future-c', pickedTeamId: null, rank: null },
        { matchupId: 'future-a', pickedTeamId: 'lsu', rank: 15 },
      ],
    })
  })

  it('assigns unused filler ranks after a partial save reloads in rank order', async () => {
    let saved = false
    const open = main.weeks[1]
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'
        calls.push({
          url,
          method,
          body: typeof init?.body === 'string' ? init.body : null,
        })
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          const blank = {
            ...open,
            matchups: open.matchups.map((matchup) => ({
              ...matchup,
              pickedTeamId: null,
              rank: null,
            })),
          }
          if (!saved) {
            return Response.json({ ...main, weeks: [blank] })
          }
          const [first, second, third] = blank.matchups
          return Response.json({
            ...main,
            weeks: [
              {
                ...blank,
                matchups: [
                  { ...third, pickedTeamId: 'vandy', rank: 14 },
                  first,
                  second,
                ],
              },
            ],
          })
        }
        if (method === 'POST') {
          saved = true
          return Response.json([])
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderGrid()
    const future = within(await screen.findByRole('region', { name: 'Week 2' }))
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('16')
    expect(rankText(future, 'Rank for Texas at Oklahoma')).toBe('15')
    expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe('14')

    fireEvent.click(
      future.getByRole('radio', { name: 'Pick Vanderbilt in week 2' }),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      const vandy = future.getByLabelText(
        'Rank for Vanderbilt at South Carolina',
      )
      const lsu = future.getByLabelText('Rank for LSU at Ole Miss')
      expect(
        vandy.compareDocumentPosition(lsu) & Node.DOCUMENT_POSITION_FOLLOWING,
      ).toBeTruthy()
      expect(rankText(future, 'Rank for Vanderbilt at South Carolina')).toBe(
        '14',
      )
      expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('16')
      expect(rankText(future, 'Rank for Texas at Oklahoma')).toBe('15')
    })

    fireEvent.click(future.getByRole('radio', { name: 'Pick LSU in week 2' }))
    fireEvent.click(future.getByRole('radio', { name: 'Pick Texas in week 2' }))
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => {
      expect(calls.filter((call) => call.method === 'POST')).toHaveLength(2)
    })
    const posts = calls.filter((call) => call.method === 'POST')
    expect(JSON.parse(posts[1]?.body ?? '{}')).toEqual({
      seasonWeekId: 'week-2',
      picks: [
        { matchupId: 'future-c', pickedTeamId: 'vandy', rank: 14 },
        { matchupId: 'future-a', pickedTeamId: 'lsu', rank: 16 },
        { matchupId: 'future-b', pickedTeamId: 'texas', rank: 15 },
      ],
    })
  })

  it('reloads the grid on cancel without saving', async () => {
    renderGrid()
    const future = within(await screen.findByRole('region', { name: 'Week 2' }))
    fireEvent.click(
      future.getByRole('radio', { name: 'Pick Ole Miss in week 2' }),
    )
    fireEvent.click(
      future.getByRole('button', { name: 'Move LSU at Ole Miss down' }),
    )
    expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('15')
    expect(
      isChecked(future.getByRole('radio', { name: 'Pick Ole Miss in week 2' })),
    ).toBe(true)

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    await waitFor(() => {
      expect(rankText(future, 'Rank for LSU at Ole Miss')).toBe('3')
    })
    expect(
      isChecked(future.getByRole('radio', { name: 'Pick LSU in week 2' })),
    ).toBe(true)
    expect(calls.some((call) => call.method === 'POST')).toBe(false)
    expect(
      calls.filter((call) => call.url === '/api/game/main').length,
    ).toBeGreaterThan(1)
  })

  it('shows the server message when a save is rejected', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          return Response.json({
            ...main,
            weeks: main.weeks.map((week) => ({
              ...week,
              currentOrPast: false,
            })),
          })
        }
        if (method === 'POST') {
          return Response.json({ detail: 'picks are locked' }, { status: 409 })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderGrid()
    await screen.findByRole('region', { name: 'Week 1' })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect((await screen.findByRole('alert')).textContent).toContain(
      'picks are locked',
    )
  })

  it('keeps the card when cancel cannot reload', async () => {
    let reads = 0
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'
        calls.push({
          url,
          method,
          body: typeof init?.body === 'string' ? init.body : null,
        })
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          reads += 1
          if (reads > 1) {
            return Response.json({ detail: 'unavailable' }, { status: 400 })
          }
          return Response.json(main)
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderGrid()
    await screen.findByRole('region', { name: 'Week 2' })
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect((await screen.findByRole('alert')).textContent).toContain(
      'Could not reload picks.',
    )
    expect(screen.getByRole('region', { name: 'Week 2' })).toBeTruthy()
    expect(
      screen.getByRole('heading', { name: 'Welcome Pat, Season 2026' }),
    ).toBeTruthy()
    expect(calls.some((call) => call.method === 'POST')).toBe(false)
  })

  it('disables save when every week has begun', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input)
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/main') {
          return Response.json({
            ...main,
            weeks: [main.weeks[0]],
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderGrid()
    await screen.findByRole('region', { name: 'Week 1' })
    expect(
      (screen.getByRole('button', { name: 'Save' }) as HTMLButtonElement)
        .disabled,
    ).toBe(true)
  })
})

function isDisabled(element: HTMLElement): boolean {
  if (
    'disabled' in element &&
    (element as HTMLButtonElement).disabled === true
  ) {
    return true
  }
  return element.getAttribute('aria-disabled') === 'true'
}

function isChecked(element: HTMLElement): boolean {
  if ('checked' in element && (element as HTMLInputElement).checked === true) {
    return true
  }
  return element.getAttribute('aria-checked') === 'true'
}

function expectDisabled(element: HTMLElement) {
  expect(isDisabled(element)).toBe(true)
}

function expectEnabled(element: HTMLElement) {
  expect(isDisabled(element)).toBe(false)
}
