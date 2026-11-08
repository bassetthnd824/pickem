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

/** Matches `AccountService.NICK_NAME_MAX`. */
export const NICK_NAME_MAX = 40

export type AccountProfile = {
  emailAddr: string
  firstName: string
  lastName: string
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

function textField(value: unknown): string | null {
  return typeof value === 'string' ? value : null
}

export function parseAccount(value: unknown): AccountProfile | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const record = value as Record<string, unknown>
  const emailAddr = textField(record.emailAddr)
  const firstName = textField(record.firstName)
  const lastName = textField(record.lastName)
  const nickName = textField(record.nickName)
  const themeId = textField(record.themeId)
  if (
    emailAddr === null ||
    firstName === null ||
    lastName === null ||
    nickName === null ||
    nickName.trim() === '' ||
    themeId === null ||
    themeId.trim() === ''
  ) {
    return null
  }
  if (typeof record.version !== 'number' || !Number.isInteger(record.version)) {
    return null
  }
  return {
    emailAddr,
    firstName,
    lastName,
    nickName: nickName.trim(),
    themeId: themeId.trim(),
    version: record.version,
  }
}

/** Client check before `PUT /api/game/account`. Messages match the API. */
export function nickNameError(value: string): string | null {
  const trimmed = value.trim()
  if (trimmed === '') {
    return 'nickName is required'
  }
  if (trimmed.length > NICK_NAME_MAX) {
    return 'nickName must be 40 characters or fewer'
  }
  return null
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
