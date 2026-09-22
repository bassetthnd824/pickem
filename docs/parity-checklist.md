# Parity checklist

Maps every legacy JSP / Struts action (from `tiles.xml` + `struts.xml`) to a
Next.js route and Spring endpoint. Check a row only when the new surface
matches the old behavior (US-24 / US-26). Do not delete `src/` until this
list is fully checked.

The browser always hits Next.js. Endpoints below are the Spring paths; Next.js
exposes the same `/api/...` paths via the Axios BFF.

## Layout

| Legacy                                                    | New route / component                                                                            | Endpoint          | Done |
| --------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ----------------- | ---- |
| `template.jsp`, `headtag.jsp`, `header.jsp`, `footer.jsp` | Root App Router layout                                                                           | —                 | ☐    |
| `navbar.jsp`                                              | Shared navbar (Home, Leader Board, Team Schedule, My Account, Logout; Admin dropdown if manager) | —                 | ☐    |
| `searchTemplate.jsp`, `adminscreen.js`                    | Manager list/detail pattern + TanStack Query cascading selects                                   | helper GETs below | ☐    |
| `index.jsp` (redirect to `game/main`)                     | `/` → `/main` (or `/login`)                                                                      | —                 | ☐    |

## Auth (JAAS / registration dropped)

| Legacy                                                | New route                                         | Endpoint                        | Done |
| ----------------------------------------------------- | ------------------------------------------------- | ------------------------------- | ---- |
| `login.jsp`, `LoginFilter`, JAAS FORM                 | `/login` — Sign in with Google                    | `POST /api/auth/session`        | ☐    |
| `loginError.jsp`                                      | `/login` with error (US-37)                       | —                               | ☐    |
| `accessRestricted.jsp`                                | Forbidden / restricted on `(manager)` for players | middleware + `GET /api/auth/me` | ☐    |
| `registration.jsp`, `RegisterAction`, `register_save` | **Dropped.** Google sign-in provisions users      | —                               | ☐    |
| `LogoutAction`                                        | Logout control                                    | `POST /api/auth/logout`         | ☐    |
| Session cookie (container)                            | Host-only `__session` on Next.js                  | `GET /api/auth/me`              | ☐    |

## Game screens

| Legacy                                                                          | New route        | Endpoint                                         | Done |
| ------------------------------------------------------------------------------- | ---------------- | ------------------------------------------------ | ---- |
| `main.jsp`, `main.js`, `MainAction`, `main_save`                                | `/main`          | `GET /api/game/main`, `POST /api/game/picks`     | ☐    |
| `leaderBoard.jsp`, `LeaderBoardAction`                                          | `/leaderboard`   | `GET /api/game/leaderboard?seasonId=`            | ☐    |
| `teamSchedule.jsp`, `teamSchedule.js`, `TeamScheduleAction`, `teamSchedule_get` | `/team-schedule` | `GET /api/game/team-schedule?teamId=&seasonId=`  | ☐    |
| `account.jsp`, `AccountAction`, `account_save` (password fields **dropped**)    | `/account`       | `GET /api/game/account`, `PUT /api/game/account` | ☐    |

## Manager screens

Each legacy resource had `*_init` / `*_search` / `*_add` / `*_edit` /
`*_delete` / `*_save` via `BaseAction`, plus list and detail Tiles.

| Legacy list / detail / action                                                                           | New routes                                            | Endpoints                        | Done |
| ------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- | -------------------------------- | ---- |
| `seasons.jsp` / `season.jsp`, `SeasonAction`, `seasons_getSeasonById`                                   | `/manager/seasons`, `/manager/seasons/[id]`           | CRUD `/api/manager/seasons`      | ☐    |
| `seasonWeeks.jsp` / `seasonWeek.jsp`, `SeasonWeekAction`, `getSeasonWeeksBySeason`, `getSeasonWeekById` | `/manager/season-weeks`, `/manager/season-weeks/[id]` | CRUD `/api/manager/season-weeks` | ☐    |
| `venues.jsp` / `venue.jsp`, `VenueAction`                                                               | `/manager/venues`, `/manager/venues/[id]`             | CRUD `/api/manager/venues`       | ☐    |
| `teams.jsp` / `team.jsp`, `TeamAction`, `teams_getTeamById`                                             | `/manager/teams`, `/manager/teams/[id]`               | CRUD `/api/manager/teams`        | ☐    |
| `rivalries.jsp` / `rivalry.jsp`, `RivalryAction`                                                        | `/manager/rivalries`, `/manager/rivalries/[id]`       | CRUD `/api/manager/rivalries`    | ☐    |
| `matchups.jsp` / `matchup.jsp`, `MatchupAction`                                                         | `/manager/matchups`, `/manager/matchups/[id]`         | CRUD `/api/manager/matchups`     | ☐    |
| `users.jsp` / `user.jsp`, `UserAction`                                                                  | `/manager/users`, `/manager/users/[id]`               | CRUD `/api/manager/users`        | ☐    |
| `themes.jsp` / `theme.jsp`, `ThemeAction`                                                               | `/manager/themes`, `/manager/themes/[id]`             | CRUD `/api/manager/themes`       | ☐    |

## New product surface (no legacy equivalent)

| Feature                                | New route / UI                                | Endpoint                                                                                | Done |
| -------------------------------------- | --------------------------------------------- | --------------------------------------------------------------------------------------- | ---- |
| CFBD import UI (US-30)                 | Import from CFBD on matchups admin            | `POST /api/manager/imports/matchups` (202 + job id), `GET /api/manager/imports/{jobId}` | ☐    |
| Optional refresh scores                | Button on matchups admin                      | `POST /api/manager/imports/scores`                                                      | ☐    |
| Game-day score job (US-29)             | Cloud Scheduler (no player UI)                | `POST /api/internal/jobs/cfbd-scores`                                                   | ☐    |
| Switchable 18 themes (US-14)           | `data-theme` + account picker                 | `PUT /api/game/account` (`themeId`)                                                     | ☐    |
| Empty season / expired session (US-37) | `/main` empty state; `/login` session-expired | —                                                                                       | ☐    |

## Intentionally not ported

| Legacy                                     | Reason                            |
| ------------------------------------------ | --------------------------------- |
| Password registration, `userPass`, SHA-256 | Google / Firebase owns identity   |
| `PCKM_GROUP` / `PCKM_USER_GROUP`           | Custom claims + `users.roles[]`   |
| Sequences (`PCKM_*_SEQ`)                   | Firestore auto-IDs                |
| Derby / JPA / EJB / Struts                 | Nx Next.js + Spring Boot          |
| Browser calls to a Java origin             | Next.js is the only public origin |
