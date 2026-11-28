'use client'

import { useQuery } from '@tanstack/react-query'

import { AdminList } from '@/components/admin/admin-list'
import {
  useCatalogSearch,
  useSyncedCriteria,
} from '@/components/admin/use-catalog-search'
import { DateField } from '@/components/date-field'
import { TextField } from '@/components/ui/text-field'
import { formatAudit } from '@/lib/admin/audit'
import { apiProblem } from '@/lib/admin/problem-detail'
import { managerCollection, withQuery } from '@/lib/admin/query-string'
import {
  EMPTY_SEASON_CRITERIA,
  parseSeasons,
  seasonCriteriaFromQuery,
  seasonSearchQuery,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'seasons'

export function SeasonList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_SEASON_CRITERIA,
    seasonCriteriaFromQuery,
  )
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeasons(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready) {
    return <p>Loading seasons.</p>
  }

  return (
    <AdminList
      addHref='/seasons/new'
      columns={[
        'Season',
        'Begin Date',
        'End Date',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((season) => ({
        id: season.id,
        cells: [
          season.season,
          season.beginDate,
          season.endDate,
          formatAudit(season.lastUpdateDate),
          season.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/seasons/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(seasonSearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='season-search-season'
            name='season'
            label='Season'
            value={criteria.season}
            onChange={(season) => {
              setCriteria((current) => ({ ...current, season }))
            }}
          />
          <DateField
            id='season-search-begin'
            name='beginDate'
            label='Begin Date'
            value={criteria.beginDate}
            onChange={(beginDate) => {
              setCriteria((current) => ({ ...current, beginDate }))
            }}
          />
          <DateField
            id='season-search-end'
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
