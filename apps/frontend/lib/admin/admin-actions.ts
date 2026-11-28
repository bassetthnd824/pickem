import {
  apiProblem,
  assignFieldErrors,
  type FieldErrors,
} from '@/lib/admin/problem-detail'
import { managerCollection, managerItem } from '@/lib/admin/query-string'
import { api } from '@/lib/api-client'

export type AdminFailure = {
  ok: false
  fieldErrors: FieldErrors
  formError?: string
}

export type AdminSuccess = { ok: true }

export async function saveAdmin(
  resource: string,
  id: string | undefined,
  body: unknown,
  fields: readonly string[],
): Promise<AdminSuccess | AdminFailure> {
  try {
    if (id) {
      await api.put(managerItem(resource, id), body)
    } else {
      await api.post(managerCollection(resource), body)
    }
    return { ok: true }
  } catch (error) {
    return { ok: false, ...assignFieldErrors(apiProblem(error), fields) }
  }
}

export async function deleteAdmin(
  resource: string,
  id: string,
): Promise<AdminSuccess | AdminFailure> {
  try {
    await api.delete(managerItem(resource, id))
    return { ok: true }
  } catch (error) {
    return {
      ok: false,
      fieldErrors: {},
      formError: apiProblem(error),
    }
  }
}
