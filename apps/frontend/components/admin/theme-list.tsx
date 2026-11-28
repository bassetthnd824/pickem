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
  EMPTY_THEME_CRITERIA,
  parseThemes,
  themeCriteriaFromQuery,
  themeSearchQuery,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'themes'

export function ThemeList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_THEME_CRITERIA,
    themeCriteriaFromQuery,
  )
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseThemes(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready) {
    return <p>Loading themes.</p>
  }

  return (
    <AdminList
      addHref='/themes/new'
      columns={['Name', 'Path', 'Active', 'Last Updated On', 'Last Updated By']}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((theme) => ({
        id: theme.id,
        cells: [
          theme.themeName,
          theme.themePath,
          theme.active ? 'Yes' : 'No',
          formatAudit(theme.lastUpdateDate),
          theme.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/themes/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(themeSearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='theme-search-name'
            name='themeName'
            label='Name'
            value={criteria.themeName}
            onChange={(themeName) => {
              setCriteria((current) => ({ ...current, themeName }))
            }}
          />
          <TextField
            id='theme-search-path'
            name='themePath'
            label='Path'
            value={criteria.themePath}
            onChange={(themePath) => {
              setCriteria((current) => ({ ...current, themePath }))
            }}
          />
        </>
      }
    />
  )
}
