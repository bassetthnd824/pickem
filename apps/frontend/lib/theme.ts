/**
 * Closed catalog of 18 theme keys. Hex values live in `app/global.css`.
 * `themeId` matches `users.themeId` and the `themeId` request cookie.
 */

export const THEME_KEYS = [
  "light",
  "dark",
  "alabama",
  "arkansas",
  "auburn",
  "florida",
  "georgia",
  "kentucky",
  "lsu",
  "mississippi-state",
  "missouri",
  "oklahoma",
  "ole-miss",
  "south-carolina",
  "tennessee",
  "texas",
  "texas-am",
  "vanderbilt",
] as const;

export type ThemeKey = (typeof THEME_KEYS)[number];

export const DEFAULT_THEME: ThemeKey = "light";

/** Mirrors `users.themeId` so the root layout can paint it during SSR. */
export const THEME_COOKIE_NAME = "themeId";

const THEME_KEY_SET: ReadonlySet<string> = new Set(THEME_KEYS);

export function isThemeKey(
  value: string | null | undefined,
): value is ThemeKey {
  return typeof value === "string" && THEME_KEY_SET.has(value);
}

/**
 * A known profile theme wins. Otherwise a known cookie theme is used.
 * An absent or unknown value is Light, including the first visit.
 */
export function resolveThemeId(
  profileThemeId?: string | null,
  cookieThemeId?: string | null,
): ThemeKey {
  if (isThemeKey(profileThemeId)) {
    return profileThemeId;
  }
  if (isThemeKey(cookieThemeId)) {
    return cookieThemeId;
  }
  return DEFAULT_THEME;
}
