'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { DateField } from '@/components/date-field'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import { apiProblem, type FieldErrors } from '@/lib/admin/problem-detail'
import {
  buildQuery,
  managerCollection,
  managerItem,
  withQuery,
} from '@/lib/admin/query-string'
import {
  parseMatchup,
  parseSeasons,
  parseSeasonWeeks,
  parseTeam,
  parseTeams,
  parseVenues,
  teamOptionLabel,
  type MatchupRecord,
  type SeasonWeekRecord,
  type TeamRecord,
} from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { constrainMatchupDate } from '@/lib/admin/week-dates'
import { api } from '@/lib/api-client'

const RESOURCE = 'matchups'
const FIELDS = [
  'seasonId',
  'seasonWeekId',
  'matchupDate',
  'homeTeamId',
  'awayTeamId',
  'homeTeamScore',
  'awayTeamScore',
  'venueId',
]
const LIST = '/matchups'

type MatchupFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type MatchupDraft = {
  seasonId: string
  seasonWeekId: string
  matchupDate: string
  homeTeamId: string
  awayTeamId: string
  homeTeamScore: string
  awayTeamScore: string
  venueId: string
}

const EMPTY: MatchupDraft = {
  seasonId: '',
  seasonWeekId: '',
  matchupDate: '',
  homeTeamId: '',
  awayTeamId: '',
  homeTeamScore: '',
  awayTeamScore: '',
  venueId: '',
}

function fromRecord(record: MatchupRecord): MatchupDraft {
  return {
    seasonId: record.seasonId,
    seasonWeekId: record.seasonWeekId,
    matchupDate: record.matchupDate,
    homeTeamId: record.homeTeamId,
    awayTeamId: record.awayTeamId,
    homeTeamScore: scoreText(record.homeTeamScore),
    awayTeamScore: scoreText(record.awayTeamScore),
    venueId: record.venueId,
  }
}

function scoreText(score: number | null): string {
  return score == null ? '' : String(score)
}

function homeVenueOf(team: TeamRecord): string {
  if (team.homeVenueId !== '') {
    return team.homeVenueId
  }
  return team.homeVenue?.id ?? ''
}

function parseScore(
  value: string,
): { ok: true; score: number | null } | { ok: false } {
  const trimmed = value.trim()
  if (trimmed === '') {
    return { ok: true, score: null }
  }
  if (!/^-?\d+$/.test(trimmed)) {
    return { ok: false }
  }
  const score = Number(trimmed)
  if (!Number.isSafeInteger(score)) {
    return { ok: false }
  }
  return { ok: true, score }
}

