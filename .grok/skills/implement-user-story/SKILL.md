---
name: implement-user-story
description: >-
  Implement one Kenney's Pickem user story end to end. Read the requested
  story from docs/user-stories.md, read other documents as needed from the
  docs folder, stop and name any predecessor that is not marked done, and
  otherwise branch from main, implement only that story, run the review
  skill, fix the findings, commit with a conventional message that includes
  the story id, push to GitHub, and open a pull request. Use when the user
  runs /implement-user-story, or says "implement user story", "work on
  US-NN", "pick up story", "finish this story and open a PR", or names a
  story id from docs/user-stories.md.
---

# Implement a user story

Ship exactly one story from `docs/user-stories.md` as a pull request. Follow `AGENTS.md`. Do not start a second story in the same run.

## 1. Choose the story

- An id (`US-05`, `US-5`) or a title in the request selects that story. Normalize an unpadded id to the index form `US-NN`.
- "Next" selects the first story in **Suggested implementation order** whose index status is not `Done` and whose predecessors are all `Done`. If that section does not contain one, walk the index from top to bottom with the same rule.
- Otherwise ask which story to implement. Do not guess.

Read `docs/user-stories.md`. Load the index row and the `### US-NN — …` section. If the id is not in the index, say so and stop.

If that story's index **Status** is already `Done`, say it is already on `main` and stop.

## 2. Stop when a predecessor is open

`Done` means the index **Status** cell is exactly `Done`. An em dash, a blank, or any other value is not done. Checked boxes in the story body do not count. A story is `Done` only when its work is on `main`.

Collect predecessor ids from both the index **Predecessors** cell and the story section's **Predecessors:** line.

- `—`, `-`, or `none` means no ids from that source.
- Split the rest on commas.
- Expand a range such as `US-16–US-19` (en dash or hyphen) to every integer id in that span, zero-padded to two digits.
- Deduplicate.

Look up each id in the index. A missing id is not done.

If any predecessor is not `Done`, stop before branching or editing. Tell the user the requested id and each open predecessor with the status text from the index (or "missing from the index"). Do not offer to start anyway.

If the index list and the section list differ, mention the mismatch. Still gate on the union.

## 3. Read only what this story needs

Read that story's user story, description, acceptance criteria, and source. Open another file under `docs/` when this story, its source line, or an acceptance criterion points at it.

Implement only this story's acceptance criteria. Do not pull a successor's behavior out of a shared doc. Obey `AGENTS.md`, including the legacy `src/` freeze until the story being implemented is US-26, and the Java null-analysis rules when the story touches backend Java.

Do not set this story's index **Status** to `Done` in the PR. After the pull request merges to `main`, `.github/workflows/mark-story-done.yml` sets that cell and the section **Status:** line when the pull request names exactly one story. Do not open a follow-up pull request to mark it done.

## 4. Branch from main

Require a clean working tree (`git status --porcelain` empty). If it is dirty, stop and name the dirty paths. Do not stash, reset, or discard.

```text
git fetch origin
git checkout main
git pull --ff-only origin main
git checkout -b US-NN-short-slug
```

`US-NN-short-slug` is the branch form required by `AGENTS.md`. Keep the slug short and kebab-case, taken from the story title.

If that branch name already exists locally or on `origin`, stop and say so. Do not reset or reuse it. If `git pull --ff-only` fails, stop.

## 5. Implement and verify

Satisfy every acceptance criterion. Leave out drive-by refactors and files the story does not need.

Run the checks the acceptance criteria name. For workspace build, test, lint, and serve tasks, follow the `nx-run-tasks` skill and run them through `npx nx`. For scaffolding, follow the `nx-generate` skill first. When the story changes a web UI, verify the affected flow in the browser before review.

Do not commit if a required check is still failing.

## 6. Review, then fix

Before committing, read and follow the `review` skill in local mode (`/review --local`). Let that skill's reviewer report the findings. Do not review the diff yourself in place of that skill, and do not ask the reviewer to edit code.

Then fix every finding it reported (bug, suggestion, and nit).

If those fixes change code, run `/review --local` once more and fix whatever that pass reports. After that second fix pass, commit. Do not open a third review.

## 7. Commit, push, and open the pull request

Invoking this skill authorizes one commit, one push, and one pull request for this story. Do not merge.

Stage only the story's files. Do not commit `.env` or other secrets.

Commit with the conventional message format in `AGENTS.md`: type, optional scope, imperative summary, body, and the story id on its own final line (`US-NN`). The subject must pass `commitlint.config.cjs`. Write the message to a temp file and commit with `git commit -F`. Do not skip hooks.

```text
git push -u origin HEAD
gh pr create --base main --title "<type>(<optional-scope>): <imperative summary>" --body-file <temp-file>
```

The PR title must pass the semantic PR check (same allowed types as `AGENTS.md`; scope optional). Use a temp file for the body, then delete it. The body states what the story asked for, how each acceptance criterion was met, how you verified it, and ends with the story id on its own line.

If `gh` is not authenticated or the push or PR creation fails, stop and report the error. Leave the local commit in place.

## 8. Report

On a predecessor block, the report is only the open-predecessor list.

Otherwise report the branch name, the commit subject, the PR URL, and that the index status stays unchanged until the merge workflow marks the story done on `main`.
