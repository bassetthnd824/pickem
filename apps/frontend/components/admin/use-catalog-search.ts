'use client'

import { useQueryClient } from '@tanstack/react-query'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import {
  useState,
  useSyncExternalStore,
  type Dispatch,
  type SetStateAction,
} from 'react'

import {
  recallSearch,
  rememberSearch,
  resumeHref,
  subscribeSearch,
} from '@/lib/admin/search-memory'

export function useCatalogSearch(resource: string) {
  const searchParams = useSearchParams()
  const pathname = usePathname()
  const router = useRouter()
  const queryClient = useQueryClient()
  const resume = searchParams.get('resume') === '1'
  const stored = useSyncExternalStore(
    subscribeSearch,
    () => recallSearch(resource),
    () => undefined as string | null | undefined,
  )
  const ready = stored !== undefined
  const [published, setPublished] = useState<string | null>(null)
  const [trackedResume, setTrackedResume] = useState(resume)

  if (trackedResume !== resume) {
    setTrackedResume(resume)
    if (!resume) {
      setPublished(null)
    }
  }

  const activeQuery =
    published !== null
      ? published
      : resume && ready && stored != null
        ? stored
        : null

  function publish(query: string) {
    rememberSearch(resource, query)
    setPublished(query)
    void queryClient.invalidateQueries({
      queryKey: ['manager', resource, 'search', query],
    })
    if (!resume) {
      router.replace(resumeHref(pathname), { scroll: false })
    }
  }

  return { ready, activeQuery, publish }
}

export function useSyncedCriteria<T>(
  ready: boolean,
  activeQuery: string | null,
  empty: T,
  fromQuery: (query: string) => T,
): [T, Dispatch<SetStateAction<T>>] {
  const [criteria, setCriteria] = useState(empty)
  const [seen, setSeen] = useState<string | null | undefined>(undefined)
  const target = ready ? activeQuery : undefined

  if (target !== seen) {
    setSeen(target)
    if (target !== undefined) {
      setCriteria(target === null ? empty : fromQuery(target))
    }
  }

  return [criteria, setCriteria]
}
