# Modernize "Kenney's Pickem" — Struts2/EJB → Next.js + Spring Boot/Firestore

## Context

`pickem` is a season-long college-football **confidence pick'em** game. Each week a
player picks a winner for every matchup and assigns each pick a *rank* (confidence
points, 1..N where N = number of conference teams). A correct pick earns its rank in
points; the leaderboard ranks players by total points. Managers run a back-office that
CRUDs the reference/schedule data (seasons, weeks, teams, venues, rivalries, matchups,
users, themes).

Today it is a single Maven WAR: **Struts 2 (2.3.24)** actions + Apache Tiles 3 JSP views
+ Bootstrap 3/jQuery, a **CDI/EJB** service layer (`@Stateless` beans extending a generic
Hibernate DAO), **JPA/Hibernate** entities over **Apache Derby**, and container-managed
**JAAS FORM** auth on WildFly. It targets Java 7 and requires a full JBoss/WildFly install.

The goal is a modern, cloud-native rebuild with the same feature set:
- **Frontend:** Next.js (App Router) + React, TanStack Form, Valibot validation, Tailwind CSS,
  with a switchable **theming system** (Light, Dark, + one per SEC school).
- **Backend:** Spring Boot REST API persisting to **Firestore** (native mode).
- **Auth:** Firebase Authentication with the **Google** provider (OAuth sign-in), roles via custom claims, replacing JAAS. No email/password, no self-managed passwords. Sessions are held in an **HttpOnly session cookie** (Firebase Admin `createSessionCookie`), not a per-request Bearer token.
- **Data:** schema-only — model the collections; no migration of existing Derby data.
- **Layout:** **Nx** monorepo — Next.js via `@nx/next`, the Spring Boot **Maven** backend via
  the third-party `@jnxplus/nx-maven` plugin. **Hosting:** the **Next.js** Cloud Run service
  is the only public origin. It exposes App Router **API routes** that **Axios**-forward
  each browser call (with the session cookie) to Spring Boot. Spring Cloud Run is **not**
  reachable from outside GCP, so an Application Load Balancer is **not required**.

This is a **greenfield rewrite that preserves behavior**, not an in-place refactor. The
existing Java/JSP tree is the functional spec; nothing in `src/main` is modified.

---

## Target repository layout (Nx monorepo)

An **Nx** workspace manages both apps under a single graph, tasks, and CI. The Next.js app
uses the official **`@nx/next`** plugin; the Spring Boot backend is a **Maven** project
integrated with the third-party **`@jnxplus/nx-maven`** plugin (the actively maintained
successor to the deprecated `@jnxplus/nx-boot-maven`), so `nx build/test/serve backend`
delegate to Maven and the backend joins the Nx project graph.

```
pickem/                      # Nx workspace root (nx.json, package.json, tsconfig.base.json)
  apps/
    frontend/                # Next.js App Router (@nx/next) — TS, Tailwind, TanStack Form, Valibot
    backend/                 # Spring Boot REST API (Maven, @jnxplus/nx-maven) + Firestore
  libs/
    shared-schemas/          # (optional) shared Valibot schemas / TS DTO types for the frontend
  tools/
    seed/                    # Firestore seed script (from import.sql.old) + theme docs
  docs/                      # architecture, Firestore data model, API contract, sec-theme-colors.md
  nx.json, project.json(s)   # Nx config; CI via GitHub Actions or Cloud Build using nx affected
  (legacy Struts app remains under src/ until parity is confirmed, then removed)
```

**Nx setup notes:**
- Bootstrap with `@jnxplus/nx-maven:init` (Java 21, Spring Boot parent POM), then generate
  the backend app; add the Next.js app with `@nx/next`. `nx-maven` builds the Maven module
  and adds it to the graph automatically; set `skipProjectWithoutProjectJson: true` to keep
  the legacy `src/` Maven POM out of the Nx graph during the transition.
- Use `nx affected` in CI to build/test only what changed; Nx caching speeds both stacks.
- Keep the legacy `src/` tree in place during the build as a reference; delete it in a final
  cleanup commit once parity is verified.

---

## Data model — Firestore collections

