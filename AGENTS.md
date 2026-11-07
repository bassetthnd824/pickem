# AGENTS.md

Guidance for coding agents working in this repository.

## What this repo is

Kenney's Pickem is a season-long college football **confidence pick'em**.
This tree is a **greenfield rewrite** of a Java EE 7 / Struts 2 WAR into:

| Layer     | Stack                                                                 |
| --------- | --------------------------------------------------------------------- |
| Frontend  | Next.js App Router (`apps/frontend`) via `@nx/next`, Base UI controls |
| Backend   | Spring Boot 4.1 / Java 21 (`apps/backend`) via `@jnxplus/nx-maven`    |
| Data      | Firestore (native mode) — later stories                               |
| Auth      | Firebase Google sign-in + HttpOnly `__session` cookie — later stories |
| Workspace | Nx monorepo (`nx.json`, `package.json`, `tsconfig.base.json`)         |

The browser never calls Spring. Next.js will Axios-proxy `/api/*` to the backend (US-31).

Product spec: [`docs/modernization-plan.md`](docs/modernization-plan.md).
Work breakdown: [`docs/user-stories.md`](docs/user-stories.md).

## Non-negotiable rules

1. **Do not modify `src/main` (or any of the legacy Struts tree under `src/`) until US-26.** It is the functional spec, not the build unit. The old WAR POM is preserved as [`legacy-pom.xml`](legacy-pom.xml).
2. **One user story per branch.** Branch as `US-NN-short-slug`.
3. **Conventional Commits are enforced** (husky `commit-msg` + GitHub Action on PR title and commit range). Include the story id.
4. **Do not expose the Spring API to the browser.** No CORS on Spring; no client-side calls to `:8080`.
5. **Do not put secrets in the client.** `CFBD_API_KEY`, Firebase Admin credentials, and session secrets stay on the server. Locally they come from `.env` (see `apps/backend/.env.example`). In GCP they come from Secret Manager (`PICKEM_SECRET_MANAGER_ENABLED=true` and `sm@`) or Cloud Run env injection. Never commit `.env`.

## Java null analysis

The Java language server runs Eclipse null analysis (`java.compile.nullAnalysis.mode` automatic). Maven compile stays clean while the Problems panel warns. Write the guards in the first version of backend Java, including tests. Do not leave them for a follow-up.

- Pass an `Objects.requireNonNull` local into Firestore `collection`, `document`, `get`, `set`, and `delete`. Check each argument, not only the return. A nested `requireNonNull(firestore.collection(name)).document(id)` still warns, because `name` and `id` are unannotated. Method parameters, loop variables, non-constant fields, and unannotated returns warn. String literals and compile-time `static final` constants do not.
- `DocumentSnapshot.toObject` is `@Nullable`. Assign `Objects.requireNonNull(snapshot.toObject(Type.class), "name")`. `assertThat(value).isNotNull()` does not clear the warning. A helper that returns a plain `T` does not either, unless that return is annotated `@Nonnull`.
- Do not pass method references such as `Season::getSeason` into a `@Nonnull` API. Use an explicit lambda.
- Same shape as `BaseRepository`, `TransactionReads`, and `PickRepository`.

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

