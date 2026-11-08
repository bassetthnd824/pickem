'use client'

import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { AUTH_ME_KEY, useAuth } from '@/components/auth-provider'
import { Radio, RadioGroup } from '@/components/ui/radio'
import { ApiError, api } from '@/lib/api-client'
import type { AuthProfile } from '@/lib/auth-profile'
import { isThemeKey, rememberTheme } from '@/lib/theme'
import {
  accountQueryKey,
  keepsCurrentTheme,
  parseAccount,
  THEME_CHOICES_KEY,
  themeChoices,
  type AccountProfile,
  type ThemeChoice,
} from '@/lib/theme-picker'

async function loadThemes(signal: AbortSignal): Promise<ThemeChoice[]> {
  return themeChoices(await api.get<unknown>('/api/game/themes', { signal }))
}

async function loadAccount(signal: AbortSignal): Promise<AccountProfile> {
  const account = parseAccount(
    await api.get<unknown>('/api/game/account', { signal }),
  )
  if (!account) {
    throw new Error('Account response was invalid')
  }
  return account
}

export function ThemePicker({
  disabled = false,
  onSavingChange,
}: {
  disabled?: boolean
  onSavingChange?: (saving: boolean) => void
}) {
  const queryClient = useQueryClient()
  const { profile } = useAuth()
  const uid = profile?.uid ?? ''
  const accountKey = accountQueryKey(uid)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const themesQuery = useQuery({
    queryKey: THEME_CHOICES_KEY,
    queryFn: ({ signal }) => loadThemes(signal),
    enabled: uid !== '',
  })
  const accountQuery = useQuery({
    queryKey: accountKey,
    queryFn: ({ signal }) => loadAccount(signal),
    enabled: uid !== '',
  })

  if (uid === '' || themesQuery.isPending || accountQuery.isPending) {
    return <p>Loading themes.</p>
  }

  if (!themesQuery.data || !accountQuery.data) {
    return <p role='alert'>Could not load themes.</p>
  }

  const choices = themesQuery.data
  const account = accountQuery.data
  const selected = choices.some(
    (choice) => choice.themePath === account.themeId,
  )
    ? account.themeId
    : null

  async function choose(themePath: string) {
    if (
      disabled ||
      saving ||
      !isThemeKey(themePath) ||
      themePath === account.themeId
    ) {
      return
    }
    if (!choices.some((choice) => choice.themePath === themePath)) {
      return
    }
    setSaving(true)
    onSavingChange?.(true)
    setError(null)
    try {
      const saved = parseAccount(
        await api.put<unknown>('/api/game/account', {
          nickName: account.nickName,
          themeId: themePath,
          version: account.version,
        }),
      )
      if (!saved || !isThemeKey(saved.themeId)) {
        throw new Error('Account response was invalid')
      }
      await queryClient.cancelQueries({ queryKey: accountKey })
      await queryClient.cancelQueries({ queryKey: AUTH_ME_KEY })
      queryClient.setQueryData(accountKey, saved)
      queryClient.setQueryData(
        AUTH_ME_KEY,
        (current: AuthProfile | undefined) =>
          current && current.uid === uid
            ? { ...current, themeId: saved.themeId }
            : current,
      )
      rememberTheme(saved.themeId)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        await queryClient.refetchQueries({ queryKey: accountKey })
      }
      setError('Could not save the theme.')
    } finally {
      setSaving(false)
      onSavingChange?.(false)
    }
  }

  return (
    <fieldset className='flex flex-col gap-3' disabled={saving || disabled}>
      <legend className='text-sm font-medium text-text'>Theme</legend>
      {keepsCurrentTheme(account.themeId, choices) ? (
        <p>Your current theme stays until you pick another.</p>
      ) : null}
      {choices.length === 0 ? (
        <p>No themes are available.</p>
      ) : (
        <RadioGroup
          aria-label='Theme'
          className='flex flex-col gap-2'
          disabled={saving || disabled}
          value={selected}
          onValueChange={(value) => {
            void choose(String(value))
          }}
        >
          {choices.map((choice) => (
            <label key={choice.id} className='inline-flex items-center gap-2'>
              <Radio value={choice.themePath} aria-label={choice.themeName} />
              <span>{choice.themeName}</span>
            </label>
          ))}
        </RadioGroup>
      )}
      {error ? <p role='alert'>{error}</p> : null}
    </fieldset>
  )
}
