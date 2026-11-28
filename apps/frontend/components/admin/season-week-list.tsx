'use client'

import { useQuery } from '@tanstack/react-query'

import { AdminList } from '@/components/admin/admin-list'
import {
  useCatalogSearch,
  useSyncedCriteria,
} from '@/components/admin/use-catalog-search'
import { DateField } from '@/components/date-field'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { formatAudit } from '@/lib/admin/audit'
import { apiProblem } from '@/lib/admin/problem-detail'
import { managerCollection, withQuery } from '@/lib/admin/query-string'
import {
  EMPTY_SEASON_WEEK_CRITERIA,
  parseSeasonWeeks,
  parseSeasons,
  seasonWeekCriteriaFromQuery,
  seasonWeekSearchQuery,
  type SeasonRecord,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'season-weeks'

function seasonLabel(seasons: SeasonRecord[] | undefined, seasonId: string) {
  return seasons?.find((season) => season.id === seasonId)?.season ?? seasonId
}

export function SeasonWeekList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_SEASON_WEEK_CRITERIA,
    seasonWeekCriteriaFromQuery,
  )
  const seasons = useQuery({
    queryKey: ['manager', 'seasons', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasons(
        await api.get<unknown>(managerCollection('seasons'), { signal }),
      ),
  })
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasonWeeks(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready || seasons.isPending) {
    return <p>Loading season weeks.</p>
  }

  if (!seasons.data) {
    return <p role='alert'>Could not load seasons.</p>
  }

  return (
    <AdminList
      addHref='/season-weeks/new'
      columns={[
        'Season',
        'Week Number',
        'Begin Date',
        'End Date',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((week) => ({
        id: week.id,
        cells: [
          seasonLabel(seasons.data, week.seasonId),
          String(week.weekNumber),
          week.beginDate,
          week.endDate,
          formatAudit(week.lastUpdateDate),
          week.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/season-weeks/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(seasonWeekSearchQuery(criteria))
      }}
      searchFields={
        <>
          <SelectField
            name='seasonId'
            label='Season'
            emptyLabel='Please select a Season'
            value={criteria.seasonId}
            options={seasons.data.map((season) => ({
              value: season.id,
              label: season.season,
            }))}
            onChange={(seasonId) => {
              setCriteria((current) => ({ ...current, seasonId }))
            }}
          />
          <TextField
            id='week-search-number'
            name='weekNumber'
            label='Week Number'
            value={criteria.weekNumber}
            onChange={(weekNumber) => {
              setCriteria((current) => ({ ...current, weekNumber }))
            }}
          />
          <DateField
            id='week-search-begin'
            name='beginDate'
            label='Begin Date'
            value={criteria.beginDate}
            onChange={(beginDate) => {
              setCriteria((current) => ({ ...current, beginDate }))
            }}
          />
          <DateField
            id='week-search-end'
            name='endDate'
            label='End Date'
            value={criteria.endDate}
            onChange={(endDate) => {
              setCriteria((current) => ({ ...current, endDate }))
            }}
          />
        </>
      }
    />
  )
}
