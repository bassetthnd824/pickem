'use client'

import { Field } from '@base-ui/react/field'
import { Input } from '@base-ui/react/input'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { AUTH_ME_KEY, useAuth } from '@/components/auth-provider'
import { ThemePicker } from '@/components/theme-picker'
import { Button } from '@/components/ui/button'
import { ApiError, api } from '@/lib/api-client'
import type { AuthProfile } from '@/lib/auth-profile'
import { isThemeKey, rememberTheme } from '@/lib/theme'
import {
  accountQueryKey,
  NICK_NAME_MAX,
  nickNameError,
  parseAccount,
  type AccountProfile,
} from '@/lib/theme-picker'

const inputClass =
  'rounded border border-border bg-surface px-2 py-1 text-text read-only:bg-surface'

async function loadAccount(signal: AbortSignal): Promise<AccountProfile> {
  const account = parseAccount(
    await api.get<unknown>('/api/game/account', { signal }),
  )
  if (!account) {
    throw new Error('Account response was invalid')
  }
  return account
}

function errorDetail(error: unknown): string {
  if (
    error instanceof ApiError &&
    error.body &&
    typeof error.body === 'object'
  ) {
    const detail = (error.body as { detail?: unknown }).detail
    if (typeof detail === 'string' && detail.trim() !== '') {
      return detail
    }
  }
  return 'Could not save the screen name.'
}

export function AccountForm() {
  const queryClient = useQueryClient()
  const { profile } = useAuth()
  const uid = profile?.uid ?? ''
  const accountKey = accountQueryKey(uid)
  const accountQuery = useQuery({
    queryKey: accountKey,
    queryFn: ({ signal }) => loadAccount(signal),
    enabled: uid !== '',
  })
  const [draft, setDraft] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [themeSaving, setThemeSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (uid === '' || accountQuery.isPending) {
    return <p>Loading account.</p>
  }

  if (!accountQuery.data) {
    return <p role='alert'>Could not load account.</p>
  }

  const account = accountQuery.data
  const nickName = draft ?? account.nickName

  async function save() {
    const message = nickNameError(nickName)
    if (message) {
      setError(message)
      return
    }
    if (themeSaving) {
      return
    }
    const trimmed = nickName.trim()
    const latest =
      queryClient.getQueryData<AccountProfile>(accountKey) ?? account
    if (trimmed === latest.nickName) {
      setDraft(null)
      setError(null)
      return
    }
    setSaving(true)
    setError(null)
    try {
      const saved = parseAccount(
        await api.put<unknown>('/api/game/account', {
          nickName: trimmed,
          themeId: latest.themeId,
          version: latest.version,
        }),
      )
      if (!saved) {
        throw new Error('Account response was invalid')
      }
      await queryClient.cancelQueries({ queryKey: accountKey })
      await queryClient.cancelQueries({ queryKey: AUTH_ME_KEY })
      queryClient.setQueryData(accountKey, saved)
      queryClient.setQueryData(
        AUTH_ME_KEY,
        (current: AuthProfile | undefined) =>
          current && current.uid === uid
            ? {
                ...current,
                nickName: saved.nickName,
                themeId: saved.themeId,
              }
            : current,
      )
      if (isThemeKey(saved.themeId)) {
        rememberTheme(saved.themeId)
      }
      setDraft(null)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        await queryClient.refetchQueries({ queryKey: accountKey })
        setError('The account changed. Save again.')
      } else {
        setError(errorDetail(caught))
      }
    } finally {
      setSaving(false)
    }
  }

  function cancel() {
    setDraft(null)
    setError(null)
  }

  return (
    <form
      className='flex max-w-md flex-col gap-4'
      onSubmit={(event) => {
        event.preventDefault()
        void save()
      }}
    >
      <ReadOnlyField
        name='emailAddr'
        label='Email Address'
        value={account.emailAddr}
      />
      <ReadOnlyField
        name='firstName'
        label='First Name'
        value={account.firstName}
      />
      <ReadOnlyField
        name='lastName'
        label='Last Name'
        value={account.lastName}
      />
      <Field.Root
        name='nickName'
        disabled={saving}
        invalid={Boolean(error)}
        className='flex flex-col gap-1'
      >
        <Field.Label className='text-sm font-medium text-text'>
          Screen Name
        </Field.Label>
        <Input
          id='nickName'
          value={nickName}
          maxLength={NICK_NAME_MAX}
          aria-invalid={error ? true : undefined}
          onValueChange={(value) => {
            setDraft(value)
            setError(null)
          }}
          className={inputClass}
        />
        {error ? (
          <Field.Error className='text-sm text-danger' match role='alert'>
            {error}
          </Field.Error>
        ) : null}
      </Field.Root>
      <ThemePicker disabled={saving} onSavingChange={setThemeSaving} />
      <div className='flex justify-end gap-2'>
        <Button
          type='button'
          variant='secondary'
          disabled={saving}
          onClick={cancel}
        >
          Cancel
        </Button>
        <Button type='submit' disabled={saving || themeSaving}>
          Save
        </Button>
      </div>
    </form>
  )
}

function ReadOnlyField({
  name,
  label,
  value,
}: {
  name: string
  label: string
  value: string
}) {
  return (
    <Field.Root name={name} className='flex flex-col gap-1'>
      <Field.Label className='text-sm font-medium text-text'>
        {label}
      </Field.Label>
      <Input readOnly value={value} className={inputClass} />
    </Field.Root>
  )
}
