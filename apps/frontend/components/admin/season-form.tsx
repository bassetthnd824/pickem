'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { DateField } from '@/components/date-field'
import { CheckboxField } from '@/components/ui/checkbox-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerItem } from '@/lib/admin/query-string'
import { parseSeason, type SeasonRecord } from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'seasons'
const FIELDS = ['season', 'beginDate', 'endDate', 'current', 'isCurrent']
const LIST = '/seasons'

type SeasonFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type SeasonDraft = {
  season: string
  beginDate: string
  endDate: string
  current: boolean
}

const EMPTY: SeasonDraft = {
  season: '',
  beginDate: '',
  endDate: '',
  current: false,
}

function fromRecord(record: SeasonRecord): SeasonDraft {
  return {
    season: record.season,
    beginDate: record.beginDate,
    endDate: record.endDate,
    current: record.current,
  }
}

export function SeasonForm({ mode, id }: SeasonFormProps) {
  const router = useRouter()
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseSeason(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<SeasonDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (mode === 'edit' && recordQuery.isPending) {
    return <p>Loading season.</p>
  }

  if (mode === 'edit' && !recordQuery.data) {
    return <p role='alert'>Could not load season.</p>
  }

  const record = recordQuery.data
  if (
    mode === 'edit' &&
    record &&
    draft === null &&
    version !== record.version
  ) {
    setVersion(record.version)
  }
  const values = draft ?? (record ? fromRecord(record) : EMPTY)

  function update(patch: Partial<SeasonDraft>) {
    setDraft((current) => ({ ...(current ?? values), ...patch }))
  }

  async function save() {
    setSaving(true)
    setFieldErrors({})
    setFormError(undefined)
    const saved = await saveAdmin(
      RESOURCE,
      mode === 'edit' ? id : undefined,
      {
        season: values.season.trim(),
        beginDate: values.beginDate.trim(),
        endDate: values.endDate.trim(),
        isCurrent: values.current,
        ...(mode === 'edit' ? { version } : {}),
      },
      FIELDS,
    )
    setSaving(false)
    if (!saved.ok) {
      setFieldErrors(saved.fieldErrors)
      setFormError(saved.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  async function remove() {
    if (!id) {
      return
    }
    setDeleting(true)
    setFormError(undefined)
    const removed = await deleteAdmin(RESOURCE, id)
    setDeleting(false)
    if (!removed.ok) {
      setFormError(removed.formError)
      return
    }
    router.push(resumeHref(LIST))
  }

  return (
    <AdminForm
      title='Season'
      mode={mode}
      saving={saving}
      deleting={deleting}
      formError={formError}
      cancelHref={resumeHref(LIST)}
      onSubmit={() => {
        void save()
      }}
      onDelete={() => {
        void remove()
      }}
      lastUpdateDate={record?.lastUpdateDate}
      lastUpdateUser={record?.lastUpdateUser}
    >
      <TextField
        id='season-season'
        name='season'
        label='Season'
        value={values.season}
        error={fieldErrors.season}
        onChange={(season) => {
          update({ season })
        }}
      />
      <DateField
        id='season-begin'
        name='beginDate'
        label='Begin Date'
        value={values.beginDate}
        error={fieldErrors.beginDate}
        onChange={(beginDate) => {
          update({ beginDate })
        }}
      />
      <DateField
        id='season-end'
        name='endDate'
        label='End Date'
        value={values.endDate}
        error={fieldErrors.endDate}
        onChange={(endDate) => {
          update({ endDate })
        }}
      />
      <CheckboxField
        name='current'
        label='Current'
        checked={values.current}
        error={fieldErrors.current ?? fieldErrors.isCurrent}
        onChange={(current) => {
          update({ current })
        }}
      />
    </AdminForm>
  )
}