Relational tables become collections. Firestore has no joins, so we **denormalize** read-
heavy references (team names/squad, venue) onto documents that are displayed together, and
compute aggregates (leaderboard) in the service layer. IDs are Firestore auto-IDs (string);
the old `PCKM_*_SEQ` sequences and `_` metamodel classes go away.

| Collection | Key fields | Notes / denormalization |
|---|---|---|
| `seasons` | `season` (e.g. "2024"), `beginDate`, `endDate`, `isCurrent` | replaces `getCurrentSeason` native query with an `isCurrent`/date query |
| `seasonWeeks` | `seasonId`, `weekNumber`, `beginDate`, `endDate` | query by `seasonId` |
| `venues` | `venueName`, `cityState` | |
| `teams` | `teamName`, `squadName`, `conferenceMember`, `homeVenue{id,name,cityState}` | embed venue snapshot; keep `homeVenueId` for edits |
| `rivalries` | `rivalryName`, `team1{id,name}`, `team2{id,name}` | |
| `matchups` | `seasonId`, `seasonWeekId`, `weekNumber`, `matchupDate`, `homeTeam{id,name,squad}`, `awayTeam{id,name,squad}`, `homeTeamScore`, `awayTeamScore`, `venue{...}`, `rivalryName?` | denormalizes the big `Test.sql` join; `winningTeamId` computed server-side |
| `picks` | `userId`, `matchupId`, `seasonId`, `seasonWeekId`, `pickedTeamId`, `rank` | one doc per user-per-matchup; query by `userId`+`seasonId` |
| `users` | `uid` (Firebase), `emailAddr`, `firstName`, `lastName`, `nickName`, `themeId`, `roles[]` | **no password** (Google/Firebase owns identity); doc id = Firebase `uid`. `emailAddr`/name seeded from the Google profile on first sign-in |
| `themes` | `themeName`, `themePath`, `active` | |

Groups (`manager`/`player`) become **Firebase custom claims** + a `roles[]` mirror on the
user doc — the `PCKM_GROUP`/`PCKM_USER_GROUP` tables are dropped.

**Audit fields** (`createDate/createUser/lastUpdateDate/lastUpdateUser`, `@Version`) carry
over as document fields set by a shared base mapper; optimistic locking uses a `version`
field checked in a Firestore transaction. Composite **indexes** (picks by `userId`+
`seasonId`, matchups by `seasonId`+`seasonWeekId`, etc.) live in `firestore.indexes.json`
and are applied to the emulator and the GCP project.

Every custom bean-validation rule is reproduced (see Validation section).

---

## Backend — Spring Boot API (`apps/backend/`)

**Stack:** Spring Boot 3.x, Java 21, **Maven** (integrated via `@jnxplus/nx-maven`);
`spring-cloud-gcp-starter-firestore` (or the Google `google-cloud-firestore` SDK),
`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`,
Firebase Admin SDK for session-cookie verification. Springdoc for OpenAPI.

**Layering** (mirrors the old action→bean→entity split):
- `config/` — Firestore client bean, Firebase Admin init, security filter chain. No
  browser CORS: the browser never calls Spring.
- `security/` — a `SessionCookieFilter` reads the **`__session` HttpOnly cookie** (forwarded
  by the Next.js BFF) and verifies it with Firebase Admin
  `verifySessionCookie(cookie, checkRevoked=true)`, populating `SecurityContext` with roles
  from the decoded custom claims. `@PreAuthorize("hasRole('MANAGER')")` guards
  `/api/manager/**`; authenticated players hit `/api/game/**`. This replaces `web.xml`
  security-constraints + `LoginFilter`. Spring is not browser-facing, so **browser CSRF
  against Spring is N/A**; lock the API down with Cloud Run IAM (Next.js service account
  `roles/run.invoker`) plus the forwarded session cookie. SameSite on the cookie still
  protects the public Next.js origin.
- `repository/` — one repository per collection over the Firestore SDK, replacing the
  `@Stateless` beans + `GenericHibernateBean`. A `BaseRepository<T>` provides
  `findById/findAll/query/save/delete` + audit/version stamping (replaces `BaseAction`
  audit logic and the generic DAO).
