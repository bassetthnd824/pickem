'use client'

import { Field } from '@base-ui/react/field'
import { Select } from '@base-ui/react/select'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'

import { api } from '@/lib/api-client'
import {
  parseConferenceTeams,
  parseTeamSchedule,
  teamOptionLabel,
  type ConferenceTeam,
} from '@/lib/team-schedule'

const TEAMS_KEY = ['game', 'teams'] as const
const EMPTY_LABEL = 'Please select a Team'

const itemClass =
  'cursor-pointer px-3 py-2 text-sm outline-hidden data-highlighted:bg-primary data-highlighted:text-primary-fg'

async function loadTeams(signal: AbortSignal) {
  return parseConferenceTeams(
    await api.get<unknown>('/api/game/teams', { signal }),
  )
}

async function loadSchedule(teamId: string, signal: AbortSignal) {
  return parseTeamSchedule(
    await api.get<unknown>(
      `/api/game/team-schedule?teamId=${encodeURIComponent(teamId)}`,
      { signal },
    ),
  )
}

export function TeamSchedule() {
  const [teamId, setTeamId] = useState<string | null>(null)
  const teamsQuery = useQuery({
    queryKey: TEAMS_KEY,
    queryFn: ({ signal }) => loadTeams(signal),
  })
  const scheduleQuery = useQuery({
    queryKey: ['game', 'team-schedule', teamId],
    enabled: teamId != null,
    queryFn: ({ signal }) => {
      if (teamId == null) {
        throw new Error('A team is required')
      }
      return loadSchedule(teamId, signal)
    },
  })

  if (teamsQuery.isPending) {
    return <p>Loading teams.</p>
  }

  if (!teamsQuery.data) {
    return <p role='alert'>Could not load teams.</p>
  }

  const teams = teamsQuery.data

  return (
    <div className='flex flex-col gap-4'>
      <TeamSelect teams={teams} teamId={teamId} onTeamIdChange={setTeamId} />
      {teamId == null ? null : (
        <SchedulePanel
          pending={scheduleQuery.isPending}
          games={scheduleQuery.data}
        />
      )}
    </div>
  )
}

function TeamSelect({
  teams,
  teamId,
  onTeamIdChange,
}: {
  teams: ConferenceTeam[]
  teamId: string | null
  onTeamIdChange: (teamId: string | null) => void
}) {
  const items = [
    { value: null, label: EMPTY_LABEL },
    ...teams.map((team) => ({
      value: team.id,
      label: teamOptionLabel(team),
    })),
  ]

  return (
    <Field.Root name='teamId' className='flex max-w-md flex-col gap-1'>
      <Select.Root
        items={items}
        value={teamId}
        onValueChange={(value) => {
          onTeamIdChange(value)
        }}
      >
        <Field.Label className='text-sm font-medium text-text'>
          Team
        </Field.Label>
        <Select.Trigger className='flex w-full cursor-pointer items-center justify-between rounded border border-border bg-surface px-2 py-1 text-left text-text'>
          <Select.Value placeholder={EMPTY_LABEL} />
          <Select.Icon className='ml-2 text-muted' aria-hidden>
            ▾
          </Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Positioner
            className='z-10 outline-none'
            side='bottom'
            sideOffset={4}
            align='start'
            alignItemWithTrigger={false}
          >
            <Select.Popup className='max-h-72 w-(--anchor-width) origin-[var(--transform-origin)] overflow-y-auto rounded-md border border-border bg-surface py-1 text-text outline-none'>
              <Select.List>
                <Select.Item value={null} className={itemClass}>
                  <Select.ItemText>{EMPTY_LABEL}</Select.ItemText>
                </Select.Item>
                {teams.map((team) => (
                  <Select.Item
                    key={team.id}
                    value={team.id}
                    className={itemClass}
                  >
                    <Select.ItemText>{teamOptionLabel(team)}</Select.ItemText>
                  </Select.Item>
                ))}
              </Select.List>
            </Select.Popup>
          </Select.Positioner>
        </Select.Portal>
      </Select.Root>
    </Field.Root>
  )
}

function SchedulePanel({
  pending,
  games,
}: {
  pending: boolean
  games: ReturnType<typeof parseTeamSchedule> | undefined
}) {
  if (pending) {
    return <p>Loading schedule.</p>
  }

  if (!games) {
    return <p role='alert'>Could not load the schedule.</p>
  }

  return (
    <div className='overflow-x-auto'>
      <table className='w-full border-collapse text-left text-sm text-text'>
        <thead>
          <tr className='border-b border-border'>
            <th scope='col' className='px-2 py-2 font-medium'>
              Date
            </th>
            <th scope='col' className='px-2 py-2 font-medium'>
              Opponent
            </th>
            <th scope='col' className='px-2 py-2 font-medium'>
              Score
            </th>
            <th scope='col' className='px-2 py-2 font-medium'>
              Result
            </th>
          </tr>
        </thead>
        <tbody>
          {games.map((game, index) => (
            <tr
              key={`${game.matchupDate}-${game.opponentName}-${index}`}
              className='border-b border-border'
            >
              <td className='px-2 py-2'>{game.matchupDate}</td>
              <td className='px-2 py-2'>{game.opponentName}</td>
              <td className='px-2 py-2 tabular-nums'>{game.scoreResult}</td>
              <td className='px-2 py-2'>{game.winLoss}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
