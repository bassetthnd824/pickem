export function buildQuery(entries: Record<string, string>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(entries)) {
    const trimmed = value.trim()
    if (trimmed !== '') {
      params.set(key, trimmed)
    }
  }
  return params.toString()
}

export function readQuery<K extends string>(
  query: string,
  keys: readonly K[],
): Record<K, string> {
  const params = new URLSearchParams(query)
  const result = {} as Record<K, string>
  for (const key of keys) {
    result[key] = params.get(key) ?? ''
  }
  return result
}

export function withQuery(path: string, query: string): string {
  return query === '' ? path : `${path}?${query}`
}

export function managerCollection(resource: string): string {
  return `/api/manager/${resource}`
}

export function managerItem(resource: string, id: string): string {
  return `/api/manager/${resource}/${encodeURIComponent(id)}`
}
