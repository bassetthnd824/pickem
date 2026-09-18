# Firestore data model

Source of truth for the nine domain collections. Companion to
[`modernization-plan.md`](./modernization-plan.md) (*Data model*) and
[`api-contract.md`](./api-contract.md). Seed work (US-12) and backend DTOs
(US-03+) must follow this document.

Firestore is used in **native mode**. The browser never talks to Firestore.
Spring Boot (Admin SDK / service account) is the only writer. See
[`firestore.rules`](../firestore.rules).

## What was dropped from Derby

| Legacy | Replacement |
|---|---|
| `PCKM_GROUP` / `PCKM_USER_GROUP` | Firebase custom claims (`player`, `manager`) plus a `roles[]` array on `users` |
| `USER_PASS` / SHA-256 / JAAS | Google sign-in via Firebase Auth; no password fields |
| `PCKM_*_SEQ` sequences | Firestore auto-IDs (string). `users` documents are keyed by Firebase `uid` |
| JPA `_` metamodel classes | Not generated; DTOs live in `apps/backend` |
| `getCurrentSeason` native SQL | `isCurrent` flag and/or a date-window query on `seasons` |
| SQL joins (`Test.sql`) | Denormalized snapshots on read-heavy documents; in-code joins in services |

No historical Derby data is migrated. Schema only.

## Document conventions

Every domain document except where noted carries:

| Field | Type | Notes |
|---|---|---|
| `createDate` | timestamp | Set on insert; never overwritten |
| `createUser` | string | Firebase `uid` or system actor (`seed`, `cfbd-import`) |
| `lastUpdateDate` | timestamp | Set on every write |
| `lastUpdateUser` | string | Firebase `uid` or system actor |
| `version` | number | Optimistic lock (legacy `sysModCount`). `BaseRepository` checks it in a transaction and rejects a stale value |

IDs are Firestore document IDs (string), not numeric sequences.

**Dates on the wire** are ISO-8601 (`YYYY-MM-DD` for calendar dates, RFC 3339
for timestamps). Store timestamps as Firestore `Timestamp`; calendar dates may
be stored as `YYYY-MM-DD` strings so day-boundary queries do not depend on a
timezone. Game-day jobs use `America/New_York`.

**Deletes** return HTTP 409 while children or references exist. No cascade of
picks. Saving a team or venue **refreshes denormalized snapshots** on matchups,
rivalries, and teams that embed those fields (US-35).

## Collections

### `seasons`

Replaces `PCKM_SEASON`.

| Field | Type | Required | Notes |
|---|---|---|---|
| `season` | string | yes | Four-digit year, e.g. `"2024"`. Unique |
| `beginDate` | date | yes | Calendar date. Begin year matches `season` |
| `endDate` | date | yes | End year is `season` or `season + 1` |
| `isCurrent` | boolean | yes | Replaces the Derby `getCurrentSeason` native query |

Manual week CRUD (US-05) still requires week 1 begin = season begin.

### `seasonWeeks`

Replaces `PCKM_SEASON_WEEK`. Query by `seasonId`.

| Field | Type | Required | Notes |
|---|---|---|---|
| `seasonId` | string | yes | Parent `seasons` document id |
| `weekNumber` | number | yes | Unique per season |
| `beginDate` | date | yes | |
| `endDate` | date | yes | |

**Manual CRUD:** begin date is Thursday; end date is Wednesday = begin + 6
days; week 1 begin equals season begin; later weeks begin = season begin +
`(weekNumber - 1) * 7`.

**CFBD import (US-38):** upserts weeks from `GET /calendar` (week number,
start/end). Imported weeks are **not** subject to the Thursday–Wednesday rule.

### `venues`

Replaces `PCKM_VENUE`.

| Field | Type | Required | Notes |
|---|---|---|---|
| `venueName` | string | yes | ≤ 60 |
| `cityState` | string | yes | ≤ 60 |
| `cfbdVenueId` | number | no | Unique when present. CFBD `venueId` for import matching |

