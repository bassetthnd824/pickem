# API contract

Source of truth for the REST surface. Companion to
[`modernization-plan.md`](./modernization-plan.md) (*REST surface*, *Frontend
BFF*) and [`firestore-data-model.md`](./firestore-data-model.md).

## Call path

**The browser calls Next.js `/api` only.** Next.js App Router handlers
**Axios-forward** each request (method, query, JSON body, and the incoming
`__session` cookie) to Spring Boot. Spring has **no public URL** — locally it
listens on loopback (`http://localhost:8080`); in GCP it is an internal Cloud
Run service. There is no browser CORS on Spring.

```
Browser  ── same-origin /api/* ──►  Next.js (public)
                                      │  Axios + Cookie: __session
                                      │  (+ Cloud Run IAM token in GCP)
                                      ▼
                                    Spring Boot (not reachable outside GCP)
```

- Browser client: relative `/api/...` with `credentials: 'include'`. Never a
  Spring or `*.run.app` base URL.
- BFF allowlist (US-33): `/api/auth/**`, `/api/game/**`, `/api/manager/**`
  only. Paths outside that list are not proxied.
- `/api/internal/**` is **not** browser-callable. Cloud Scheduler OIDC or a
  shared secret; never the user session cookie.
- Session cookie `__session`: `HttpOnly; Secure; SameSite=Lax` (or `Strict`);
  **host-only** on the Next.js hostname (`Path=/`; do not set `Domain=`).
- Dates on the wire are **ISO-8601**: calendar dates `YYYY-MM-DD`, timestamps
  RFC 3339. Replaces `DateConverter` / `DateSerializerDeserializer`.

Roles come from Firebase custom claims, mirrored on `users.roles[]`.

| Role | May call |
|---|---|
| *(none)* | `POST /api/auth/session` only |
| `player` | `/api/auth/**`, `/api/game/**` |
| `manager` | `/api/auth/**`, `/api/game/**`, `/api/manager/**` |

Unauthenticated requests to `/api/game/**` and `/api/manager/**` are rejected.
Players are blocked from `/api/manager/**`.

---

## `/api/auth`

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/session` | none (Google ID token in body) | Verify ID token, provision/refresh `users`, mint `__session` cookie (~5–14 days) |
| `POST` | `/api/auth/logout` | session | Clear `__session` (and optionally revoke refresh tokens) |
| `GET` | `/api/auth/me` | session | Current profile + roles for `AuthProvider` |

`POST /api/auth/session` body: `{ "idToken": "<Firebase ID token>" }`.
First sign-in creates `users/{uid}` with email/name from Google,
`themeId=light`, `roles: ['player']`. Repeat sign-in refreshes `emailAddr`,
`firstName`, `lastName` and leaves `nickName` / `themeId` unchanged.

No registration endpoint. No password fields.

---

## `/api/game`

All endpoints require a signed-in user (`player` or `manager`).

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/game/main` | session | Current season weeks + the user's matchup/pick grid + `numberOfConferenceTeams` |
| `POST` | `/api/game/picks` | session | Bulk-upsert the authenticated user's weekly picks |
| `GET` | `/api/game/leaderboard` | session | Season leaderboard. Query: `seasonId` (default current) |
| `GET` | `/api/game/team-schedule` | session | Conference team W/L schedule. Query: `teamId`, `seasonId` |
| `GET` | `/api/game/account` | session | Profile: email, name, `nickName`, `themeId` |
| `PUT` | `/api/game/account` | session | Update `nickName` (required, ≤ 40) and `themeId` only |
| `GET` | `/api/game/teams` | session | Conference-member list for the team-schedule dropdown. Query: `conferenceMember=true` |

### Picks

`POST /api/game/picks` body: `{ "seasonWeekId": "...", "picks": [ { "matchupId", "pickedTeamId", "rank" } ] }`.

- Rows with no `pickedTeamId` are dropped.
- Ranks must be unique for that user and week; 1..N where N is conference team count.
- Saving a week whose `beginDate` is today or past is 409/400 (picks locked).
- A player cannot write another user's picks.

`GET /api/game/main` returns denormalized teams, venue, rivalry, scores,
existing pick/rank, and conference-team count. Future weeks hide scores and
result icons; current/past weeks show them.

### Leaderboard

Correct pick adds its rank; incorrect or unscored add 0. Sort score
descending, tie-break `userId` ascending. Users with no picks appear with
score 0. Response columns: rank, nickname, score.

### Team schedule

Away games prefix the opponent with `"at "`. Unplayed games omit W/L and
score result.

### Account

Email and name are read-only (Google). Password fields are rejected.

---

## `/api/manager`

All endpoints require the `manager` claim. Standard REST CRUD:

`GET` list (search-by-example query params) · `GET /{id}` · `POST` create ·
`PUT /{id}` update · `DELETE /{id}` (409 if referenced).

