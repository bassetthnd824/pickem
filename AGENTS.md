# AGENTS.md

Guidance for coding agents working in this repository.

## What this repo is

Kenney's Pickem is a season-long college football **confidence pick'em**.
This tree is a **greenfield rewrite** of a Java EE 7 / Struts 2 WAR into:

| Layer | Stack |
|---|---|
| Frontend | Next.js App Router (`apps/frontend`) via `@nx/next` |
| Backend | Spring Boot 3.x / Java 21 (`apps/backend`) via `@jnxplus/nx-maven` |
| Data | Firestore (native mode) — later stories |
| Auth | Firebase Google sign-in + HttpOnly `__session` cookie — later stories |
| Workspace | Nx monorepo (`nx.json`, `package.json`, `tsconfig.base.json`) |

The browser never calls Spring. Next.js will Axios-proxy `/api/*` to the backend (US-31).

Product spec: [`docs/modernization-plan.md`](docs/modernization-plan.md).
Work breakdown: [`docs/user-stories.md`](docs/user-stories.md).

## Non-negotiable rules

1. **Do not modify `src/main` (or any of the legacy Struts tree under `src/`) until US-26.** It is the functional spec, not the build unit. The old WAR POM is preserved as [`legacy-pom.xml`](legacy-pom.xml).
2. **One user story per branch.** Branch as `feat/US-NN-short-slug` (or `fix/` / `docs/` / `chore/` matching the commit type).
3. **Conventional Commits are enforced** (husky `commit-msg` + GitHub Action on PR title and commit range). Include the story id.
4. **Do not expose the Spring API to the browser.** No CORS on Spring; no client-side calls to `:8080`.
5. **Do not put secrets in the client.** `CFBD_API_KEY`, Firebase Admin credentials, and session secrets stay on the server.

## Conventional commits

Format:

```
<type>(<optional-scope>): <imperative summary>

<body>

US-NN
```

Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

Examples:

```
feat(workspace): bootstrap nx monorepo

US-01
```

```
feat(api): add season CRUD endpoints

US-05
```

Local enforcement: `.husky/commit-msg` runs commitlint.
CI enforcement: `.github/workflows/lint-commits.yml` (PR title + commits).

## Commands

Requires **Node 20+** (24 is fine) and **JDK 21+**. Maven Wrapper is in the repo (`mvnw` / `mvnw.cmd`).

```bash
npm install

npx nx show projects          # must list frontend and backend; not the legacy WAR
npx nx graph

npx nx build frontend
npx nx test frontend
npx nx serve frontend         # Next.js dev server (plugin maps "serve" ← Next "dev")

npx nx build backend          # delegates to Maven
npx nx test backend
npx nx serve backend          # spring-boot:run, port 8080
```

Legacy WAR (reference only):

```bash
mvn -f legacy-pom.xml package
```

## Layout

```
apps/frontend/          Next.js App Router (TypeScript)
apps/backend/           Spring Boot 3 / Java 21
docs/                   modernization plan + user stories
src/                    legacy Struts/JSP WAR (do not edit until US-26)
legacy-pom.xml          original Java 7 WAR POM
pom.xml                 Maven aggregator (Spring Boot parent, Java 21)
```

`skipProjectWithoutProjectJson: true` keeps Maven modules without an Nx `project.json` (the legacy tree) out of the project graph.

## Implementation order

Follow [`docs/user-stories.md`](docs/user-stories.md). Current foundation story is **US-01**. Next independent stories: **US-02** (architecture docs / Firestore rules) and, after US-01, **US-13** (frontend scaffold beyond this shell). Backend platform starts at **US-03**.

Keep each PR inside its story's acceptance criteria. Do not pull later stories into this one.

## Defaults from the plan (do not reopen)

- Any Google account may sign in; new users get the `player` claim.
- Default theme is Light. Themes are a closed set of 18 (Light, Dark, 16 SEC schools).
- Picks lock when the week has begun. College football ties are invalid (400).
- CFBD import uses `GET /games?year={season}&conference=SEC` from Spring only.
- Team/venue edits refresh denormalized snapshots; deletes block if referenced.