A seeded **Unknown venue** is used when CFBD import cannot resolve a home
venue for a newly created non-conference opponent.

### `teams`

Replaces `PCKM_TEAM`. Embeds a home-venue snapshot and keeps `homeVenueId` for
edits.

| Field | Type | Required | Notes |
|---|---|---|---|
| `teamName` | string | yes | ≤ 40, unique |
| `squadName` | string | yes | ≤ 40 |
| `conferenceMember` | boolean | yes | The 16 SEC schools are `true` |
| `homeVenueId` | string | yes | `venues` document id |
| `homeVenue` | map | yes | Snapshot `{ id, name, cityState }` |
| `cfbdTeamId` | number | no | Unique when present. Required on the 16 SEC teams before import |

The 16 conference members must exist (seeded) with a `cfbdTeamId` that matches
CFBD `homeId` / `awayId`. Missing non-conference opponents are auto-created
with `conferenceMember=false`, `squadName` defaulting to the CFBD team name,
`cfbdTeamId`, and a home venue (CFBD venue if present, else Unknown venue).

### `rivalries`

Replaces `PCKM_RIVALRY`. Embeds both team snapshots. `team1.id` ≠ `team2.id`.

| Field | Type | Required | Notes |
|---|---|---|---|
| `rivalryName` | string | yes | ≤ 60 |
| `team1Id` | string | yes | |
| `team2Id` | string | yes | |
| `team1` | map | yes | Snapshot `{ id, name }` |
| `team2` | map | yes | Snapshot `{ id, name }` |

### `matchups`

Replaces `PCKM_MATCHUP` plus the denormalized `Test.sql` / `MatchupUserPick`
join. This is the read-heavy document for the picks grid.

| Field | Type | Required | Notes |
|---|---|---|---|
| `seasonId` | string | yes | |
| `seasonWeekId` | string | yes | |
| `weekNumber` | number | yes | Denormalized from the week |
| `matchupDate` | date | yes | Must fall in the week's `[beginDate, endDate]` |
| `homeTeamId` | string | yes | ≠ `awayTeamId` |
| `awayTeamId` | string | yes | |
| `homeTeam` | map | yes | Snapshot `{ id, name, squad }` |
| `awayTeam` | map | yes | Snapshot `{ id, name, squad }` |
| `homeTeamScore` | number \| null | no | Null until scored |
| `awayTeamScore` | number \| null | no | Null until scored; both null or both set |
| `winningTeamId` | string \| null | computed | Server-side only. Set when one score is strictly greater |
| `venueId` | string | yes | |
| `venue` | map | yes | Snapshot `{ id, name, cityState }` |
| `rivalryName` | string | no | Filled when the two teams already have a rivalry |
| `cfbdGameId` | number | no | Unique when present. Idempotent CFBD upsert key |

**Ties are invalid.** Both scores set and equal is a 400. `winningTeamId` is
undefined until a strict home or away winner exists. College football has no
ties.

CFBD import upserts by `cfbdGameId`. It does not write picks and does not set
scores for uncompleted games. Score sync writes `homeTeamScore` /
`awayTeamScore` only when CFBD marks the game `completed` with both
`homePoints` and `awayPoints`.

### `picks`

Replaces `PCKM_USER_PICK`. One document per user-per-matchup.

| Field | Type | Required | Notes |
|---|---|---|---|
| `userId` | string | yes | Firebase `uid`; must equal the authenticated user on write |
| `matchupId` | string | yes | Unique together with `userId` |
| `seasonId` | string | yes | Denormalized for leaderboard / week queries |
| `seasonWeekId` | string | yes | |
| `pickedTeamId` | string | yes | Must be the matchup's home or away team |
| `rank` | number | yes | 1..N where N = conference team count. Unique per user per week |

Empty picks (no `pickedTeamId`) are not persisted. Picks **lock** when
`now >= week.beginDate`; saving that week is rejected. Ranks for a user in a
week must be unique.

Scoring: a correct pick earns its `rank` when both scores exist and
`pickedTeamId == winningTeamId`. Otherwise 0.

