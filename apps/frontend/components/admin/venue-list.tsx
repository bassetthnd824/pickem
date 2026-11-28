'use client'

import { useQuery } from '@tanstack/react-query'

import { AdminList } from '@/components/admin/admin-list'
import {
  useCatalogSearch,
  useSyncedCriteria,
} from '@/components/admin/use-catalog-search'
import { TextField } from '@/components/ui/text-field'
import { formatAudit } from '@/lib/admin/audit'
import { apiProblem } from '@/lib/admin/problem-detail'
import { managerCollection, withQuery } from '@/lib/admin/query-string'
import {
  EMPTY_VENUE_CRITERIA,
  parseVenues,
  venueCriteriaFromQuery,
  venueSearchQuery,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'venues'

export function VenueList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_VENUE_CRITERIA,
    venueCriteriaFromQuery,
  )
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenues(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready) {
    return <p>Loading venues.</p>
  }

  return (
    <AdminList
      addHref='/venues/new'
      columns={['Name', 'City/State', 'Last Updated On', 'Last Updated By']}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((venue) => ({
        id: venue.id,
        cells: [
          venue.venueName,
          venue.cityState,
          formatAudit(venue.lastUpdateDate),
          venue.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/venues/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(venueSearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='venue-search-name'
            name='venueName'
            label='Name'
            value={criteria.venueName}
            onChange={(venueName) => {
              setCriteria((current) => ({ ...current, venueName }))
            }}
          />
          <TextField
            id='venue-search-city'
            name='cityState'
            label='City/State'
            value={criteria.cityState}
            onChange={(cityState) => {
              setCriteria((current) => ({ ...current, cityState }))
            }}
          />
        </>
      }
    />
  )
}