- `service/` — domain logic ported from the actions/beans:
  - `MatchupService` — replaces `MatchupBean`: `getMatchupsBySeason`, `getMatchupsBySeasonTeam`
    (team schedule W/L), and `getMatchupUserPicks(userId, seasonId)` — done by fetching the
    season's matchups + the user's picks and joining **in code** (no SQL).
  - `PickService` — replaces `MainAction.save` + `UserPickBean.persistList`: bulk upsert of a
    user's weekly picks in a batched Firestore write; drops empty picks.
  - `LeaderboardService` — replaces `LeaderBoardAction`/`getLeaderBoardForSeason`: the
    **scoring engine**. Loads season matchups (→ `winningTeamId` per matchup) and all picks
    for the season, sums `rank` where `pickedTeamId == winningTeamId` per user, sorts desc
    (tie-break by userId). Preserve the `MatchupUserPick.getScore()` / `isCurrentOrPastWeek()`
    rules. **Picks lock when `weekBeginDate` is today or past** (cannot save that week).
    Scores/results show only for current and past weeks. **Ties are not valid** in college
    football: both scores set and equal is a 400; `winningTeamId` is undefined until a
    strict home or away winner exists.
  - `SeasonService`, `SeasonWeekService`, `TeamService`, `VenueService`, `RivalryService`,
    `ThemeService`, `UserService`, `AccountService` — port the corresponding actions/beans
    (no `RegistrationService`; Google sign-in provisions users).
- `web/` — REST controllers returning JSON (replaces JSP/Tiles + the custom `JSONResult`).

**REST surface** (namespaces map: `/game/*`→`/api/game`, `/manager/*`→`/api/manager`).
The **browser never calls these URLs**. It calls the same paths on the Next.js origin;
Next.js Route Handlers Axios-proxy them to Spring (see Frontend BFF).

- `POST /api/auth/session` — the **session-login** endpoint. The Next.js BFF forwards the
  Google **ID token** here once. The backend verifies it, provisions the `users` doc on
  first sign-in (email/name from the Google profile) + assigns the `player` claim if new,
  then mints a **session cookie** (`createSessionCookie`, ~5–14 day expiry) and returns
  `Set-Cookie`. Next.js copies that cookie onto the browser response (`HttpOnly; Secure;
  SameSite`, host-only on the Next.js hostname). Replaces `RegisterAction` + client login
  (no registration form).
- `POST /api/auth/logout` — clears the session cookie (and optionally revokes refresh
  tokens). Replaces `LogoutAction`.
- `GET /api/auth/me` — returns the current user profile + roles for the frontend
  `AuthProvider` to hydrate from the cookie.
- `GET/PUT /api/game/account` — profile only (nickName, theme). No password fields — Google
  owns the credential, so `AccountAction`'s old/new/confirm-password flow is removed.
- `GET /api/game/main` — current season weeks + the user's matchups/picks grid + `numberOfConferenceTeams`.
- `POST /api/game/picks` — save weekly picks.
- `GET /api/game/leaderboard?seasonId=` — leaderboard.
- `GET /api/game/team-schedule?teamId=&seasonId=` — replaces `teamSchedule_get` JSON.
- `/api/manager/{seasons|season-weeks|teams|venues|rivalries|matchups|users|themes}` —
  standard REST CRUD (list/get/create/update/delete). Cascading helpers
  (`getSeasonWeeksBySeason`, `getTeamById` venue auto-fill) become plain GET endpoints.

**Dates:** replace `DateConverter`/`DateSerializerDeserializer` with ISO-8601 JSON
(`java.time.LocalDate`/`Instant`).

---

## Frontend — Next.js App Router (`apps/frontend/`)

**Stack:** Next.js (App Router) + TypeScript, Tailwind CSS, TanStack Form, Valibot,
TanStack Query (server-state/data fetching), Firebase JS SDK (auth), **Axios on the
server** (BFF proxy only — not in client bundles). This replaces JSP, Tiles, jQuery,
Underscore templates, Bootstrap, and the datetimepicker.

