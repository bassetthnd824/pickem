import { isThemeKey } from '@/lib/theme'

/** Prefix for every signed-in player's account query. Logout removes this. */
export const ACCOUNT_KEY_PREFIX = ['game', 'account'] as const

export const THEME_CHOICES_KEY = ['game', 'themes'] as const

export function accountQueryKey(uid: string) {
  return ['game', 'account', uid] as const
}

/**
 * One active catalog theme the account picker may offer.
 * A key with no CSS block is dropped so `data-theme` cannot be left unstyled.
 */
export type ThemeChoice = {
  id: string
  themeName: string
  themePath: string
}

export type AccountTheme = {
  nickName: string
  themeId: string
  version: number
}

export function themeChoices(value: unknown): ThemeChoice[] {
  if (!Array.isArray(value)) {
    return []
  }
  const choices: ThemeChoice[] = []
  for (const item of value) {
    if (!item || typeof item !== 'object') {
      continue
    }
    const record = item as Record<string, unknown>
    if (record.active === false) {
      continue
    }
    const id = typeof record.id === 'string' ? record.id : ''
    const themeName =
      typeof record.themeName === 'string' ? record.themeName.trim() : ''
    const themePath =
      typeof record.themePath === 'string' ? record.themePath : ''
    if (id === '' || themeName === '' || !isThemeKey(themePath)) {
      continue
    }
    choices.push({ id, themeName, themePath })
  }
  return choices
}

export function parseAccountTheme(value: unknown): AccountTheme | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const record = value as Record<string, unknown>
  if (typeof record.nickName !== 'string' || record.nickName.trim() === '') {
    return null
  }
  if (typeof record.themeId !== 'string' || record.themeId.trim() === '') {
    return null
  }
  if (typeof record.version !== 'number' || !Number.isInteger(record.version)) {
    return null
  }
  return {
    nickName: record.nickName.trim(),
    themeId: record.themeId.trim(),
    version: record.version,
  }
}

/** True when the saved key is styled but the picker must not offer it. */
export function keepsCurrentTheme(
  themeId: string,
  choices: readonly ThemeChoice[],
): boolean {
  return (
    isThemeKey(themeId) &&
    !choices.some((choice) => choice.themePath === themeId)
  )
}