mvnw -pl apps/backend spotless:apply   # format backend Java
mvnw -pl apps/backend spotless:check   # same check the validate phase runs
```

Legacy WAR (reference only):

```bash
mvn -f legacy-pom.xml package
```

## Layout

```
apps/frontend/          Next.js App Router (TypeScript)
apps/backend/           Spring Boot 4.1 / Java 21
docs/                   plan, stories, data model, API contract, themes, parity
firestore.rules         deny client reads/writes; Admin SDK only
src/                    legacy Struts/JSP WAR (do not edit until US-26)
legacy-pom.xml          original Java 7 WAR POM
pom.xml                 Maven aggregator (Spring Boot parent, Java 21)
```

`skipProjectWithoutProjectJson: true` keeps Maven modules without an Nx `project.json` (the legacy tree) out of the project graph.

## Implementation order

Follow [`docs/user-stories.md`](docs/user-stories.md). Architecture docs and Firestore rules are **US-02**. Next independent stories after US-01: **US-13** (frontend scaffold). Backend platform starts at **US-03** (needs US-01 and US-02).

Keep each PR inside its story's acceptance criteria. Do not pull later stories into this one.

## Defaults from the plan (do not reopen)

See [`docs/open-decisions.md`](docs/open-decisions.md).

- Any Google account may sign in; new users get the `player` claim.
- Default theme is Light. Themes are a closed set of 18 (Light, Dark, 16 SEC schools).
- Picks lock when the week has begun. College football ties are invalid (400).
- CFBD import uses `GET /games?year={season}&conference=SEC` from Spring only (`conference=SEC` includes non-conference games). Scores only when CFBD `completed`.
- Team/venue edits refresh denormalized snapshots; deletes block if referenced.
- Interactive UI uses Base UI (`@base-ui/react`): buttons, menus, radios, fields, and inputs. Compose Next.js `Link` with the `render` prop. Tables, headings, and plain text stay semantic HTML. Shared wrappers live in `apps/frontend/components/ui`.

## Frontend style

The Next.js app (`apps/frontend`) is formatted with oxfmt and linted with oxlint. Run oxfmt from `apps/frontend` (`npx nx format frontend`). Do not run it from the repo root.

- Single quotes in JavaScript, TypeScript, and JSX. A string may use the other quote when it avoids escaping.
- No semicolons.
- Import app modules with the `@/` alias (`tsconfig.json` `paths`: `@/*` → `./*`). Do not use relative imports (`./` or `../`).
- Sort imports. Keep Node built-ins and third-party packages in one group. Put a blank line between that group and internal `@/` imports (including styles).
- Format with `npx nx format frontend`. Check with `npx nx format-check frontend` and `npx nx lint frontend`. Config: `apps/frontend/.oxfmtrc.json` and `.oxlintrc.json`.

## Java style

Backend Java (`apps/backend`) is formatted with the Maven Spotless plugin (`spotless-maven-plugin` 3.10.4) and the Eclipse JDT profile in `apps/backend/eclipse-formatter.xml`. Spotless `check` is bound to the Maven `validate` phase, so `npx nx build backend` and `npx nx test backend` fail when Java is unformatted. Apply with `mvnw -pl apps/backend spotless:apply`. The plugin formats only `apps/backend/src/main/java` and `apps/backend/src/test/java`. Do not format the legacy tree under `src/`.

The profile uses 4-space indents and hanging parentheses: a wrapped argument list ends the opening line with `(`, puts one parameter or argument on each line, and puts `)` on its own line. Lists that fit stay on one line. Opening braces stay at the end of the line. Put exactly one blank line before and after `if`, `try`, `while`, `for`, and other statements that open a block. Consecutive blank lines collapse to one. `else`, `catch`, and `finally` stay on the closing-brace line (`} else {`), with that one blank line before the line. Field annotations go on the line above the field. Parameter annotations stay on the parameter line.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

## General Guidelines for working with Nx

- For navigating/exploring the workspace, invoke the `nx-workspace` skill first - it has patterns for querying projects, targets, and dependencies
- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
- Prefix nx commands with the workspace's package manager (e.g., `pnpm nx build`, `npm exec nx test`) - avoids using globally installed CLI
- You have access to the Nx MCP server and its tools, use them to help the user
- For Nx plugin best practices, check `node_modules/@nx/<plugin>/PLUGIN.md`. Not all plugins have this file - proceed without it if unavailable.
- NEVER guess CLI flags - always check nx_docs or `--help` first when unsure

## Scaffolding & Generators

- For scaffolding tasks (creating apps, libs, project structure, setup), ALWAYS invoke the `nx-generate` skill FIRST before exploring or calling MCP tools

## When to use nx_docs

- USE for: advanced config options, unfamiliar flags, migration guides, plugin configuration, edge cases
- DON'T USE for: basic generator syntax (`nx g @nx/react:app`), standard commands, things you already know
- The `nx-generate` skill handles generator discovery internally - don't call nx_docs just to look up generator syntax

<!-- nx configuration end-->
