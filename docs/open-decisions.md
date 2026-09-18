# Open decisions and decided behaviors

Recorded so backend, frontend, seed, and CFBD work share one source of truth.
Assumed defaults come from [`modernization-plan.md`](./modernization-plan.md)
(*Open decisions*, *Decided behaviors*) and [`user-stories.md`](./user-stories.md)
(*Assumptions*). Do not reopen these in a later story without updating this
file.

## Assumed defaults (confirm at execution)

| Decision | Choice |
|---|---|
| Google sign-in scope | **Open.** Any Google account may sign in. First sign-in auto-assigns the `player` claim and creates a `users` doc. |
| Default theme | **Light** (`themeId=light`). `prefers-color-scheme` may later switch the Light/Dark pair on first visit; it is not the default. |

## CFBD (new product scope)

| Decision | Choice |
|---|---|
| Which games `conference=SEC` returns | **Every game with an SEC team on either side**, including non-conference opponents. Not SEC-vs-SEC only. |
| When scores are written | **Only when CFBD marks the game `completed`** with both `homePoints` and `awayPoints`. No live in-progress scoring. |
| Season type | Regular season is the default; postseason is an optional import flag. |
| Weeks | Import upserts `seasonWeeks` from `GET /calendar`. Thursday–Wednesday rules apply to **manual** week CRUD only. |
| Import execution | Background job + UI poll (202 + job id). Not a single synchronous BFF POST. |
| Missing opponents / venues | Auto-created (`conferenceMember=false`; Unknown venue if needed). The 16 SEC teams must already exist with `cfbdTeamId`. |
| Game-day timezone | `America/New_York`. |
| API key | Server-only (`CFBD_API_KEY`). Never in the browser. Calls only from Spring to `https://api.collegefootballdata.com`. |

## Other decided behaviors

| Decision | Choice |
|---|---|
| Pick lock | Once a week's `beginDate` is today or past, that week's picks cannot be saved. Results show for current/past weeks only. |
| Ties | Not allowed. Equal home/away scores are 400. `winningTeamId` requires a strict winner. |
| Themes | Closed set of 18 compiled palettes (Light, Dark, 16 SEC). Admin activate / deactivate / rename only. |
| Snapshots | Saving a team or venue refreshes denormalized fields on matchups / rivalries / teams. |
| Deletes | Blocked (409) while children or references exist. No cascade of picks. |
| Google profile | Each sign-in refreshes email and name; `nickName` and `themeId` stay user-owned. |
| Hosting | Next.js is the only public origin. Spring is internal. An Application Load Balancer is not required. |
| Auth cookie | `__session` HttpOnly session cookie via Firebase Admin `createSessionCookie`. Not a per-request Bearer token. |
