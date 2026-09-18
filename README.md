# Kenney's Pickem

A season-long college football **confidence pick'em**. Each week, players pick a winner for every matchup and rank those picks by confidence. A correct pick scores its rank in points; the leaderboard is the season total.

This repository is an **Nx monorepo** rewrite (Next.js + Spring Boot + Firestore). The original Java EE application remains under `src/` as the functional spec until [US-26](docs/user-stories.md).

## How the game works

1. A **manager** sets up a season, weeks, teams, venues, rivalries, and weekly matchups.
2. A **player** picks a winner for each game in the current week.
3. Picks are ordered by **confidence rank**. Rank `N` is the player's most confident pick, down to `1`, where `N` is the number of conference teams.
4. After scores are entered, a correct pick earns its rank. Wrong picks earn `0`.
5. The **leaderboard** sums those points for the season.

Picks for a week lock once that week's begin date has arrived. Results show for the current and past weeks.

## Workspace

```
pickem/
  apps/frontend/     # Next.js App Router (@nx/next)
  apps/backend/      # Spring Boot 4.1 / Java 21 (@jnxplus/nx-maven)
  docs/              # architecture, data model, API contract, themes
  src/               # legacy Struts WAR (do not edit until US-26)
  firestore.rules    # deny all client reads/writes (Admin SDK only)
  firebase.json
  legacy-pom.xml     # original Java 7 WAR POM
  pom.xml            # Maven aggregator (Spring Boot parent)
  nx.json
  package.json
  tsconfig.base.json
  AGENTS.md
```

| Command | What it does |
|---|---|
| `npx nx serve frontend` | Next.js dev server |
| `npx nx build frontend` | Production Next.js build |
| `npx nx test frontend` | Vitest |
| `npx nx serve backend` | `spring-boot:run` on port 8080 |
| `npx nx build backend` | Maven `package` (skip tests) |
| `npx nx test backend` | Maven `test` |
| `npx nx show projects` | Project graph membership |

The legacy `src/` Maven WAR is **not** in the Nx graph (`skipProjectWithoutProjectJson`). To package it: `mvn -f legacy-pom.xml package`.

## Prerequisites

- Node.js 20+ (24 recommended)
- JDK 21+ (Maven Wrapper included)
- npm 11+

## Setup

```bash
npm install
cp apps/backend/.env.example apps/backend/.env
npx nx serve frontend
npx nx serve backend
```

### Secrets

The Spring Boot API reads secrets from **OS environment variables**, a **`.env` file**, or **Google Secret Manager**. Existing environment variables always win over `.env`.

| Source | When |
|---|---|
| `.env` | Local. The backend looks at `PICKEM_DOTENV_FILE`, then `./.env`, `./apps/backend/.env`, and `../.env`. |
| Environment variables | Any environment, including Cloud Run secrets mounted as env vars. |
| Google Secret Manager | Set `PICKEM_SECRET_MANAGER_ENABLED=true` (ADC + `GOOGLE_CLOUD_PROJECT`). Values resolve as `sm://secret-id`. |

Example: `CFBD_API_KEY` or Secret Manager secret `cfbd-api-key`. Do not commit `.env`. See `apps/backend/.env.example`.

## Conventional commits

Commits and PR titles must follow [Conventional Commits](https://www.conventionalcommits.org/). Include the user-story id (for example `US-01`).

```
feat(workspace): bootstrap nx monorepo

US-01
```

Enforced locally by Husky + commitlint, and on PRs by `.github/workflows/lint-commits.yml`.

## Docs

- [Modernization plan](docs/modernization-plan.md) — rewrite to Next.js, Spring Boot, and Firestore
- [User stories](docs/user-stories.md) — implementable work for that rewrite
- [Firestore data model](docs/firestore-data-model.md) — collections, snapshots, CFBD ids, dropped Derby tables
- [API contract](docs/api-contract.md) — `/api/auth`, `/api/game`, `/api/manager` (browser → Next.js BFF → Spring)
- [Theme colors](docs/sec-theme-colors.md) — 18 theme keys and hex values
- [Parity checklist](docs/parity-checklist.md) — legacy JSP/action → new route/endpoint
- [Open decisions](docs/open-decisions.md) — sign-in, default theme, CFBD import/scoring
- [AGENTS.md](AGENTS.md) — conventions for coding agents

## Legacy stack (reference)

The original WAR (`src/` + `legacy-pom.xml`) is Struts 2.3.24 + Tiles 3 + JPA/Hibernate on WildFly / Java 7 / Apache Derby. Auth was container-managed JAAS (`player` / `manager`). It is not the build unit anymore.