**Routing / layout** (Tiles `template.jsp` → root layout; navbar → shared component):
```
app/
  (auth)/login                       # single "Sign in with Google" button; replaces login.jsp + registration.jsp (no register route)
  (game)/main                        # weekly picks grid (main.jsp + main.js reorder/rank)
  (game)/leaderboard                 # leaderBoard.jsp
  (game)/team-schedule               # teamSchedule.jsp + AJAX → server component/Query
  (game)/account                     # account.jsp
  (manager)/{seasons,season-weeks,teams,venues,rivalries,matchups,users,themes}
                                     # list + detail routes; replaces searchTemplate.jsp accordion CRUD
  api/[...path]/route.ts             # BFF: Axios-proxy /api/* to Spring with the session cookie
```
- **BFF proxy (required):** the browser must **not** call Spring. Client code uses
  same-origin relative `/api/...` (`credentials: 'include'`). App Router Route Handlers
  receive the request, and **Axios** (server-side) forwards method, query, body, and the
  incoming `__session` cookie to Spring (`SPRING_API_BASE_URL`, e.g. the internal Cloud Run
  URL locally `http://localhost:8080`). Axios also forwards Spring’s status, JSON body, and
  `Set-Cookie` back to the browser, rewriting the cookie as **host-only** on the Next.js
  origin (drop any `Domain=` Spring might set; force `Path=/`). Local `nx serve` uses the
  same proxy so the browser still never talks to Spring — no CORS. The catch-all **must
  allowlist** `/api/auth`, `/api/game`, `/api/manager` only. `/api/internal/**` is not
  browser-callable: OIDC or a shared secret, never the user session cookie. Locally Spring
  accepts a shared secret (or open loopback) instead of a Cloud Run ID token.
- **Auth guard:** Next.js middleware reads the **session cookie** (server-side) to gate
  `(game)` (any signed-in user) and `(manager)` (custom claim `manager`), replacing the
  servlet security constraints. A client `AuthProvider` hydrates from `GET /api/auth/me`
  (which the BFF forwards). The raw ID token is used only once at
  `POST /api/auth/session` on the Next.js origin.
- **Picks screen:** the up/down reorder + rank renumber logic from `main.js` becomes React
  state; radio pick selection + confidence ordering; correct/incorrect check/cross icons.
- **Manager CRUD:** a reusable list/detail pattern replaces `searchTemplate.jsp` + generic
  `BaseAction`. Cascading selects (season→week, team→venue auto-fill) use TanStack Query
  against the helper GET endpoints — replaces `adminscreen.js` AJAX + double-select pattern.
- **Forms & validation:** TanStack Form + **Valibot schemas** shared across all forms.
- **Dates:** native `<input type="date">` / a React date picker (replaces Bootstrap datetimepicker + moment.js).
- **Styling:** Tailwind; port `pickem.css` intent. The per-user `Theme` entity becomes a
  live, in-scope feature — see **UI Color Themes** below.

---

## UI Color Themes

The legacy `Theme` entity (`themeName`, `themePath`, `active`) is realized as a real,
switchable theming system. Ship **18 themes**: a generic **Light** and **Dark**, plus one
per **SEC school (16)** derived from that school's official team colors.

**Mechanism (Tailwind + CSS variables):**
- Define a fixed set of **semantic design tokens** as CSS custom properties:
  `--color-bg`, `--color-surface`, `--color-text`, `--color-muted`, `--color-border`,
  `--color-primary`, `--color-primary-fg`, `--color-secondary`, `--color-accent`,
  plus state colors (`--color-success`/`--color-danger` for the correct/incorrect pick icons).
- `tailwind.config` maps color utilities to these variables
  (e.g. `primary: 'rgb(var(--color-primary) / <alpha-value>)'`), so all components use
  semantic classes (`bg-surface`, `text-primary`) and never hard-code a hex.
- Each theme is a block of variable values scoped by attribute selector on `<html>`:
  `[data-theme="alabama"] { --color-primary: …; --color-secondary: …; … }`, with `light`
  and `dark` as the baseline two.
- **Applying a theme:** the user's chosen theme key is stored on their `users` doc
  (`themeId` → a theme key like `light`/`dark`/`alabama`). The root layout / `AuthProvider`
  sets `data-theme` on `<html>` (SSR-friendly to avoid flash). Default = `light` (optionally
  follow `prefers-color-scheme` for the Light/Dark pair on first visit).
