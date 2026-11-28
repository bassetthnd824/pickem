'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerItem } from '@/lib/admin/query-string'
import { parseVenue, type VenueRecord } from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'venues'
const FIELDS = ['venueName', 'cityState']
const LIST = '/venues'

type VenueFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type VenueDraft = {
  venueName: string
  cityState: string
}

const EMPTY: VenueDraft = { venueName: '', cityState: '' }

function fromRecord(record: VenueRecord): VenueDraft {
  return { venueName: record.venueName, cityState: record.cityState }
}

export function VenueForm({ mode, id }: VenueFormProps) {
  const router = useRouter()
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenue(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<VenueDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [cfbdVenueId, setCfbdVenueId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (mode === 'edit' && recordQuery.isPending) {
    return <p>Loading venue.</p>
  }

  if (mode === 'edit' && !recordQuery.data) {
    return <p role='alert'>Could not load venue.</p>
  }

  const record = recordQuery.data
  if (mode === 'edit' && record && draft === null) {
    if (version !== record.version) {
      setVersion(record.version)
    }
    if (cfbdVenueId !== record.cfbdVenueId) {
      setCfbdVenueId(record.cfbdVenueId)
    }
  }
  const values = draft ?? (record ? fromRecord(record) : EMPTY)

  function update(patch: Partial<VenueDraft>) {
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
        venueName: values.venueName.trim(),
        cityState: values.cityState.trim(),
        cfbdVenueId: mode === 'edit' ? cfbdVenueId : null,
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
      title='Venue'
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
        id='venue-name'
        name='venueName'
        label='Name'
        value={values.venueName}
        error={fieldErrors.venueName}
        onChange={(venueName) => {
          update({ venueName })
        }}
      />
      <TextField
        id='venue-city'
        name='cityState'
        label='City/State'
        value={values.cityState}
        error={fieldErrors.cityState}
        onChange={(cityState) => {
          update({ cityState })
        }}
      />
    </AdminForm>
  )
}
