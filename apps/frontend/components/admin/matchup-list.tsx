'use client'

import { useQuery } from '@tanstack/react-query'

import { AdminList } from '@/components/admin/admin-list'
import {
  useCatalogSearch,
  useSyncedCriteria,
} from '@/components/admin/use-catalog-search'
import { DateField } from '@/components/date-field'
import { SelectField } from '@/components/ui/select-field'
import { formatAudit } from '@/lib/admin/audit'
import { apiProblem } from '@/lib/admin/problem-detail'
import {
  buildQuery,
  managerCollection,
  withQuery,
} from '@/lib/admin/query-string'
import {
  EMPTY_MATCHUP_CRITERIA,
  matchupCriteriaFromQuery,
  matchupSearchQuery,
  parseMatchups,
  parseSeasons,
  parseSeasonWeeks,
  parseTeams,
  parseVenues,
  squadLabel,
  teamOptionLabel,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'matchups'

function scoreText(score: number | null): string {
  return score == null ? '' : String(score)
}

export function MatchupList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_MATCHUP_CRITERIA,
    matchupCriteriaFromQuery,
  )
  const seasons = useQuery({
    queryKey: ['manager', 'seasons', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasons(
        await api.get<unknown>(managerCollection('seasons'), { signal }),
      ),
  })
  const teams = useQuery({
    queryKey: ['manager', 'teams', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeams(
        await api.get<unknown>(managerCollection('teams'), { signal }),
      ),
  })
  const venues = useQuery({
    queryKey: ['manager', 'venues', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenues(
        await api.get<unknown>(managerCollection('venues'), { signal }),
      ),
  })
  const weeks = useQuery({
    queryKey: ['manager', 'season-weeks', 'by-season', criteria.seasonId],
    enabled: criteria.seasonId !== '',
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasonWeeks(
        await api.get<unknown>(
          withQuery(
            managerCollection('season-weeks'),
            buildQuery({ seasonId: criteria.seasonId }),
          ),
          { signal },
        ),
      ),
  })
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseMatchups(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready || seasons.isPending || teams.isPending || venues.isPending) {
    return <p>Loading matchups.</p>
  }

  if (!seasons.data || !teams.data || !venues.data) {
    return <p role='alert'>Could not load matchups.</p>
  }

  const seasonName = new Map(
    seasons.data.map((season) => [season.id, season.season]),
  )
  const weekOptions = [...(weeks.data ?? [])].sort(
    (left, right) => left.weekNumber - right.weekNumber,
  )

  return (
    <AdminList
      addHref='/matchups/new'
      columns={[
        'Season',
        'Week Number',
        'Date',
        'Home Team',
        'Home Team Score',
        'Away Team',
        'Away Team Score',
        'Venue',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={
        weeks.isError
          ? apiProblem(weeks.error)
          : results.isError
            ? apiProblem(results.error)
            : undefined
      }
      rows={(results.data ?? []).map((matchup) => ({
        id: matchup.id,
        cells: [
          seasonName.get(matchup.seasonId) ?? matchup.seasonId,
          matchup.weekNumber == null ? '' : String(matchup.weekNumber),
          matchup.matchupDate,
          squadLabel(matchup.homeTeam, matchup.homeTeamId),
          scoreText(matchup.homeTeamScore),
          squadLabel(matchup.awayTeam, matchup.awayTeamId),
          scoreText(matchup.awayTeamScore),
          matchup.venue?.name ?? '',
          formatAudit(matchup.lastUpdateDate),
          matchup.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/matchups/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(matchupSearchQuery(criteria))
      }}
      searchFields={
        <>
          <SelectField
            name='seasonId'
            label='Season'
            emptyLabel='Please select a Season'
            value={criteria.seasonId}
            options={[...seasons.data]
              .sort((left, right) => left.season.localeCompare(right.season))
              .map((season) => ({
                value: season.id,
                label: season.season,
              }))}
            onChange={(seasonId) => {
              setCriteria((current) => ({
                ...current,
                seasonId,
                seasonWeekId: '',
              }))
            }}
          />
          <SelectField
            name='seasonWeekId'
            label='Week Number'
            emptyLabel='Please select a Week Number'
            value={criteria.seasonWeekId}
            disabled={criteria.seasonId === '' || weeks.isFetching}
            options={weekOptions.map((week) => ({
              value: week.id,
              label: String(week.weekNumber),
            }))}
            onChange={(seasonWeekId) => {
              setCriteria((current) => ({ ...current, seasonWeekId }))
            }}
          />
          <DateField
            id='matchup-search-date'
            name='matchupDate'
            label='Date'
            value={criteria.matchupDate}
            onChange={(matchupDate) => {
              setCriteria((current) => ({ ...current, matchupDate }))
            }}
          />
          <SelectField
            name='teamId'
            label='Team'
            emptyLabel='Please select a Team'
            value={criteria.teamId}
            options={[...teams.data]
              .sort((left, right) =>
                teamOptionLabel(left).localeCompare(teamOptionLabel(right)),
              )
              .map((team) => ({
                value: team.id,
                label: teamOptionLabel(team),
              }))}
            onChange={(teamId) => {
              setCriteria((current) => ({ ...current, teamId }))
            }}
          />
          <SelectField
            name='venueId'
            label='Venue'
            emptyLabel='Please select a Venue'
            value={criteria.venueId}
            options={[...venues.data]
              .sort((left, right) =>
                left.venueName.localeCompare(right.venueName),
              )
              .map((venue) => ({
                value: venue.id,
                label: venue.venueName,
              }))}
            onChange={(venueId) => {
              setCriteria((current) => ({ ...current, venueId }))
            }}
          />
        </>
      }
    />
  )
}