- A **theme picker** on the Account screen lets a user choose their theme from the **closed
  set of 18** (Light, Dark, 16 SEC). Managers may activate/deactivate and rename those
  seeded rows; they cannot add a palette that is not compiled into the CSS tokens. Each SEC
  theme uses the **primary** school color for headers/nav/buttons and the **secondary** as
  accent; neutral surfaces keep text readable.

**Accessibility rule:** every theme must meet **WCAG AA contrast** for text on surfaces and
for `--color-primary-fg` on `--color-primary`. Several SEC palettes are very dark (black
secondaries) — for those, derive a light neutral surface and use the team color as the
primary/accent rather than as the page background, and compute a readable foreground
(white/near-black) per swatch.

**SEC school colors** (official primary/secondary hex, from teamcolorcodes.com / school
brand guides) — the seed values for the 16 school themes. These go into
`docs/sec-theme-colors.md`; the seed script writes one `themes` document per school (name,
key/`themePath`, primary, secondary) plus Light and Dark.

| School | Theme key | Primary | Secondary / accent |
|---|---|---|---|
| Alabama Crimson Tide | `alabama` | Crimson `#9E1B32` | Cool Gray `#828A8F` |
| Arkansas Razorbacks | `arkansas` | Cardinal Red `#9D2235` | White `#FFFFFF` |
| Auburn Tigers | `auburn` | Navy Blue `#0C2340` | Orange `#E87722` |
| Florida Gators | `florida` | Blue `#0021A5` | Orange `#FA4616` |
| Georgia Bulldogs | `georgia` | Bulldog Red `#BA0C2F` | Black `#000000` |
| Kentucky Wildcats | `kentucky` | Wildcat Blue `#0033A0` | Black `#000000` |
| LSU Tigers | `lsu` | Purple `#461D7C` | Gold `#FDD023` |
| Mississippi State Bulldogs | `mississippi-state` | Maroon `#660000` | Gray `#75787B` |
| Missouri Tigers | `missouri` | Black `#000000` | MU Gold `#F1B82D` |
| Oklahoma Sooners | `oklahoma` | Crimson `#841617` | Cream `#FDF9D8` |
| Ole Miss Rebels | `ole-miss` | Red `#CE1126` | Navy Blue `#14213D` |
| South Carolina Gamecocks | `south-carolina` | Garnet `#73000A` | Black `#000000` |
| Tennessee Volunteers | `tennessee` | Tennessee Orange `#FF8200` | Smokey Gray `#58595B` |
| Texas Longhorns | `texas` | Burnt Orange `#BF5700` | Dark Gray `#333F48` |
| Texas A&M Aggies | `texas-am` | Maroon `#500000` | White `#FFFFFF` |
| Vanderbilt Commodores | `vanderbilt` | Black `#000000` | Old Gold `#866D4B` |

Notes: For **Missouri**, MU Gold is the distinctive color but black is the brand primary —
theme built as gold-accent on a dark/black primary. Where the secondary is effectively
**white** (Arkansas, Texas A&M), the theme derives a neutral surface and uses the primary as
the accent, keeping WCAG AA text contrast. Very dark primaries (Vanderbilt/Missouri black,
Texas A&M/South Carolina deep maroon/garnet) use a light neutral page surface with the team
color for nav/headers/buttons rather than as the full-page background.

---

## Validation — port every JSR-303 rule to Valibot (+ Spring)

Reproduce each custom constraint from `model/constraints` + `model/validators` as a Valibot
schema on the client **and** as Spring validation on the server (never trust the client):

- `PasswordsMatch`, `UserPassProperlyFormed` → **dropped**; Google/Firebase owns the
  credential, so there are no password forms to validate.
- `MatchupTeamsNotEqual`, `RivalryTeamsNotEqual` (home≠away / team1≠team2).
- `ValidMatchupDate`, `ValidSeasonBeginDate`/`EndDate`, `ValidSeasonWeekBeginDate`/`EndDate`
  (date-bounds/order; matchup within week, week within season).
