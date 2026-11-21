# Graph Report - pickem  (2026-11-20)

## Corpus Check
- 332 files · ~124,550 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: (none) 6, .example 3, .xml 3)

## Summary
- 3016 nodes · 9367 edges · 159 communities (89 shown, 70 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 1058 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Account Update Binding
- Bootstrap and Seeding
- Base Repository Core
- Role Claims and Scoring
- Matchup Model
- Firebase Admin Identity
- Audit Actor Repositories
- CI Fix Flows
- Season and Leaderboard
- Document Version Errors
- Repository Transactions
- Manager Theme Tests
- CFBD Import Job
- CFBD Calendar Venues
- Score Sync Job
- Frontend App Shell
- Pick and Season Week
- Backend Application Config
- Game Route Pages
- Picks Grid UI
- Auditable Import Documents
- Account Form UI
- Mark Story Workflow
- Audit Fields and Venue
- Frontend Oxlint Config
- Google Sign-In Page
- Auth Layout Providers
- Venue and Team Snapshots
- Seed Display Names
- REST Controllers
- Account Profile Service
- Session Cookie Auth Wiring
- API Proxy Route
- Commit Lint Workflow
- User Profile Model
- Principal and Roles
- Internal Job Filter
- Internal Job Route
- Team Schedule Page
- Mark Story Script
- Root Dev Dependencies
- Backend Nx Project
- Dotenv Post Processor
- Auth Session Routes
- Leaderboard API Client
- Nx Workspace Config
- Backend Test Runner
- Manager Matchup Tests
- Theme Catalog Service
- Manager Reference Tests
- Backend Test Config
- Story and Snapshot Docs
- Agent Implementation Rules
- Import Job Executor
- CFBD Client Config
- Frontend Package Scripts
- Firestore and Clock Config
- Rivalry Model
- Spring Proxy Client
- Nx Gradle Import
- User Story Skill
- User Controller Session
- CFBD Client Tests
- Player API Resources
- Frontend Runtime Dependencies
- Seed Environment Loader
- Auth Session Tests
- Manager Season Tests
- TypeScript Compiler Options
- CFBD HTTP Client
- Workspace Package Linking
- Firestore Emulator Support
- Date Field Controls
- Auth and Theme API Docs
- Next and Vite Import
- Fake Firebase Identity
- Frontend Format Config
- Frontend Nx Project
- Frontend TypeScript Config
- Spring Security Config
- Session Cookie Filter Tests
- CFBD Import Product Rules
- Internal Job Authentication
- Query Provider Tests
- Theme Cookie Resolution
- Backend TypeScript Options
- CFBD Date Helpers
- Season Week Service
- Home and Button UI
- Search Text Helper
- CI Workflow Jobs
- CI Environment Variables
- Jest Import Migration
- CFBD Game DTOs
- Auth Credential Errors
- Internal Job Controller
- Domain JSON Tests
- Frontend Story Scaffold
- CI Workflow Steps
- ESLint Import Migration
- Turborepo Import Migration
- Internal Job API Tests
- CI Setup Action Inputs
- Google Profile Tests
- Job Secret Comparison
- Loopback Address Checks
- Pinned Test Clock
- Second Pinned Clock
- NPM Dependency Overrides
- Spring Boot Application
- CI Affected Outputs
- CI Backend Test Job
- CFBD Client Exception
- Import Accepted Response
- CI Backend Build Job
- CI Frontend Build Job
- CI Frontend Test Job
- CI Workflow Triggers
- Save Picks Request
- Seed Failed Exception
- Team Schedule Row
- SVG Module Types
- Next Config
- Test Setup Polyfill
- CFBD Import Endpoints
- Dropped Legacy Identity
- Commitlint Scripts
- Frontend Style Rules
- PostCSS Config
- Confidence Pickem Rules
- Greenfield Rewrite
- Husky Hook
- JSDOM Test Env
- Nx Next Plugin
- Nx Oxlint Plugin
- Oxfmt Formatter
- Oxlint Linter
- React DOM Types
- Vite TSConfig Paths
- Vitest Coverage
- Vitest UI
- React Component Type
- Auth Me and Logout
- API Error Shape
- Firebase Role Matrix
- Team Schedule API
- Seasons Collection
- Backend Maven Module
- Root Maven Module

## God Nodes (most connected - your core abstractions)
1. `Matchup` - 83 edges
2. `CfbdImportService` - 68 edges
3. `GamePicksApiTests` - 59 edges
4. `Team` - 58 edges
5. `CfbdScoreJobTests` - 57 edges
6. `Season` - 52 edges
7. `Venue` - 52 edges
8. `SeasonWeek` - 50 edges
9. `GameAccountScheduleApiTests` - 50 edges
10. `User Stories` - 50 edges

## Surprising Connections (you probably didn't know these)
- `Game Rules` --semantically_similar_to--> `Confidence Pick'em`  [INFERRED] [semantically similar]
  README.md → docs/modernization-plan.md
- `Seed README` --conceptually_related_to--> `US-12 Seed Firestore with reference data and themes`  [INFERRED]
  tools/seed/README.md → docs/user-stories.md
- `Redundant Vite npm Scripts` --semantically_similar_to--> `Duplicate lint and eslint:lint Targets`  [INFERRED] [semantically similar]
  .agents/skills/nx-import/references/VITE.md → .agents/skills/nx-import/references/ESLINT.md
- `Acceptance Checkbox Update` --references--> `User Stories`  [EXTRACTED]
  .grok/skills/implement-user-story/SKILL.md → docs/user-stories.md
- `Implement User Story Skill` --cites--> `User Stories`  [EXTRACTED]
  .grok/skills/implement-user-story/SKILL.md → docs/user-stories.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Nine Firestore Domain Collections** — docs_firestore_data_model_seasons, docs_firestore_data_model_season_weeks, docs_firestore_data_model_venues, docs_firestore_data_model_teams, docs_firestore_data_model_rivalries, docs_firestore_data_model_matchups, docs_firestore_data_model_picks, docs_firestore_data_model_users, docs_firestore_data_model_themes [EXTRACTED 1.00]
- **Google Session Cookie Authentication Flow** — docs_modernization_plan_auth_flow, docs_api_contract_auth_session, docs_api_contract_bff_call_path, docs_modernization_plan_session_cookie_filter, docs_open_decisions_session_cookie, docs_user_stories_us_04, docs_user_stories_us_15, docs_user_stories_us_32 [EXTRACTED 1.00]
- **CFBD Import and Game-day Score Pipeline** — docs_user_stories_us_27, docs_user_stories_us_38, docs_user_stories_us_28, docs_user_stories_us_29, docs_user_stories_us_30, docs_api_contract_cfbd_import_api, docs_api_contract_score_job, docs_open_decisions_cfbd_conference_sec, docs_open_decisions_scores_when_completed [EXTRACTED 1.00]
- **nx import Technology-Specific Guidance** — _agents_skills_nx_import_skill_nx_import, _agents_skills_nx_import_references_eslint_plugin, _agents_skills_nx_import_references_gradle_nx_gradle, _agents_skills_nx_import_references_jest_plugin, _agents_skills_nx_import_references_next_plugin, _agents_skills_nx_import_references_turborepo_migration, _agents_skills_nx_import_references_vite_plugin [EXTRACTED 1.00]
- **Nx Cloud Self-Healing Monitor Loop** — _agents_skills_monitor_ci_skill_orchestrator, agents_skills_monitor_ci_scripts_ci_poll_decide, agents_skills_monitor_ci_scripts_ci_state_update, _agents_skills_monitor_ci_skill_ci_information, _agents_skills_monitor_ci_references_fix_flows_status_handling [EXTRACTED 1.00]
- **Frontend Framework and Test Runner Coexistence** — _agents_skills_nx_import_references_next_mixed_vite, _agents_skills_nx_import_references_vite_mixed_frameworks, _agents_skills_nx_import_references_jest_vitest_coexistence, _agents_skills_nx_import_references_vite_typecheck_names [INFERRED 0.85]

## Communities (159 total, 70 thin omitted)

### Community 0 - "Account Update Binding"
Cohesion: 0.05
Nodes (9): AccountUpdate, InvalidRequestException, AccountUpdateBinder, GameAccountScheduleApiTests, Slate, GamePicksApiTests, PinnedClockConfig, Clock (+1 more)

### Community 1 - "Bootstrap and Seeding"
Cohesion: 0.06
Nodes (13): BootstrapAccount, BootstrapIdentity, FirestoreSeeder, SeedCatalog, MatchupSeed, RivalrySeed, TeamSeed, ThemeSeed (+5 more)

### Community 2 - "Base Repository Core"
Cohesion: 0.06
Nodes (7): BaseRepository, SaveConstraint, FirestoreAccessException, WeekGuard, ReferenceGuard, ReferenceCheck, PickRepositoryTests

### Community 3 - "Role Claims and Scoring"
Cohesion: 0.14
Nodes (11): RoleClaims, SessionCookies, VerifiedIdentity, PickScoring, BackendApplicationTests, PickemSecretsPropertiesTests, SecurityConfigTests, HelloControllerTests (+3 more)

### Community 4 - "Matchup Model"
Cohesion: 0.08
Nodes (7): Matchup, TeamSquadSnapshot, CfbdPause, CfbdScoreService, MatchupService, TeamScheduleService, MatchupController

### Community 5 - "Firebase Admin Identity"
Cohesion: 0.07
Nodes (5): FirebaseAdminIdentityClient, FirebaseEmulatorEnvironment, FirebaseBootstrapIdentity, FirebaseAdminIdentityClientTests, FirebaseEmulatorEnvironmentTests

### Community 6 - "Audit Actor Repositories"
Cohesion: 0.12
Nodes (11): AuditActorResolver, ImportJobRepository, MatchupRepository, PickRepository, RivalryRepository, SeasonRepository, SeasonWeekRepository, TeamRepository (+3 more)

### Community 7 - "CI Fix Flows"
Cohesion: 0.06
Nodes (44): Apply Locally and Enhance, nx-cloud apply-locally, Apply Fix via MCP, ci-state-update.mjs Gate, Reject and Fix From Scratch, CI Fix Status Handling, UPDATE_FIX Subagent, ci_information MCP Tool (+36 more)

### Community 8 - "Season and Leaderboard"
Cohesion: 0.08
Nodes (10): Season, Leaderboard, Standing, Scored, SeasonCalendar, ParsedSeason, ParsedWeek, SeasonService (+2 more)

### Community 9 - "Document Version Errors"
Cohesion: 0.08
Nodes (12): StaleDocumentVersionException, AuthUnavailableException, IdentityAdminException, Kind, UNAVAILABLE, USER_NOT_FOUND, ConflictException, ResourceNotFoundException (+4 more)

### Community 11 - "Manager Theme Tests"
Cohesion: 0.09
Nodes (4): ManagerUserThemeApiTests, SnapshotReferenceApiTests, PinnedClockConfig, Clock

### Community 12 - "CFBD Import Job"
Cohesion: 0.11
Nodes (3): CfbdGame, ImportJob, CfbdImportService

### Community 13 - "CFBD Calendar Venues"
Cohesion: 0.18
Nodes (3): CfbdCalendarWeek, CfbdVenue, CfbdImportServiceTests

### Community 14 - "Score Sync Job"
Cohesion: 0.18
Nodes (3): ScoreSyncResult, CfbdScoreJobTests, Slate

### Community 15 - "Frontend App Shell"
Cohesion: 0.09
Nodes (32): metadata, RootLayout(), GAME_LINKS, RESTRICTED_PATH, resolveAuthEmulatorHost(), resolveFirebaseProjectId(), ensureApp(), isRejectedCookie() (+24 more)

### Community 16 - "Pick and Season Week"
Cohesion: 0.13
Nodes (7): Pick, SeasonWeek, GameMain, GameMatchup, GameWeek, PickService, WeekPlan

### Community 17 - "Backend Application Config"
Cohesion: 0.05
Nodes (39): path, name, emulator-host, session-duration, base-url, gcp, import, datetime (+31 more)

### Community 18 - "Game Route Pages"
Cohesion: 0.10
Nodes (26): AccountPage(), metadata, LeaderboardPage(), metadata, MainPage(), metadata, metadata, RestrictedPage() (+18 more)

### Community 19 - "Picks Grid UI"
Cohesion: 0.11
Nodes (36): Column(), dateText(), errorDetail(), loadMain(), MAIN_KEY, PicksGrid(), cancel(), reload() (+28 more)

### Community 21 - "Account Form UI"
Cohesion: 0.15
Nodes (31): AccountForm(), save(), errorDetail(), loadAccount(), ReadOnlyField(), useAuth(), loadAccount(), loadThemes() (+23 more)

### Community 22 - "Mark Story Workflow"
Cohesion: 0.05
Nodes (36): concurrency, cancel-in-progress, group, BASE_SHA, GH_TOKEN, GITHUB_TOKEN, PR_NUMBER, PR_URL (+28 more)

### Community 23 - "Audit Fields and Venue"
Cohesion: 0.15
Nodes (4): AuditableDocument, Venue, VenueService, BaseRepositoryTests

### Community 24 - "Frontend Oxlint Config"
Cohesion: 0.06
Nodes (35): categories, correctness, Link, env, browser, es2024, node, ignorePatterns (+27 more)

### Community 25 - "Google Sign-In Page"
Cohesion: 0.13
Nodes (25): LoginPage(), metadata, GoogleSignIn(), onClick(), signInMessage(), Button(), clientAuth(), consumeGoogleRedirect() (+17 more)

### Community 26 - "Auth Layout Providers"
Cohesion: 0.11
Nodes (26): GameLayout(), ManagerLayout(), AppShell(), AUTH_ME_KEY, AuthContext, AuthProvider(), AuthState, loadProfile() (+18 more)

### Community 27 - "Venue and Team Snapshots"
Cohesion: 0.14
Nodes (4): VenueSnapshot, Team, TeamService, TeamController

### Community 28 - "Seed Display Names"
Cohesion: 0.09
Nodes (7): SeedNames, Name, PickemAuthPropertiesTests, JobSecretsTests, SeedNamesTests, AuthServiceTests, PickScoringTests

### Community 29 - "REST Controllers"
Cohesion: 0.27
Nodes (3): HelloController, ImportController, SeasonController

### Community 30 - "Account Profile Service"
Cohesion: 0.12
Nodes (6): AccountProfile, AccountService, ConferenceTeam, ThemeCatalog, ThemeChoice, GameController

### Community 31 - "Session Cookie Auth Wiring"
Cohesion: 0.14
Nodes (7): PickemAuthProperties, UserRepository, FirebaseIdentityClient, GoogleProfile, SessionCookieFilter, AuthService, RoleClaimSync

### Community 32 - "API Proxy Route"
Cohesion: 0.11
Nodes (20): DELETE(), dynamic, forward(), GET(), POST(), PUT(), RouteContext, runtime (+12 more)

### Community 33 - "Commit Lint Workflow"
Cohesion: 0.08
Nodes (27): name, runs-on, steps, GITHUB_TOKEN, jobs, commitlint, pr-title, name (+19 more)

### Community 36 - "Principal and Roles"
Cohesion: 0.11
Nodes (3): PickemPrincipal, SessionAuthentication, RoleClaimsTests

### Community 38 - "Internal Job Route"
Cohesion: 0.14
Nodes (21): dynamic, POST(), RouteContext, runtime, authorizeInternalJob(), bearerToken(), configuredJobSecret(), configuredOidcEmails() (+13 more)

### Community 39 - "Team Schedule Page"
Cohesion: 0.15
Nodes (20): metadata, TeamSchedulePage(), loadSchedule(), loadTeams(), SchedulePanel(), TEAMS_KEY, TeamSchedule(), TeamSelect() (+12 more)

### Community 40 - "Mark Story Script"
Cohesion: 0.16
Nodes (21): applyToFile(), collectExplicitStoryIds(), fetchCommitMessages(), findHeading(), findIndexRow(), main(), markIndexDone(), markStoryDone() (+13 more)

### Community 41 - "Root Dev Dependencies"
Cohesion: 0.08
Nodes (26): devDependencies, @commitlint/cli@^21.2.2, @commitlint/config-conventional@^21.2.2, eslint-plugin-perfectionist@^5.13.0, @jnxplus/nx-maven@^2.3.0, @swc/core@~1.15.5, @swc/helpers@~0.5.18, @swc-node/register@~1.11.1 (+18 more)

### Community 42 - "Backend Nx Project"
Cohesion: 0.09
Nodes (24): executor, executor, options, options, outputs, name, command, cwd (+16 more)

### Community 43 - "Dotenv Post Processor"
Cohesion: 0.13
Nodes (5): DotenvEnvironmentPostProcessor, DotenvFileLoader, LoadedDotenv, DotenvEnvironmentPostProcessorTests, DotenvFileLoaderTests

### Community 44 - "Auth Session Routes"
Cohesion: 0.15
Nodes (20): dynamic, POST(), runtime, dynamic, GET(), runtime, dynamic, POST() (+12 more)

### Community 45 - "Leaderboard API Client"
Cohesion: 0.14
Nodes (18): LEADERBOARD_KEY, LeaderboardTable(), loadLeaderboard(), api, ApiCall, ApiError, ApiMethod, ApiRequest (+10 more)

### Community 46 - "Nx Workspace Config"
Cohesion: 0.08
Nodes (24): analytics, linter, style, unitTestRunner, cache, dependsOn, inputs, defaultBase (+16 more)

### Community 48 - "Backend Test Runner"
Cohesion: 0.10
Nodes (14): args, backendDir, env, envFile, mvnw, path, { readFileSync }, repoRoot (+6 more)

### Community 50 - "Theme Catalog Service"
Cohesion: 0.20
Nodes (3): Theme, ThemeService, ThemeController

### Community 52 - "Backend Test Config"
Cohesion: 0.09
Nodes (22): provider, session-duration, base-url, gcp, enabled, emulator-enabled, emulator-host, enabled (+14 more)

### Community 53 - "Story and Snapshot Docs"
Cohesion: 0.20
Nodes (21): Valibot and Spring Validation Parity, C Manager APIs, E Seed, G Player / manager UI, US-05 Manage seasons and season weeks via REST, US-06 Manage venues, teams, and rivalries via REST, US-07 Manage matchups via REST, US-08 Manage users and themes via REST (+13 more)

### Community 54 - "Agent Implementation Rules"
Cohesion: 0.11
Nodes (18): Review Then One PR, Spotless Eclipse Formatter, Health Checks, Document Audit and Version, Firestore Composite Indexes, BaseRepository, Nx Monorepo Layout, A Foundation (+10 more)

### Community 55 - "Import Job Executor"
Cohesion: 0.13
Nodes (4): ImportJobExecutorConfig, SecurityContextAuditActorResolver, ImportJobDispatcher, SleepingCfbdPause

### Community 56 - "CFBD Client Config"
Cohesion: 0.14
Nodes (5): CfbdConfig, PickemCfbdProperties, PickemInternalProperties, PickemSecretsProperties, PickemInternalPropertiesTests

### Community 57 - "Frontend Package Scripts"
Cohesion: 0.10
Nodes (20): nx@23.3.1, @nx/js@23.3.1, @nx/vitest@23.3.1, @stylistic/eslint-plugin@^5.10.0, @swc/cli@~0.8.1, @types/react@^19.0.0, typescript@~6.0.3, vitest@^4.1.12 (+12 more)

### Community 58 - "Firestore and Clock Config"
Cohesion: 0.15
Nodes (5): ClockConfig, FirestoreConfig, OpenApiConfig, PickemFirestoreProperties, FirestoreConfigTests

### Community 59 - "Rivalry Model"
Cohesion: 0.23
Nodes (3): Rivalry, RivalryService, RivalryController

### Community 60 - "Spring Proxy Client"
Cohesion: 0.16
Nodes (19): API_ERRORS, BROWSER_API_ROOTS, browserApiDecision(), callSpring(), cloudRunAuthorization(), hasControlCharacter(), headerList(), headerString() (+11 more)

### Community 61 - "Nx Gradle Import"
Cohesion: 0.13
Nodes (17): Nx Cloud Connection Check, Nx Gradle Introduction Docs, @nx/gradle Plugin, Verify Gradle Inference with nx show projects, Inferred Plugin Targets, nx affected, nx run, nx run-many (+9 more)

### Community 62 - "User Story Skill"
Cohesion: 0.23
Nodes (14): Acceptance Checkbox Update, Implement User Story Skill, API Contract, Manager CRUD API, Firestore Data Model, Modernization Plan, Open Decisions, Legacy Screen Inventory (+6 more)

### Community 65 - "Player API Resources"
Cohesion: 0.14
Nodes (16): Weekly Picks API, Leaderboard API, matchups, picks, rivalries, seasonWeeks, teams, venues (+8 more)

### Community 66 - "Frontend Runtime Dependencies"
Cohesion: 0.11
Nodes (19): dependencies, axios@1.21.0, @base-ui/react@^1.9.0, firebase@^13.1.0, firebase-admin@^14.5.0, google-auth-library@^11.1.0, next@~16.4.3, postcss@^8.5.32 (+11 more)

### Community 70 - "TypeScript Compiler Options"
Cohesion: 0.11
Nodes (18): compilerOptions, allowJs, allowSyntheticDefaultImports, emitDeclarationOnly, esModuleInterop, forceConsistentCasingInFileNames, incremental, isolatedModules (+10 more)

### Community 72 - "Workspace Package Linking"
Cohesion: 0.12
Nodes (11): Link Workspace Packages Skill, Package Manager Detection, workspace Protocol Linking, Generator directory Flag Is the Full Path, Generator Discovery, Generator Dry Run, Nx Generate Skill, pnpm Workspace Glob Fix (+3 more)

### Community 75 - "Date Field Controls"
Cohesion: 0.18
Nodes (11): DateField(), DateFieldProps, fieldErrorMessage(), messageOf(), IsoDate, isoDateSchema, GameDateForm(), @tanstack/react-form@^1.33.5 (+3 more)

### Community 76 - "Auth and Theme API Docs"
Cohesion: 0.12
Nodes (12): Account and Theme Picker API, POST /api/auth/session, themes, users, Firebase Auth Flow, SessionCookieFilter, Eighteen Theme Catalog, Success and Danger Pick Icons (+4 more)

### Community 77 - "Next and Vite Import"
Cohesion: 0.20
Nodes (15): Jest Test Script Rewriting, Mixed Next.js and Vite Coexistence, @nx/next/plugin, @nx/next:init Script Rewriting, Next.js tsconfig noEmit and jsx preserve, withNx composePlugins, Next.js yarn add Wrong Package Manager, Mixed React and Vue Vite Settings (+7 more)

### Community 79 - "Frontend Format Config"
Cohesion: 0.13
Nodes (14): ignorePatterns, jsxSingleQuote, printWidth, $schema, semi, singleQuote, sortImports, customGroups (+6 more)

### Community 80 - "Frontend Nx Project"
Cohesion: 0.14
Nodes (14): cache, command, options, command, options, name, cwd, projectType (+6 more)

### Community 81 - "Frontend TypeScript Config"
Cohesion: 0.13
Nodes (12): exclude, extends, include, compilerOptions, jsx, outDir, types, exclude (+4 more)

### Community 84 - "CFBD Import Product Rules"
Cohesion: 0.22
Nodes (11): BFF Allowlist, Internal Job Auth, Game-day Score Job, I CFBD, US-27 Integrate the College Football Data API client, US-28 Import a season's SEC-team matchups from CFBD, US-29 Automatically import scores on game days, US-30 Trigger and review CFBD imports from the admin UI (+3 more)

### Community 86 - "Query Provider Tests"
Cohesion: 0.22
Nodes (9): makeQueryClient(), QueryProvider(), Call, expectDisabled(), expectEnabled(), isDisabled(), main, profile (+1 more)

### Community 87 - "Theme Cookie Resolution"
Cohesion: 0.19
Nodes (10): DEFAULT_THEME, resolveThemeId(), THEME_COOKIE_NAME, THEME_KEY_SET, THEME_KEYS, ThemeKey, BRAND_COLORS, frontendRoot (+2 more)

### Community 88 - "Backend TypeScript Options"
Cohesion: 0.15
Nodes (13): compilerOptions, declaration, emitDecoratorMetadata, experimentalDecorators, importHelpers, lib, module, moduleResolution (+5 more)

### Community 91 - "Home and Button UI"
Cohesion: 0.26
Nodes (6): HomePage(), PickResult, PickResultIcon(), PickResultIconProps, ButtonProps, buttonVariantClass

### Community 93 - "CI Workflow Jobs"
Cohesion: 0.18
Nodes (10): if, name, needs, runs-on, jobs, build, name, permissions (+2 more)

### Community 94 - "CI Environment Variables"
Cohesion: 0.18
Nodes (11): AFFECTED_RESULT, BACKEND, BUILD_BACKEND, BUILD_FRONTEND, FRONTEND, NX_DAEMON, PR_BASE_SHA, PUSH_BEFORE (+3 more)

### Community 95 - "Jest Import Migration"
Cohesion: 0.22
Nodes (10): Jest CI Atomization, @nx/jest/plugin, jest.preset.js, Jest and Vitest Coexistence, Application versus Library Detection, Nx Import Project Docs, Project Name Collisions, nx import Skill (+2 more)

### Community 96 - "CFBD Game DTOs"
Cohesion: 0.31
Nodes (4): CfbdScoreboardGame, Team, Venue, SessionRequest

### Community 97 - "Auth Credential Errors"
Cohesion: 0.24
Nodes (3): InvalidCredentialException, AuthController, AuthProfileResponse

### Community 98 - "Internal Job Controller"
Cohesion: 0.24
Nodes (4): ImportWorkerRequest, InternalJobAccepted, InternalJobController, ScoreJobResponse

### Community 100 - "Frontend Story Scaffold"
Cohesion: 0.40
Nodes (9): Browser to BFF to Spring, Cloud Run Split Hosting, F Frontend shell, US-04 Authenticate with Google via an HttpOnly session cookie, US-13 Scaffold the Next.js app and API client, US-14 Apply switchable Light, Dark, and SEC school themes, US-15 Sign in with Google and protect game and manager routes, US-31 Proxy browser API calls through Next.js with Axios (+1 more)

### Community 101 - "CI Workflow Steps"
Cohesion: 0.40
Nodes (10): steps, steps, steps, steps, id, if, name, run (+2 more)

### Community 103 - "ESLint Import Migration"
Cohesion: 0.22
Nodes (9): Duplicate lint and eslint:lint Targets, Flat Config CJS Self-Lint, Legacy eslintrc ignorePatterns, Mixed ESLint v8 and v9, @nx/eslint/plugin, typescript-eslint v7 versus ESLint 9, eslint-config-next Self-Contained Config, Vue ESLint Install Order (+1 more)

### Community 104 - "Turborepo Import Migration"
Cohesion: 0.22
Nodes (7): Nx from Turborepo Docs, @repo/eslint-config, Merge ESLint Config into Root eslint.config, Merge TypeScript Config into tsconfig.base.json, Turborepo to Nx Migration, @repo/typescript-config, Frontend tsconfig.base.json Settings

### Community 106 - "CI Setup Action Inputs"
Cohesion: 0.22
Nodes (9): with, cache, distribution, fetch-depth, java-version, key, node-version, path (+1 more)

### Community 112 - "NPM Dependency Overrides"
Cohesion: 0.29
Nodes (7): overrides, glob, @grpc/grpc-js, minimatch, nwsapi, typescript, undici

### Community 114 - "CI Affected Outputs"
Cohesion: 0.33
Nodes (6): name, outputs, runs-on, affected, backend, frontend

### Community 115 - "CI Backend Test Job"
Cohesion: 0.33
Nodes (6): test-backend, if, name, needs, runs-on, steps

### Community 119 - "CI Backend Build Job"
Cohesion: 0.40
Nodes (5): if, name, needs, runs-on, build-backend

### Community 120 - "CI Frontend Build Job"
Cohesion: 0.40
Nodes (5): if, name, needs, runs-on, build-frontend

### Community 121 - "CI Frontend Test Job"
Cohesion: 0.40
Nodes (5): test-frontend, if, name, needs, runs-on

### Community 122 - "CI Workflow Triggers"
Cohesion: 0.40
Nodes (5): on, pull_request, push, workflow_dispatch, branches

### Community 131 - "Commitlint Scripts"
Cohesion: 0.67
Nodes (3): scripts, commitlint, prepare

## Knowledge Gaps
- **56 isolated node(s):** `com.curleesoft.pickem:backend`, `USER_NOT_FOUND`, `UNAVAILABLE`, `com.curleesoft.pickem:pickem`, `ReactComponent` (+51 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 612 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **70 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Matchup` connect `Matchup Model` to `Bootstrap and Seeding`, `Base Repository Core`, `Domain JSON Tests`, `Role Claims and Scoring`, `Audit Actor Repositories`, `Repository Transactions`, `CFBD Import Job`, `CFBD Calendar Venues`, `Score Sync Job`, `CFBD Import Tests`, `Pick and Season Week`, `Auditable Import Documents`, `Audit Fields and Venue`, `Venue and Team Snapshots`, `REST Controllers`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **What connects `com.curleesoft.pickem:backend`, `USER_NOT_FOUND`, `UNAVAILABLE` to the rest of the system?**
  _56 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Account Update Binding` be split into smaller, more focused modules?**
  _Cohesion score 0.054215625644197074 - nodes in this community are weakly interconnected._
- **Why does `SessionCookies` connect `Role Claims and Scoring` to `Auth Credential Errors`, `Internal Job Filter`, `CFBD Import Tests`, `Spring Security Config`, `Session Cookie Filter Tests`, `Session Cookie Builder`, `Import Job Executor`, `REST Controllers`, `Session Cookie Auth Wiring`?**
  _High betweenness centrality (0.017) - this node is a cross-community bridge._
- **Should `Bootstrap and Seeding` be split into smaller, more focused modules?**
  _Cohesion score 0.06426484907497566 - nodes in this community are weakly interconnected._
- **Why does `Rivalry` connect `Rivalry Model` to `Bootstrap and Seeding`, `Base Repository Core`, `Audit Actor Repositories`, `Repository Transactions`, `CFBD Calendar Venues`, `CFBD Import Tests`, `Auditable Import Documents`, `Audit Fields and Venue`, `Search Text Helper`, `REST Controllers`?**
  _High betweenness centrality (0.014) - this node is a cross-community bridge._
- **Should `Base Repository Core` be split into smaller, more focused modules?**
  _Cohesion score 0.05824561403508772 - nodes in this community are weakly interconnected._