export function MatchupForm({ mode, id }: MatchupFormProps) {
  const router = useRouter()
  const seasonsQuery = useQuery({
    queryKey: ['manager', 'seasons', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasons(
        await api.get<unknown>(managerCollection('seasons'), { signal }),
      ),
  })
  const teamsQuery = useQuery({
    queryKey: ['manager', 'teams', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeams(
        await api.get<unknown>(managerCollection('teams'), { signal }),
      ),
  })
  const venuesQuery = useQuery({
    queryKey: ['manager', 'venues', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenues(
        await api.get<unknown>(managerCollection('venues'), { signal }),
      ),
  })
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseMatchup(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<MatchupDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [cfbdGameId, setCfbdGameId] = useState<number | null>(null)
  const [pendingVenueTeamId, setPendingVenueTeamId] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()
  const record = mode === 'edit' ? recordQuery.data : undefined
  const seasonId = draft?.seasonId ?? record?.seasonId ?? ''
  const weeksQuery = useQuery({
    queryKey: ['manager', 'season-weeks', 'by-season', seasonId],
    enabled: seasonId !== '',
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasonWeeks(
        await api.get<unknown>(
          withQuery(
            managerCollection('season-weeks'),
            buildQuery({ seasonId }),
          ),
          { signal },
        ),
      ),
  })
  const venueTeamQuery = useQuery({
    queryKey: ['manager', 'teams', 'venue', pendingVenueTeamId],
    enabled: pendingVenueTeamId !== '',
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeam(
        await api.get<unknown>(managerItem('teams', pendingVenueTeamId), {
          signal,
        }),
      ),
  })

  if (
    seasonsQuery.isPending ||
    teamsQuery.isPending ||
    venuesQuery.isPending ||
    (mode === 'edit' && recordQuery.isPending)
  ) {
    return <p>Loading matchup.</p>
  }

  if (
    !seasonsQuery.data ||
    !teamsQuery.data ||
    !venuesQuery.data ||
    (mode === 'edit' && !recordQuery.data)
  ) {
    return <p role='alert'>Could not load matchup.</p>
  }

  const loaded = recordQuery.data
  if (mode === 'edit' && loaded && draft === null) {
    if (version !== loaded.version) {
      setVersion(loaded.version)
    }
    if (cfbdGameId !== loaded.cfbdGameId) {
      setCfbdGameId(loaded.cfbdGameId)
    }
  }
  const values = draft ?? (loaded ? fromRecord(loaded) : EMPTY)
  const weeks = [...(weeksQuery.data ?? [])].sort(
    (left, right) => left.weekNumber - right.weekNumber,
  )
  const selectedWeek = weeks.find((week) => week.id === values.seasonWeekId)
  const pendingTeam = venueTeamQuery.data
  if (pendingVenueTeamId !== '' && pendingTeam?.id === pendingVenueTeamId) {
    const homeVenueId = homeVenueOf(pendingTeam)
    if (homeVenueId !== '' && values.venueId !== homeVenueId) {
      setDraft((current) => ({
        ...(current ?? values),
        venueId: homeVenueId,
      }))
    }
    setPendingVenueTeamId('')
  }

  function update(patch: Partial<MatchupDraft>) {
    setDraft((current) => ({ ...(current ?? values), ...patch }))
  }

  function chooseSeason(nextSeasonId: string) {
    update({ seasonId: nextSeasonId, seasonWeekId: '' })
  }

  function chooseWeek(seasonWeekId: string) {
    const week = weeks.find((item) => item.id === seasonWeekId)
    update({
      seasonWeekId,
      matchupDate: week
        ? constrainMatchupDate(values.matchupDate, week.beginDate, week.endDate)
        : values.matchupDate,
    })
  }

  function chooseHomeTeam(homeTeamId: string) {
    const team = teamsQuery.data?.find((item) => item.id === homeTeamId)
    const venueId = team ? homeVenueOf(team) : ''
    setPendingVenueTeamId(homeTeamId)
    update({
      homeTeamId,
      ...(venueId !== '' ? { venueId } : {}),
    })
  }

  function changeDate(matchupDate: string) {
    if (
      selectedWeek &&
      matchupDate !== '' &&
      (matchupDate < selectedWeek.beginDate ||
        matchupDate > selectedWeek.endDate)
    ) {
      return
    }
    update({ matchupDate })
  }

  async function save() {
    const homeScore = parseScore(values.homeTeamScore)
    const awayScore = parseScore(values.awayTeamScore)
    const scoreErrors: FieldErrors = {}
    if (!homeScore.ok) {
      scoreErrors.homeTeamScore = 'must be a whole number'
    }
    if (!awayScore.ok) {
      scoreErrors.awayTeamScore = 'must be a whole number'
    }
    if (!homeScore.ok || !awayScore.ok) {
      setFieldErrors(scoreErrors)
      setFormError(undefined)
      return
    }
    setSaving(true)
    setFieldErrors({})
    setFormError(undefined)
    const saved = await saveAdmin(
      RESOURCE,
      mode === 'edit' ? id : undefined,
      {
        seasonId: values.seasonId,
        seasonWeekId: values.seasonWeekId,
        matchupDate: values.matchupDate,
        homeTeamId: values.homeTeamId,
        awayTeamId: values.awayTeamId,
        homeTeamScore: homeScore.score,
        awayTeamScore: awayScore.score,
        venueId: values.venueId,
        cfbdGameId: mode === 'edit' ? cfbdGameId : null,
        ...(mode === 'edit' ? { version } : {}),
      },
      FIELDS,
    )
    setSaving(false)
    if (!saved.ok) {
      setFieldErrors(saved.fieldErrors)
      setFormError(saved.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  async function remove() {
    if (!id) {
      return
    }
    setDeleting(true)
    setFormError(undefined)
    const removed = await deleteAdmin(RESOURCE, id)
    setDeleting(false)
    if (!removed.ok) {
      setFormError(removed.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  const teamOptions = [...teamsQuery.data]
    .sort((left, right) =>
      teamOptionLabel(left).localeCompare(teamOptionLabel(right)),
    )
    .map((team) => ({
      value: team.id,
      label: teamOptionLabel(team),
    }))

  return (
    <AdminForm
      title='Matchup'
      mode={mode}
      saving={saving}
      deleting={deleting}
      formError={formError}
      cancelHref={resumeHref(LIST)}
      onSubmit={() => {
        void save()
      }}
      onDelete={() => {
        void remove()
      }}
      lastUpdateDate={loaded?.lastUpdateDate}
      lastUpdateUser={loaded?.lastUpdateUser}
    >
      <SelectField
        name='seasonId'
        label='Season'
        emptyLabel='Please select a Season'
        value={values.seasonId}
        error={fieldErrors.seasonId}
        options={[...seasonsQuery.data]
          .sort((left, right) => left.season.localeCompare(right.season))
          .map((season) => ({
            value: season.id,
            label: season.season,
          }))}
        onChange={chooseSeason}
      />
      <SelectField
        name='seasonWeekId'
        label='Week Number'
        emptyLabel='Please select a Week Number'
        value={values.seasonWeekId}
        error={
          weeksQuery.isError
            ? apiProblem(weeksQuery.error)
            : fieldErrors.seasonWeekId
        }
        disabled={
          values.seasonId === '' || weeksQuery.isFetching || weeksQuery.isError
        }
        options={weeks.map((week: SeasonWeekRecord) => ({
          value: week.id,
          label: String(week.weekNumber),
        }))}
        onChange={chooseWeek}
      />
      <DateField
        id='matchup-date'
        name='matchupDate'
        label='Date'
        value={values.matchupDate}
        error={fieldErrors.matchupDate}
        disabled={!selectedWeek}
        min={selectedWeek?.beginDate}
        max={selectedWeek?.endDate}
        onChange={changeDate}
      />
      <SelectField
        name='homeTeamId'
        label='Home Team'
        emptyLabel='Please select a Team'
        value={values.homeTeamId}
        error={fieldErrors.homeTeamId}
        options={teamOptions}
        onChange={chooseHomeTeam}
      />
      <TextField
        id='home-team-score'
        name='homeTeamScore'
        label='Home Team Score'
        value={values.homeTeamScore}
        error={fieldErrors.homeTeamScore}
        onChange={(homeTeamScore) => {
          update({ homeTeamScore })
        }}
      />
      <SelectField
        name='awayTeamId'
        label='Away Team'
        emptyLabel='Please select a Team'
        value={values.awayTeamId}
        error={fieldErrors.awayTeamId}
        options={teamOptions}
        onChange={(awayTeamId) => {
          update({ awayTeamId })
        }}
      />
      <TextField
        id='away-team-score'
        name='awayTeamScore'
        label='Away Team Score'
        value={values.awayTeamScore}
        error={fieldErrors.awayTeamScore}
        onChange={(awayTeamScore) => {
          update({ awayTeamScore })
        }}
      />
      <SelectField
        name='venueId'
        label='Venue'
        emptyLabel='Please select a Venue'
        value={values.venueId}
        error={fieldErrors.venueId}
        options={[...venuesQuery.data]
          .sort((left, right) => left.venueName.localeCompare(right.venueName))
          .map((venue) => ({
            value: venue.id,
            label: venue.venueName,
          }))}
        onChange={(venueId) => {
          setPendingVenueTeamId('')
          update({ venueId })
        }}
      />
    </AdminForm>
  )
}
