import { fireEvent, render, screen, waitFor } from '@testing-library/react'

import { QueryProvider } from '@/components/query-provider'
import { TeamSchedule } from '@/components/team-schedule'
import {
  parseConferenceTeams,
  parseTeamSchedule,
  teamOptionLabel,
} from '@/lib/team-schedule'

const teams = [
  { id: 'georgia', teamName: 'Georgia', squadName: 'Bulldogs' },
  { id: 'alabama', teamName: 'Alabama', squadName: 'Crimson Tide' },
]

const georgiaSchedule = [
  {
    matchupDate: '2026-09-05',
    opponentName: 'at Alabama',
    scoreResult: '31 - 14',
    winLoss: 'L',
  },
  {
    matchupDate: '2026-09-12',
    opponentName: 'Tennessee',
  },
]

function renderSchedule() {
  return render(
    <QueryProvider>
      <TeamSchedule />
    </QueryProvider>,
  )
}

function stubApi(
  scheduleByTeam: Record<string, unknown> = { georgia: georgiaSchedule },
) {
  const calls: string[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      calls.push(url)
      if (url === '/api/game/teams') {
        return Response.json(teams)
      }
      const teamId = new URL(url, 'http://pickem.local').searchParams.get(
        'teamId',
      )
      const schedule = teamId == null ? undefined : scheduleByTeam[teamId]
      if (schedule === undefined) {
        return new Response('missing', { status: 404 })
      }
      return Response.json(schedule)
    }),
  )
  return calls
}

async function chooseTeam(label: string) {
  fireEvent.click(screen.getByRole('combobox', { name: 'Team' }))
  const option = await screen.findByRole('option', { name: label })
  fireEvent.pointerDown(option, { pointerType: 'mouse', button: 0 })
  fireEvent.click(option)
  await waitFor(() => {
    expect(
      screen
        .getByRole('combobox', { name: 'Team' })
        .getAttribute('aria-expanded'),
    ).toBe('false')
  })
}

describe('team schedule parsers', () => {
  it('labels a conference team as team name then squad name', () => {
    expect(teamOptionLabel(teams[0])).toBe('Georgia Bulldogs')
    expect(parseConferenceTeams(teams)).toEqual(teams)
  })

  it('keeps the away prefix and omits unplayed score and result', () => {
    expect(parseTeamSchedule(georgiaSchedule)).toEqual([
      {
        matchupDate: '2026-09-05',
        opponentName: 'at Alabama',
        scoreResult: '31 - 14',
        winLoss: 'L',
      },
      {
        matchupDate: '2026-09-12',
        opponentName: 'Tennessee',
        scoreResult: '',
        winLoss: '',
      },
    ])
  })

  it('rejects a payload that is not the schedule shape', () => {
    expect(() => parseConferenceTeams(null)).toThrow(
      'Team list response was invalid',
    )
    expect(() =>
      parseConferenceTeams([
        { id: ' ', teamName: 'Georgia', squadName: 'Bulldogs' },
      ]),
    ).toThrow('Team list response was invalid')
    expect(() => parseTeamSchedule(null)).toThrow(
      'Team schedule response was invalid',
    )
    expect(() =>
      parseTeamSchedule([
        {
          matchupDate: '2026-09-05',
          opponentName: 'at Alabama',
          winLoss: 'T',
        },
      ]),
    ).toThrow('Team schedule response was invalid')
  })
})

describe('TeamSchedule', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists conference teams and shows no rows until one is selected', async () => {
    const calls = stubApi()
    renderSchedule()

    expect(screen.getByText('Loading teams.')).toBeTruthy()
    expect(await screen.findByRole('combobox', { name: 'Team' })).toBeTruthy()
    fireEvent.click(screen.getByRole('combobox', { name: 'Team' }))
    expect(
      await screen.findByRole('option', { name: 'Georgia Bulldogs' }),
    ).toBeTruthy()
    expect(
      screen.getByRole('option', { name: 'Alabama Crimson Tide' }),
    ).toBeTruthy()
    expect(screen.queryByRole('table')).toBeNull()
    expect(calls).toEqual(['/api/game/teams'])
  })

  it('loads date, opponent, score, and W/L without navigating', async () => {
    const calls = stubApi()
    const href = window.location.href
    renderSchedule()
    await screen.findByRole('combobox', { name: 'Team' })

    await chooseTeam('Georgia Bulldogs')

    expect(
      await screen.findByRole('columnheader', { name: 'Date' }),
    ).toBeTruthy()
    expect(screen.getByRole('columnheader', { name: 'Opponent' })).toBeTruthy()
    expect(screen.getByRole('columnheader', { name: 'Score' })).toBeTruthy()
    expect(screen.getByRole('columnheader', { name: 'Result' })).toBeTruthy()
    const rows = screen.getAllByRole('row').slice(1)
    expect(rows.map((row) => row.textContent)).toEqual([
      '2026-09-05at Alabama31 - 14L',
      '2026-09-12Tennessee',
    ])
    expect(calls).toEqual([
      '/api/game/teams',
      '/api/game/team-schedule?teamId=georgia',
    ])
    expect(window.location.href).toBe(href)
  })

  it('clears the rows when the selection returns to empty', async () => {
    const calls = stubApi()
    renderSchedule()
    await screen.findByRole('combobox', { name: 'Team' })
    await chooseTeam('Georgia Bulldogs')
    expect(await screen.findByRole('table')).toBeTruthy()

    await chooseTeam('Please select a Team')

    await waitFor(() => {
      expect(screen.queryByRole('table')).toBeNull()
    })
    expect(calls.filter((url) => url.includes('team-schedule'))).toEqual([
      '/api/game/team-schedule?teamId=georgia',
    ])
  })

  it('reports a failed team list and a failed schedule', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('missing', { status: 404 })),
    )
    const { unmount } = renderSchedule()
    expect((await screen.findByRole('alert')).textContent).toBe(
      'Could not load teams.',
    )
    unmount()

    stubApi({})
    renderSchedule()
    await screen.findByRole('combobox', { name: 'Team' })
    await chooseTeam('Georgia Bulldogs')
    expect((await screen.findByRole('alert')).textContent).toBe(
      'Could not load the schedule.',
    )
  })
})