Audit (`createDate` / `createUser` / `lastUpdateDate` / `lastUpdateUser`) and
`version` are set on write. Stale `version` is rejected.

### Reference data

| Method | Path | Purpose |
|---|---|---|
| *CRUD* | `/api/manager/seasons` | Seasons. `season` unique; begin year matches season year |
| *CRUD* | `/api/manager/season-weeks` | Weeks. Query `seasonId` replaces `seasonWeeks_getSeasonWeeksBySeason` |
| `GET` | `/api/manager/seasons/{id}` | Replaces `seasons_getSeasonById` |
| `GET` | `/api/manager/season-weeks/{id}` | Replaces `seasonWeeks_getSeasonWeekById` |
| *CRUD* | `/api/manager/venues` | Venues. Optional unique `cfbdVenueId` |
| *CRUD* | `/api/manager/teams` | Teams. Embed `homeVenue`. Optional unique `cfbdTeamId` |
| `GET` | `/api/manager/teams/{id}` | Includes home venue (replaces `teams_getTeamById` auto-fill) |
| `GET` | `/api/manager/teams?conferenceMember=true` | Replaces `TeamBean.getConferenceTeams` |
| *CRUD* | `/api/manager/rivalries` | Rivalries. Two different teams; name ≤ 60 |
| *CRUD* | `/api/manager/matchups` | Matchups. Optional unique `cfbdGameId`. `winningTeamId` is derived, not client-set |
| *CRUD* | `/api/manager/users` | List/search by email, first, last. Edit name/email/nickname/theme/roles. No passwords |
| *CRUD* | `/api/manager/themes` | Name, key/`themePath` (no leading `/`), `active`. Closed set of 18 keys |

Manual week CRUD still requires Thursday begin and Wednesday = begin + 6.
Imported CFBD weeks are not subject to that day-of-week rule.

Matchup create/update rejects equal home and away teams, dates outside the
week, and `homeTeamScore == awayTeamScore` when both are set (400). Scores may
be null (unplayed) or both set.

Updating user roles to include `manager` sets the Firebase custom claim;
removing it clears the claim. Zero roles is 400. POST of a theme key that is
not one of the compiled 18 is 400.

### CFBD import and score-sync

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/manager/imports/matchups` | manager | Enqueue a season import job. Body: `{ "seasonId", "seasonType"? }`. Returns **202** + `{ "jobId" }` |
| `GET` | `/api/manager/imports/{jobId}` | manager | Poll job status and summary (created / updated / skipped with reasons) |
| `POST` | `/api/manager/imports/scores` | manager | Optional UI "Refresh scores" for a season. Same worker as the game-day job |

`POST /api/manager/imports/matchups` must return quickly. The worker (not the
BFF request) calls CFBD `GET /games?year={season}&conference=SEC` (default
`seasonType=regular`) and upserts weeks from `GET /calendar`.
`conference=SEC` includes every game with an SEC team on either side
(non-conference opponents too), not only SEC-vs-SEC.

The catch-all BFF does not expose the worker.

---

## Internal jobs (not browser-callable)

These paths are **outside** the BFF allowlist. Invoked with Cloud Scheduler
OIDC or a shared secret (loopback locally). Not the `__session` cookie.

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/internal/jobs/cfbd-scores` | OIDC / shared secret | Game-day score sync. Preferred production entry: Next.js forwards this to Spring |
| `POST` | `/api/internal/jobs/cfbd-matchups` | OIDC / shared secret | Import worker. Not started by the browser |

Score job: current-season matchups whose `matchupDate` is today in
`America/New_York` (or whose CFBD game is still not completed after a kickoff
that spilled past midnight). Writes scores only when CFBD `completed=true`
with both `homePoints` and `awayPoints`. No-op on days with no matchups.
Idempotent. Cloud Scheduler: hourly Thursday–Monday 12:00–23:00 ET during the
current season; can be disabled out of season.

---

## Health (not proxied to Spring)

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/health` | none | Spring process up (Cloud Run probe). Inside GCP only |
| `GET` | `/health` (Next.js) | none | UI process up. Does **not** call Spring |

Documented here for operators; owned by US-40. Not on the BFF allowlist as a
Spring proxy.

---

## Error shape

| Status | When |
|---|---|
| 400 | Validation failure (including ties, zero roles, unknown theme key, bad dates) |
| 401 | Missing / expired / revoked session |
| 403 | Authenticated but missing `manager` for `/api/manager/**`; browser hitting `/api/internal/**` |
| 409 | Stale `version`; pick lock; delete blocked by references |
| 202 | Import job accepted |
| 5xx | CFBD or persistence failure after retries; Scheduler may retry jobs |

Validation messages follow legacy `MessageResources.properties` intent (US-22).