- `ThemePathDoesNotBeginWithSlash`, `UserHasAtLeastOneGroup` (→ at least one role).
- Field-level `@NotBlank`/`@Size`/`@Email` from `Account`, `Registration`, and entities.

Error messages seed from `MessageResources.properties`.

---

## Auth flow (Firebase, replacing JAAS)

1. Client signs in via Firebase Auth **Google provider** (OAuth popup/redirect) → gets a
   short-lived **ID token**.
2. Client POSTs that ID token once to **Next.js** `POST /api/auth/session`. The BFF
   Axios-forwards it to Spring. Spring verifies the token, provisions the `users` doc on
   first sign-in (email/name from the Google profile) + sets the `player` claim if new,
   then mints a **session cookie**. Each sign-in **refreshes** `emailAddr`, `firstName`,
   and `lastName` from the Google profile; `nickName` and `themeId` stay user-controlled.
   Next.js copies `Set-Cookie` onto the browser response (`HttpOnly; Secure; SameSite`,
   host-only). Managers are elevated via the Users admin screen (Admin SDK sets the
   `manager` claim); the next session refresh picks up the new role. Next.js middleware
   **verifies** the session cookie with Firebase Admin (not mere presence).
3. Every subsequent browser request goes to Next.js on the same origin and sends the
   cookie automatically. The BFF attaches that cookie on the Axios call to Spring;
   `SessionCookieFilter` verifies it (`verifySessionCookie`, revocation-checked) and
   authorizes by claim. `POST /api/auth/logout` (via the BFF) clears the cookie. No
   passwords anywhere (drops the unsalted SHA-256 entirely). The browser has no URL it
   can use to reach Spring.

---

## Deployment — Google Cloud / Cloud Run (Next.js public, Spring internal)

The **browser talks only to Next.js**. Spring Boot is a GCP-internal service. Because the
UI and `/api` already share the Next.js origin, an **Application Load Balancer is not
required** (optional later for CDN / Cloud Armor / a custom domain in front of Next.js
only). Do not expose Spring on a public hostname or path-route `/api` at a load balancer
to Spring.

```
Browser  ──HTTPS──►  Cloud Run: frontend (Next.js)     ← only public origin
                       │  App Router UI
                       │  app/api/**  Axios + Cookie: __session
                       │  (+ IAM identity token)
                       ▼
                     Cloud Run: backend (Spring Boot)  ← not reachable outside GCP
```

- **Frontend Cloud Run (public):** Next.js; custom domain via Cloud Run domain mapping
  (managed cert). Firebase web config via env. Browser calls relative `/api`. The Next.js
  service account has `roles/run.invoker` on the backend service. Use **Direct VPC egress**
  or a serverless VPC connector so Axios can reach Spring when backend ingress is internal.
- **Backend Cloud Run (internal):** Spring Boot; Firestore native mode; ADC for Firestore +
  Firebase Admin; `CFBD_API_KEY` from Secret Manager. Ingress **`internal`** (not
  invokable from the public internet). `--no-allow-unauthenticated`. Only the Next.js
  service account (and Cloud Scheduler if it targets Spring directly) may invoke it.
  Axios from Next.js sends a Cloud Run ID token **and** the user’s `__session` cookie.
- **Session cookie:** issued through the BFF; **host-only** on the Next.js hostname
  (`Secure; HttpOnly; SameSite=Lax` or `Strict`; do not set `Domain=`).
- **CORS:** none. The browser never calls a different origin, locally or in production.
- **Cloud Scheduler** (game-day scores) stays inside GCP: either OIDC to a Next.js
  `POST /api/internal/jobs/cfbd-scores` route that Axios-forwards to Spring (keeps Spring
  fully internal), or OIDC to Spring if ingress/IAM allows that scheduler SA. Do **not**
  give the browser a Spring URL. The schedule can be disabled out of season.
- **CI** (`cloudbuild.yaml` or GitHub Actions): `nx affected` build/test, build both
  images, deploy both Cloud Run services with the ingress/IAM settings above.

---

## Seed script (`tools/seed/`)

