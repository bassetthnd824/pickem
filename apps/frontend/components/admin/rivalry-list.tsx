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
  EMPTY_RIVALRY_CRITERIA,
  parseRivalries,
  parseTeams,
  rivalryCriteriaFromQuery,
  rivalrySearchQuery,
  teamOptionLabel,
  type TeamRecord,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'rivalries'

function teamLabel(
  teams: TeamRecord[] | undefined,
  teamId: string,
  fallback: string | undefined,
) {
  const team = teams?.find((item) => item.id === teamId)
  if (team) {
    return teamOptionLabel(team)
  }
  return fallback ?? teamId
}

export function RivalryList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_RIVALRY_CRITERIA,
    rivalryCriteriaFromQuery,
  )
  const teams = useQuery({
    queryKey: ['manager', 'teams', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeams(
        await api.get<unknown>(managerCollection('teams'), { signal }),
      ),
  })
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseRivalries(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready || teams.isPending) {
    return <p>Loading rivalries.</p>
  }

  if (!teams.data) {
    return <p role='alert'>Could not load teams.</p>
  }

  return (
    <AdminList
      addHref='/rivalries/new'
      columns={[
        'Name',
        'Team 1',
        'Team 2',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((rivalry) => ({
        id: rivalry.id,
        cells: [
          rivalry.rivalryName,
          teamLabel(teams.data, rivalry.team1Id, rivalry.team1?.name),
          teamLabel(teams.data, rivalry.team2Id, rivalry.team2?.name),
          formatAudit(rivalry.lastUpdateDate),
          rivalry.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/rivalries/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(rivalrySearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='rivalry-search-name'
            name='rivalryName'
            label='Name'
            value={criteria.rivalryName}
            onChange={(rivalryName) => {
              setCriteria((current) => ({ ...current, rivalryName }))
            }}
          />
          <SelectField
            name='team1Id'
            label='Team'
            emptyLabel='Please select a Team'
            value={criteria.team1Id}
            options={teams.data.map((team) => ({
              value: team.id,
              label: teamOptionLabel(team),
            }))}
            onChange={(team1Id) => {
              setCriteria((current) => ({ ...current, team1Id }))
            }}
          />
        </>
      }
    />
  )
}
