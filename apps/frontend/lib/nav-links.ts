/**
 * Shared destinations for the signed-in navbar. Route prefixes in the
 * session gate are derived from these hrefs.
 */

export const GAME_LINKS = [
  { href: "/main", label: "Home" },
  { href: "/leaderboard", label: "Leader Board" },
  { href: "/team-schedule", label: "Team Schedule" },
  { href: "/account", label: "My Account" },
] as const;

export const MANAGER_LINKS = [
  { href: "/users", label: "Users" },
  { href: "/themes", label: "Themes" },
  { href: "/teams", label: "Teams" },
  { href: "/venues", label: "Venues" },
  { href: "/rivalries", label: "Rivalries" },
  { href: "/seasons", label: "Seasons" },
  { href: "/season-weeks", label: "Season Weeks" },
  { href: "/matchups", label: "Matchups" },
] as const;

/** Signed-in page shown when a player opens a manager URL. */
export const RESTRICTED_PATH = "/restricted";
