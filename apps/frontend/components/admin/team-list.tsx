'use client'

import { useQuery } from '@tanstack/react-query'

import { AdminList } from '@/components/admin/admin-list'
import {
  useCatalogSearch,
  useSyncedCriteria,
} from '@/components/admin/use-catalog-search'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { formatAudit } from '@/lib/admin/audit'
import { apiProblem } from '@/lib/admin/problem-detail'
import { managerCollection, withQuery } from '@/lib/admin/query-string'
import {
  EMPTY_TEAM_CRITERIA,
  parseTeams,
  parseVenues,
  teamCriteriaFromQuery,
  teamSearchQuery,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'teams'

export function TeamList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_TEAM_CRITERIA,
    teamCriteriaFromQuery,
  )
  const venues = useQuery({
    queryKey: ['manager', 'venues', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenues(
        await api.get<unknown>(managerCollection('venues'), { signal }),
      ),
  })
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeams(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready || venues.isPending) {
    return <p>Loading teams.</p>
  }

  if (!venues.data) {
    return <p role='alert'>Could not load venues.</p>
  }

  return (
    <AdminList
      addHref='/teams/new'
      columns={[
        'Name',
        'Squad Name',
        'Conference Member',
        'Home Venue',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((team) => ({
        id: team.id,
        cells: [
          team.teamName,
          team.squadName,
          team.conferenceMember ? 'Yes' : 'No',
          team.homeVenue?.name ?? '',
          formatAudit(team.lastUpdateDate),
          team.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/teams/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(teamSearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='team-search-name'
            name='teamName'
            label='Name'
            value={criteria.teamName}
            onChange={(teamName) => {
              setCriteria((current) => ({ ...current, teamName }))
            }}
          />
          <TextField
            id='team-search-squad'
            name='squadName'
            label='Squad Name'
            value={criteria.squadName}
            onChange={(squadName) => {
              setCriteria((current) => ({ ...current, squadName }))
            }}
          />
          <SelectField
            name='homeVenueId'
            label='Home Venue'
            emptyLabel='Please select a Home Venue'
            value={criteria.homeVenueId}
            options={venues.data.map((venue) => ({
              value: venue.id,
              label: venue.venueName,
            }))}
            onChange={(homeVenueId) => {
              setCriteria((current) => ({ ...current, homeVenueId }))
            }}
          />
        </>
      }
    />
  )
}
