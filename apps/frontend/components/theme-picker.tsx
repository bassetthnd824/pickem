'use client'

import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import { AUTH_ME_KEY, useAuth } from '@/components/auth-provider'
import { Radio, RadioGroup } from '@/components/ui/radio'
import { api } from '@/lib/api-client'
import type { AuthProfile } from '@/lib/auth-profile'
import { isThemeKey, rememberTheme } from '@/lib/theme'
import {
  accountQueryKey,
  keepsCurrentTheme,
  parseAccountTheme,
  THEME_CHOICES_KEY,
  themeChoices,
  type AccountTheme,
  type ThemeChoice,
} from '@/lib/theme-picker'

async function loadThemes(signal: AbortSignal): Promise<ThemeChoice[]> {
  return themeChoices(await api.get<unknown>('/api/game/themes', { signal }))
}

async function loadAccount(signal: AbortSignal): Promise<AccountTheme> {
  const account = parseAccountTheme(
    await api.get<unknown>('/api/game/account', { signal }),
  )
  if (!account) {
    throw new Error('Account response was invalid')
  }
  return account
}

export function ThemePicker() {
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
    if (!isThemeKey(themePath) || themePath === account.themeId || saving) {
      return
    }
    if (!choices.some((choice) => choice.themePath === themePath)) {
      return
    }
    setSaving(true)
    setError(null)
    try {
      const saved = parseAccountTheme(
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
    } catch {
      setError('Could not save the theme.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <fieldset className='flex flex-col gap-3' disabled={saving}>
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
