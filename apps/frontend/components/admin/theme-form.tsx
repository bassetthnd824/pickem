'use client'

import { useQuery } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useState } from 'react'

import { AdminForm } from '@/components/admin/admin-form'
import { CheckboxField } from '@/components/ui/checkbox-field'
import { TextField } from '@/components/ui/text-field'
import { deleteAdmin, saveAdmin } from '@/lib/admin/admin-actions'
import type { FieldErrors } from '@/lib/admin/problem-detail'
import { managerItem } from '@/lib/admin/query-string'
import { parseTheme, type ThemeRecord } from '@/lib/admin/records'
import { resumeHref } from '@/lib/admin/search-memory'
import { api } from '@/lib/api-client'

const RESOURCE = 'themes'
const FIELDS = ['themeName', 'themePath', 'active']
const LIST = '/themes'
const PATH_SLASH = 'theme path must not begin with a slash'

type ThemeFormProps = {
  mode: 'add' | 'edit'
  id?: string
}

type ThemeDraft = {
  themeName: string
  themePath: string
  active: boolean
  primary: string
  secondary: string
}

const EMPTY: ThemeDraft = {
  themeName: '',
  themePath: '',
  active: false,
  primary: '',
  secondary: '',
}

function fromRecord(record: ThemeRecord): ThemeDraft {
  return {
    themeName: record.themeName,
    themePath: record.themePath,
    active: record.active,
    primary: record.primary,
    secondary: record.secondary,
  }
}

export function ThemeForm({ mode, id }: ThemeFormProps) {
  const router = useRouter()
  const recordQuery = useQuery({
    queryKey: ['manager', RESOURCE, id],
    enabled: mode === 'edit' && Boolean(id),
    staleTime: 0,
    queryFn: async ({ signal }) =>
      parseTheme(
        await api.get<unknown>(managerItem(RESOURCE, id ?? ''), { signal }),
      ),
  })
  const [draft, setDraft] = useState<ThemeDraft | null>(null)
  const [version, setVersion] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | undefined>()

  if (mode === 'edit' && recordQuery.isPending) {
    return <p>Loading theme.</p>
  }

  if (mode === 'edit' && !recordQuery.data) {
    return <p role='alert'>Could not load theme.</p>
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

  function update(patch: Partial<ThemeDraft>) {
    setDraft((current) => ({ ...(current ?? values), ...patch }))
  }

  function changePath(themePath: string) {
    update({ themePath })
  }

  async function save() {
    const themePath = values.themePath.trim()
    if (themePath.startsWith('/')) {
      setFieldErrors({ themePath: PATH_SLASH })
      setFormError(undefined)
      return
    }
    setSaving(true)
    setFieldErrors({})
    setFormError(undefined)
    const saved = await saveAdmin(
      RESOURCE,
      mode === 'edit' ? id : undefined,
      {
        themeName: values.themeName.trim(),
        themePath,
        active: values.active,
        primary:
          record != null && themePath === record.themePath
            ? values.primary
            : '',
        secondary:
          record != null && themePath === record.themePath
            ? values.secondary
            : '',
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
      title='Theme'
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
        id='theme-name'
        name='themeName'
        label='Name'
        value={values.themeName}
        error={fieldErrors.themeName}
        onChange={(themeName) => {
          update({ themeName })
        }}
      />
      <TextField
        id='theme-path'
        name='themePath'
        label='Path'
        value={values.themePath}
        error={fieldErrors.themePath}
        onChange={changePath}
      />
      <CheckboxField
        name='active'
        label='Active'
        checked={values.active}
        error={fieldErrors.active}
        onChange={(active) => {
          update({ active })
        }}
      />
    </AdminForm>
  )
}
