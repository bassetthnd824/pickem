'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { SelectField } from '@/components/ui/select-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerCollection, managerItem } from '@/lib/admin/query-string'
import {
  parseRivalry,
  parseTeams,
  teamOptionLabel,
  type RivalryRecord,
} from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'rivalries'
const FIELDS = ['rivalryName', 'team1Id', 'team2Id']
const LIST = '/rivalries'

type RivalryFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type RivalryDraft = {
  rivalryName: string
  team1Id: string
  team2Id: string
}

const EMPTY: RivalryDraft = { rivalryName: '', team1Id: '', team2Id: '' }

function fromRecord(record: RivalryRecord): RivalryDraft {
  return {
    rivalryName: record.rivalryName,
    team1Id: record.team1Id,
    team2Id: record.team2Id,
  }
}

export function RivalryForm({ mode, id }: RivalryFormProps) {
  const router = useRouter()
  const teamsQuery = useQuery({
    queryKey: ['manager', 'teams', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTeams(
        await api.get<unknown>(managerCollection('teams'), { signal }),
      ),
  })
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseRivalry(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<RivalryDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (teamsQuery.isPending || (mode === 'edit' && recordQuery.isPending)) {
    return <p>Loading rivalry.</p>
  }

  if (!teamsQuery.data || (mode === 'edit' && !recordQuery.data)) {
    return <p role='alert'>Could not load rivalry.</p>
  }

  const teams = teamsQuery.data
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
  const options = teams.map((team) => ({
    value: team.id,
    label: teamOptionLabel(team),
  }))

  function update(patch: Partial<RivalryDraft>) {
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
        rivalryName: values.rivalryName.trim(),
        team1Id: values.team1Id,
        team2Id: values.team2Id,
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
      title='Rivalry'
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
        id='rivalry-name'
        name='rivalryName'
        label='Name'
        value={values.rivalryName}
        error={fieldErrors.rivalryName}
        onChange={(rivalryName) => {
          update({ rivalryName })
        }}
      />
      <SelectField
        name='team1Id'
        label='Team 1'
        emptyLabel='Please select a Team'
        value={values.team1Id}
        error={fieldErrors.team1Id}
        options={options}
        onChange={(team1Id) => {
          update({ team1Id })
        }}
      />
      <SelectField
        name='team2Id'
        label='Team 2'
        emptyLabel='Please select a Team'
        value={values.team2Id}
        error={fieldErrors.team2Id}
        options={options}
        onChange={(team2Id) => {
          update({ team2Id })
        }}
      />
    </AdminForm>
  )
}
