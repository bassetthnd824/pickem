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
import { parseThemes, parseUser, type UserRecord } from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'users'
const FIELDS = [
  'uid',
  'emailAddr',
  'firstName',
  'lastName',
  'nickName',
  'themeId',
  'roles',
]
const LIST = '/users'
const ROLE_REQUIRED = 'User must belong to at least one group.'

type UserFormProps = {
  id: string
}

type UserDraft = {
  emailAddr: string
  firstName: string
  lastName: string
  nickName: string
  themeId: string
  roles: string[]
}

function fromRecord(record: UserRecord): UserDraft {
  return {
    emailAddr: record.emailAddr,
    firstName: record.firstName,
    lastName: record.lastName,
    nickName: record.nickName,
    themeId: record.themeId,
    roles: orderedRoles(record.roles),
  }
}

function orderedRoles(roles: readonly string[]): string[] {
  const selected = new Set(roles)
  return ['player', 'manager'].filter((role) => selected.has(role))
}

function withRole(roles: readonly string[], role: string, checked: boolean) {
  const selected = new Set(roles)
  if (checked) {
    selected.add(role)
  } else {
    selected.delete(role)
  }
  return orderedRoles([...selected])
}

export function UserForm({ id }: UserFormProps) {
  const router = useRouter()
  const themesQuery = useQuery({
    queryKey: ['manager', 'themes', 'choices'],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseThemes(
        await api.get<unknown>(managerCollection('themes'), { signal }),
      ),
  })
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseUser(await api.get<unknown>(managerItem(RESOURCE, id), { signal })),
  })
  const [draft, setDraft] = useState<UserDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (themesQuery.isPending || recordQuery.isPending) {
    return <p>Loading user.</p>
  }

  if (!themesQuery.data || !recordQuery.data) {
    return <p role='alert'>Could not load user.</p>
  }

  const record = recordQuery.data
  if (draft === null && version !== record.version) {
    setVersion(record.version)
  }
  const values = draft ?? fromRecord(record)
  const themeOptions = [...themesQuery.data]
    .sort((left, right) => left.themeName.localeCompare(right.themeName))
    .map((theme) => ({
      value: theme.themePath,
      label: theme.themeName,
    }))
  if (
    values.themeId !== '' &&
    !themeOptions.some((option) => option.value === values.themeId)
  ) {
    themeOptions.push({ value: values.themeId, label: values.themeId })
  }

  function update(patch: Partial<UserDraft>) {
    setDraft((current) => ({ ...(current ?? values), ...patch }))
  }

  async function save() {
    if (values.roles.length === 0) {
      setFieldErrors({ roles: ROLE_REQUIRED })
      setFormError(undefined)
      return
    }
    setSaving(true)
    setFieldErrors({})
    setFormError(undefined)
    const saved = await saveAdmin(
      RESOURCE,
      id,
      {
        uid: record.uid,
        emailAddr: values.emailAddr.trim(),
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        nickName: values.nickName.trim(),
        themeId: values.themeId,
        roles: values.roles,
        version,
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
      title='User'
      mode='edit'
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
      lastUpdateDate={record.lastUpdateDate}
      lastUpdateUser={record.lastUpdateUser}
    >
      {fieldErrors.uid ? (
        <p className='field-error'>{fieldErrors.uid}</p>
      ) : null}
      <TextField
        id='user-email'
        name='emailAddr'
        label='Email Address'
        value={values.emailAddr}
        error={fieldErrors.emailAddr}
        onChange={(emailAddr) => {
          update({ emailAddr })
        }}
      />
      <TextField
        id='user-first'
        name='firstName'
        label='First Name'
        value={values.firstName}
        error={fieldErrors.firstName}
        onChange={(firstName) => {
          update({ firstName })
        }}
      />
      <TextField
        id='user-last'
        name='lastName'
        label='Last Name'
        value={values.lastName}
        error={fieldErrors.lastName}
        onChange={(lastName) => {
          update({ lastName })
        }}
      />
      <TextField
        id='user-nickname'
        name='nickName'
        label='Nickname'
        value={values.nickName}
        error={fieldErrors.nickName}
        onChange={(nickName) => {
          update({ nickName })
        }}
      />
      <SelectField
        name='themeId'
        label='Theme'
        emptyLabel='Please select a Theme'
        value={values.themeId}
        error={fieldErrors.themeId}
        options={themeOptions}
        onChange={(themeId) => {
          update({ themeId })
        }}
      />
      <div className='flex flex-col gap-2'>
        <p className='field-label'>Roles</p>
        <CheckboxField
          name='player'
          label='Player'
          checked={values.roles.includes('player')}
          onChange={(checked) => {
            update({ roles: withRole(values.roles, 'player', checked) })
          }}
        />
        <CheckboxField
          name='manager'
          label='Manager'
          checked={values.roles.includes('manager')}
          onChange={(checked) => {
            update({ roles: withRole(values.roles, 'manager', checked) })
          }}
        />
        {fieldErrors.roles ? (
          <p className='field-error'>{fieldErrors.roles}</p>
        ) : null}
        <p className='text-muted'>
          A manager role applies the next time this user signs in.
        </p>
      </div>
    </AdminForm>
  )
}
