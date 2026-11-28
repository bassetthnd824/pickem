const PREFIX = 'pickem:admin-search:'

const listeners = new Set<() => void>()

const fallback = new Map<string, string>()

function emitSearch() {
  for (const listener of listeners) {
    listener()
  }
}

export function subscribeSearch(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function searchStorageKey(resource: string): string {
  return `${PREFIX}${resource}`
}

export function resumeHref(path: string): string {
  return `${path}?resume=1`
}

export function rememberSearch(resource: string, query: string): void {
  try {
    sessionStorage.setItem(searchStorageKey(resource), query)
    fallback.delete(resource)
  } catch {
    // Private mode can reject storage. The in-memory copy still feeds this tab.
    fallback.set(resource, query)
  }
  emitSearch()
}

export function recallSearch(resource: string): string | null {
  const cached = fallback.get(resource)
  if (cached !== undefined) {
    return cached
  }
  try {
    return sessionStorage.getItem(searchStorageKey(resource))
  } catch {
    return null
  }
}
