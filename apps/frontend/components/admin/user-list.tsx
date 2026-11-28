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
  EMPTY_USER_CRITERIA,
  parseThemes,
  parseUsers,
  userCriteriaFromQuery,
  userSearchQuery,
} from '@/lib/admin/records'
import { api } from '@/lib/api-client'

const RESOURCE = 'users'

export function UserList() {
  const { ready, activeQuery, publish } = useCatalogSearch(RESOURCE)
  const [criteria, setCriteria] = useSyncedCriteria(
    ready,
    activeQuery,
    EMPTY_USER_CRITERIA,
    userCriteriaFromQuery,
  )
  const themes = useQuery({
    queryKey: ['manager', 'themes', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseThemes(
        await api.get<unknown>(managerCollection('themes'), { signal }),
      ),
  })
  const results = useQuery({
    queryKey: ['manager', RESOURCE, 'search', activeQuery],
    enabled: ready && activeQuery !== null,
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseUsers(
        await api.get<unknown>(
          withQuery(managerCollection(RESOURCE), activeQuery ?? ''),
          { signal },
        ),
      ),
  })

  if (!ready || themes.isPending) {
    return <p>Loading users.</p>
  }

  if (!themes.data) {
    return <p role='alert'>Could not load users.</p>
  }

  const themeName = new Map(
    themes.data.map((theme) => [theme.themePath, theme.themeName]),
  )

  return (
    <AdminList
      columns={[
        'Email Address',
        'First Name',
        'Last Name',
        'Nickname',
        'Theme',
        'Last Updated On',
        'Last Updated By',
      ]}
      pending={results.isLoading}
      searched={activeQuery !== null}
      error={results.isError ? apiProblem(results.error) : undefined}
      rows={(results.data ?? []).map((user) => ({
        id: user.id,
        cells: [
          user.emailAddr,
          user.firstName,
          user.lastName,
          user.nickName,
          themeName.get(user.themeId) ?? user.themeId,
          formatAudit(user.lastUpdateDate),
          user.lastUpdateUser ?? '',
        ],
      }))}
      editHref={(id) => `/users/${encodeURIComponent(id)}`}
      onSearch={() => {
        publish(userSearchQuery(criteria))
      }}
      searchFields={
        <>
          <TextField
            id='user-search-email'
            name='emailAddr'
            label='Email Address'
            value={criteria.emailAddr}
            onChange={(emailAddr) => {
              setCriteria((current) => ({ ...current, emailAddr }))
            }}
          />
          <TextField
            id='user-search-first'
            name='firstName'
            label='First Name'
            value={criteria.firstName}
            onChange={(firstName) => {
              setCriteria((current) => ({ ...current, firstName }))
            }}
          />
          <TextField
            id='user-search-last'
            name='lastName'
            label='Last Name'
            value={criteria.lastName}
            onChange={(lastName) => {
              setCriteria((current) => ({ ...current, lastName }))
            }}
          />
        </>
      }
    />
  )
}
