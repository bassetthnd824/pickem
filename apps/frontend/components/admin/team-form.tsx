'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { CheckboxField } from '@/components/ui/checkbox-field'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerCollection, managerItem } from '@/lib/admin/query-string'
import { parseTeam, parseVenues, type TeamRecord } from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'teams'
const FIELDS = ['teamName', 'squadName', 'conferenceMember', 'homeVenueId']
const LIST = '/teams'

type TeamFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type TeamDraft = {
  teamName: string
  squadName: string
  conferenceMember: boolean
  homeVenueId: string
}

const EMPTY: TeamDraft = {
  teamName: '',
  squadName: '',
  conferenceMember: false,
  homeVenueId: '',
}

function fromRecord(record: TeamRecord): TeamDraft {
  return {
    teamName: record.teamName,
    squadName: record.squadName,
    conferenceMember: record.conferenceMember,
    homeVenueId: record.homeVenueId,
  }
}

export function TeamForm({ mode, id }: TeamFormProps) {
  const router = useRouter()
  const venuesQuery = useQuery({
    queryKey: ['manager', 'venues', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseVenues(
        await api.get<unknown>(managerCollection('venues'), { signal }),
      ),
  })
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeam(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<TeamDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [cfbdTeamId, setCfbdTeamId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (venuesQuery.isPending || (mode === 'edit' && recordQuery.isPending)) {
    return <p>Loading team.</p>
  }

  if (!venuesQuery.data || (mode === 'edit' && !recordQuery.data)) {
    return <p role='alert'>Could not load team.</p>
  }

  const venues = venuesQuery.data
  const record = recordQuery.data
  if (mode === 'edit' && record && draft === null) {
    if (version !== record.version) {
      setVersion(record.version)
    }
    if (cfbdTeamId !== record.cfbdTeamId) {
      setCfbdTeamId(record.cfbdTeamId)
    }
  }
  const values = draft ?? (record ? fromRecord(record) : EMPTY)

  function update(patch: Partial<TeamDraft>) {
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
        teamName: values.teamName.trim(),
        squadName: values.squadName.trim(),
        conferenceMember: values.conferenceMember,
        homeVenueId: values.homeVenueId,
        cfbdTeamId: mode === 'edit' ? cfbdTeamId : null,
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
      title='Team'
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
        id='team-name'
        name='teamName'
        label='Name'
        value={values.teamName}
        error={fieldErrors.teamName}
        onChange={(teamName) => {
          update({ teamName })
        }}
      />
      <TextField
        id='team-squad'
        name='squadName'
        label='Squad Name'
        value={values.squadName}
        error={fieldErrors.squadName}
        onChange={(squadName) => {
          update({ squadName })
        }}
      />
      <CheckboxField
        name='conferenceMember'
        label='Conference Member'
        checked={values.conferenceMember}
        error={fieldErrors.conferenceMember}
        onChange={(conferenceMember) => {
          update({ conferenceMember })
        }}
      />
      <SelectField
        name='homeVenueId'
        label='Home Venue'
        emptyLabel='Please select a Home Venue'
        value={values.homeVenueId}
        error={fieldErrors.homeVenueId}
        options={venues.map((venue) => ({
          value: venue.id,
          label: venue.venueName,
        }))}
        onChange={(homeVenueId) => {
          update({ homeVenueId })
        }}
      />
    </AdminForm>
  )
}