### `users`

Replaces `PCKM_USER`. Document id = Firebase `uid`. **No password.**

| Field | Type | Required | Notes |
|---|---|---|---|
| `uid` | string | yes | Same as document id |
| `emailAddr` | string | yes | From Google; refreshed on every sign-in |
| `firstName` | string | yes | From Google; refreshed on every sign-in |
| `lastName` | string | yes | From Google; refreshed on every sign-in |
| `nickName` | string | yes | Player-owned, ≤ 40. Not overwritten on sign-in |
| `themeId` | string | yes | Theme key (`light`, `dark`, `alabama`, …). Default `light` |
| `roles` | string[] | yes | Mirror of Firebase custom claims. At least one of `player`, `manager` |

First Google sign-in creates the document with `roles: ['player']` and sets
the `player` custom claim. Repeat sign-in refreshes `emailAddr` / `firstName`
/ `lastName` and leaves `nickName` and `themeId` unchanged. Managers are
elevated from the Users admin screen (Admin SDK `setCustomUserClaims`).

### `themes`

Replaces `PCKM_THEME`. Closed catalog of **18** palettes. Managers may
activate, deactivate, and rename; they cannot add a key that is not compiled
into CSS.

| Field | Type | Required | Notes |
|---|---|---|---|
| `themeName` | string | yes | ≤ 40, unique. Display name |
| `themePath` | string | yes | Theme key, ≤ 100, must not begin with `/`. One of the 18 keys |
| `active` | boolean | yes | Inactive themes are hidden from the account picker |
| `primary` | string | yes | Hex, seeded from [`sec-theme-colors.md`](./sec-theme-colors.md) |
| `secondary` | string | yes | Hex |

Document id may be the theme key. Seed writes all 18 rows (Light, Dark, 16 SEC
schools). Hex values and keys: [`sec-theme-colors.md`](./sec-theme-colors.md).

## Denormalized snapshots

Firestore has no joins. Read-heavy documents embed the fields they display:

| Document | Embedded snapshot | Source of truth |
|---|---|---|
| `teams.homeVenue` | `{ id, name, cityState }` | `venues` |
| `rivalries.team1` / `team2` | `{ id, name }` | `teams` |
| `matchups.homeTeam` / `awayTeam` | `{ id, name, squad }` | `teams` |
| `matchups.venue` | `{ id, name, cityState }` | `venues` |
| `matchups.weekNumber` | number | `seasonWeeks` |
| `matchups.rivalryName` | string | `rivalries` (optional) |
| `picks.seasonId` / `seasonWeekId` | ids | `matchups` / `seasonWeeks` |

`homeVenueId`, `team1Id` / `team2Id`, `homeTeamId` / `awayTeamId`, and
`venueId` stay on the document so edits and deletes can find references.
Saving a team or venue rewrites every snapshot that points at it.

## CFBD external ids

| Field | Collection | CFBD source |
|---|---|---|
| `cfbdTeamId` | `teams` | `homeId` / `awayId` |
| `cfbdVenueId` | `venues` | `venueId` |
| `cfbdGameId` | `matchups` | game `id` |

Unique when present so import can match without relying on display names.
The 16 SEC teams must already exist with `cfbdTeamId` before a season import.

## Composite indexes (US-39)

Indexes live in `firestore.indexes.json` and are applied to the emulator and
the GCP project in US-39. Expected compound queries:

| Collection | Fields |
|---|---|
| `picks` | `userId` + `seasonId` |
| `picks` | `userId` + `seasonId` + `seasonWeekId` |
| `matchups` | `seasonId` + `seasonWeekId` |
| `seasonWeeks` | `seasonId` + `weekNumber` |
| `teams` | `cfbdTeamId` (single-field uniqueness / lookup) |
| `matchups` | `cfbdGameId` |
| `venues` | `cfbdVenueId` |

## Operational data (not a domain collection)

CFBD import job status (US-38) may be stored in a separate `importJobs`
collection or an equivalent task store. It is not one of the nine domain
collections and is not client-readable.
