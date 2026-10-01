# Seed

Loads playable reference data into Firestore: SEC venues and teams, rivalries, the 2026 sample season, 18 themes, and optionally one bootstrap manager.

The entry points are `tools/seed/seed.cmd` (Windows) and `tools/seed/seed` (bash). Both run `SeedMain` through the Maven wrapper. Java reads `apps/backend/.env` when that file exists. An OS environment variable wins over the file. `--email=you@example.com` wins over `PICKEM_BOOTSTRAP_MANAGER_EMAIL`.

No pick documents are written. No password field is written. The script does not create a Firebase Auth user.

## Emulator

1. Copy `apps/backend/.env.example` to `apps/backend/.env`.
2. Start the Firestore and Auth emulators (`npx firebase emulators:start --only firestore,auth`). Firestore is `127.0.0.1:8085`. Auth is `127.0.0.1:9099`.
3. Sign in once with the Google account that should be the bootstrap manager, or create that user in the Auth emulator, and set `PICKEM_BOOTSTRAP_MANAGER_EMAIL`.
4. Run `tools\seed\seed.cmd` or `./tools/seed/seed`.

Leave the email blank to load reference data only. The process still exits 0.

## Real project, once

Unset `FIRESTORE_EMULATOR_HOST` so the client uses application default credentials. Set `GOOGLE_CLOUD_PROJECT` to the real project. The bootstrap Google account must already exist in Firebase Auth (sign in once). Then run the same script.

The process prints the emulator host when it is using one, or the project id and "application default credentials" when it is not.

## Bootstrap manager

When the email is set and Firebase Auth has that user, the seed sets custom claims `player` and `manager` and upserts `users/{uid}`. On a re-run it refreshes the email, roles, and claims, and keeps an existing nickname, theme, first name, and last name.

When the email is set and no Auth user exists, reference data is still written, then the process exits 1. Sign in with that Google account and run the seed again. The seed never creates a password user.

## Re-run and reset

Document ids are stable. A re-run updates those documents, keeps `createDate` and `createUser`, and increments `version`. It also finishes a seed that stopped partway through. It does not delete extra documents, picks, or another season's `isCurrent` flag.

A team that already exists under a different document id and the same team name is not removed. Uniqueness is enforced by the API, not by a Firestore constraint.

To wipe the emulator, stop it and delete its data, or clear the project in the Emulator UI at <http://127.0.0.1:4000>. There is no reset flag.