Translate `src/main/resources/import.sql.old` (SEC teams, venues, rivalries, seasons,
weeks, sample matchups) — plus the **18 theme documents** (Light, Dark, and one per SEC
school from `docs/sec-theme-colors.md`) — into a Node or Java script that writes to Firestore. Runs
against the emulator for dev and once against the real project to bootstrap. No historical
pick data. Users are **not** seeded with passwords — they are provisioned on first Google
sign-in; the script only needs to grant the `manager` claim to a known bootstrap Google
account (by email) so there is an initial admin.

---

## Suggested build order

1. **Nx workspace** scaffolding (`@jnxplus/nx-maven:init` for the Maven backend + `@nx/next`
   for the frontend) + `docs/` data-model & API contract; Firestore rules skeleton.
2. Backend: Firestore config, `BaseRepository`, entities/DTOs, Firebase session-cookie filter.
3. Backend: reference-data services + manager CRUD controllers (seasons→…→matchups).
4. Backend: `MatchupService`, `PickService`, `LeaderboardService`, game endpoints.
5. Seed script; verify data against Firestore emulator.
6. Frontend: scaffold, Tailwind + **theming tokens/`data-theme` themes (Light/Dark + 16 SEC)**,
   **Axios BFF Route Handlers**, Firebase Google auth, session-cookie exchange,
   AuthProvider/guards, same-origin API client.
7. Frontend: game screens (main/leaderboard/team-schedule/account incl. **theme picker**),
   then manager CRUD.
8. Valibot + Spring validation parity pass; theme accessibility (WCAG AA) check.
9. Cloud Run deploy (public Next.js, **internal** Spring; no ALB required) + CI
   (`nx affected`, Nx cache); final cleanup removing legacy `src/`.

---

## Verification

- **Backend unit/slice tests** against the **Firestore emulator** for each service —
  especially `LeaderboardService` (scoring parity vs the old `getLeaderBoardForSeason` /
  `Test.sql` logic) and `PickService` (bulk save, empty-pick drop, week lock).
- **API contract tests** (Springdoc/OpenAPI) covering every endpoint + auth role gating
  (player blocked from `/api/manager/**`, unauthenticated blocked from `/api/game/**`).
- **Frontend:** component/e2e (Playwright, against the Firebase Auth emulator's Google
  provider) for first-sign-in provisioning → make picks → save → see leaderboard,
  team-schedule AJAX, and one full manager CRUD cycle with cascading selects.
- **Validation parity:** table-check each ported rule fires on both client (Valibot) and
  server (Spring) with the same bad inputs.
- **End-to-end local run:** Firestore emulator + Firebase Auth emulator + backend + Next.js;
  seed data; walk the full navbar (Home/Leaderboard/Team Schedule/Account/Admin) confirming
  behavior matches the legacy app (browser traffic only hits Next.js; confirm no
  browser request goes to the Spring origin). Then a smoke test against the **Next.js
  public URL**.
- **Manual parity checklist** derived from the legacy screen inventory (every JSP/action has
  a corresponding new route/endpoint).

---

## Open decisions (assumed defaults, confirm before/at execution)

- **Google sign-in scope:** open to any Google account (auto-assign `player` on first
  sign-in) vs. a domain/email allowlist. *Assumed: open + auto-`player`.*
- **Light/Dark default:** follow `prefers-color-scheme` on first visit vs. always Light.
  *Assumed: Light default, prefers-color-scheme as an option.*

## Decided behaviors (gap review)

- **Pick lock:** once a week’s `beginDate` is today or past, that week’s picks cannot be
  saved. Results show for current/past weeks only.
- **CFBD weeks:** import upserts `seasonWeeks` from `GET /calendar`. Thursday–Wednesday
  rules apply to **manual** week CRUD only.
- **Ties:** not allowed. Equal home/away scores are rejected; college football has no ties.
- **Themes:** closed set of 18 compiled palettes; admin activate/deactivate/rename only.
- **Snapshots:** saving a team or venue refreshes denormalized fields on matchups/rivalries.
- **Deletes:** blocked (409) while children or references exist; no cascade of picks.
- **CFBD import:** background job + UI poll (not a single synchronous BFF POST).
- **Google profile:** each sign-in refreshes email and name; nickName/theme stay user-owned.
- **Hosting:** Next.js is the only public origin; Spring is internal; ALB is not required.
