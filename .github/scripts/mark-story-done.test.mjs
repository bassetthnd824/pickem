import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { describe, test } from "node:test";
import { fileURLToPath } from "node:url";

import {
  fetchCommitMessages,
  markStoryDone,
  nextPageUrl,
  resolveStoryId,
} from "./mark-story-done.mjs";

const storiesPath = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../docs/user-stories.md",
);

describe("resolveStoryId", () => {
  test("uses the branch and an agreeing commit trailer", () => {
    const resolved = resolveStoryId({
      branch: "US-14-switchable-themes",
      title: "feat(frontend): apply switchable Light, Dark, and SEC themes",
      body: [
        "US-14 applies the 18 themes.",
        "The account picker stays with US-19, US-15, and US-23.",
      ].join("\n"),
      commitMessages: [
        "feat(frontend): apply switchable Light, Dark, and SEC themes\n\nUS-14\n",
      ],
    });
    assert.equal(resolved.reason, "unique");
    assert.equal(resolved.storyId, "US-14");
  });

  test("ignores prose when the pull request names no story", () => {
    const resolved = resolveStoryId({
      branch: "ci/bump-actions-node-24",
      title: "ci: bump setup-java and setup-node off node 20",
      body: "Follow-up for US-15 and US-31.",
      commitMessages: ["ci: bump actions\n\nNo story in this change.\n"],
    });
    assert.equal(resolved.reason, "none");
    assert.equal(resolved.storyId, null);
  });

  test("reads a story from the branch prefix, a title token, or a lone line", () => {
    assert.equal(
      resolveStoryId({
        branch: "us-10-review-fixes",
        title: "fix: review",
        body: "",
        commitMessages: [],
      }).storyId,
      "US-10",
    );
    assert.equal(
      resolveStoryId({
        branch: "feat/US-01-bootstrap-nx-monorepo",
        title: "feat(workspace): bootstrap nx monorepo",
        body: "",
        commitMessages: [],
      }).storyId,
      "US-01",
    );
    assert.equal(
      resolveStoryId({
        branch: "docs/mark-US-01-done",
        title: "docs: mark US-01 as done",
        body: "",
        commitMessages: [],
      }).storyId,
      "US-01",
    );
    assert.equal(
      resolveStoryId({
        branch: "chore-normalize-line-endings",
        title: "chore: enforce LF",
        body: "Notes.\n\nUS-07\n",
        commitMessages: [],
      }).storyId,
      "US-07",
    );
  });

  test("treats US-5 and US-05 as the same story", () => {
    const resolved = resolveStoryId({
      branch: "US-5-seasons",
      title: "feat: seasons",
      body: "",
      commitMessages: ["feat: seasons\n\nUS-05\n"],
    });
    assert.equal(resolved.reason, "unique");
    assert.equal(resolved.storyId, "US-05");
  });

  test("does nothing when explicit ids disagree", () => {
    const resolved = resolveStoryId({
      branch: "US-14-switchable-themes",
      title: "feat: themes",
      body: "",
      commitMessages: ["feat: themes\n\nUS-15\n"],
    });
    assert.equal(resolved.reason, "conflict");
    assert.deepEqual(resolved.ids, ["US-14", "US-15"]);
    assert.equal(resolved.storyId, null);
  });
});

describe("fetchCommitMessages", () => {
  test("follows the next link", async () => {
    const pages = new Map([
      [
        "https://api.github.com/repos/acme/pickem/pulls/7/commits?per_page=100",
        {
          body: [{ commit: { message: "feat: one\n\nUS-07" } }],
          link: '<https://example.test/page-2>; rel="next"',
        },
      ],
      [
        "https://example.test/page-2",
        {
          body: [{ commit: { message: "fix: two\n\nUS-07" } }],
          link: null,
        },
      ],
    ]);
    const fetchImpl = async (url, options) => {
      assert.equal(options.headers.Authorization, "Bearer token");
      const page = pages.get(url);
      assert.ok(page, url);
      return {
        ok: true,
        status: 200,
        json: async () => page.body,
        text: async () => "",
        headers: {
          get: (name) => (name.toLowerCase() === "link" ? page.link : null),
        },
      };
    };
    const messages = await fetchCommitMessages(
      "acme/pickem",
      7,
      "token",
      fetchImpl,
    );
    assert.deepEqual(messages, ["feat: one\n\nUS-07", "fix: two\n\nUS-07"]);
  });
});

test("nextPageUrl reads rel=next", () => {
  assert.equal(
    nextPageUrl(
      '<https://api.github.com/last>; rel="last", <https://api.github.com/next>; rel="next"',
    ),
    "https://api.github.com/next",
  );
  assert.equal(nextPageUrl(null), null);
});

describe("markStoryDone", () => {
  const prUrl = "https://github.com/bassetthnd824/pickem/pull/99";

  const fixture = [
    "# User stories",
    "",
    "## Index",
    "",
    "| ID    | Title                                                    | Epic                  | Status | Predecessors                                 | Successors                                             |",
    "| ----- | -------------------------------------------------------- | --------------------- | ------ | -------------------------------------------- | ------------------------------------------------------ |",
    "| US-01 | Bootstrap the Nx monorepo                                | A Foundation          | Done   | —                                            | US-03, US-13                                           |",
    "| US-31 | Proxy browser API calls through Next.js with Axios       | F Frontend shell      | —      | US-04, US-13                                 | US-15, US-25, US-33                                    |",
    "",
    "---",
    "",
    "## Dependency graph",
    "",
    "US31[US-31 Next.js Axios BFF]",
    "",
    "### US-01 — Bootstrap the Nx monorepo",
    "",
    "**Status:** Done ([PR #1](https://github.com/bassetthnd824/pickem/pull/1)).",
    "",
    "**User story.** As a developer, I want a workspace.",
    "",
    "### US-31 — Proxy browser API calls through Next.js with Axios",
    "",
    "**User story.** As a player, I want the browser to call only Next.js.",
    "",
    "**Acceptance criteria.**",
    "",
    "- Browser network traces stay on the Next.js origin.",
    "",
    "**Predecessors:** US-04, US-13.",
    "",
  ].join("\n");

  test("sets the index cell and inserts the section status", () => {
    const { markdown, changed } = markStoryDone(fixture, {
      storyId: "US-31",
      prNumber: 99,
      prUrl,
    });
    assert.equal(changed, true);
    const lines = markdown.split("\n");
    const doneRow = lines.find((line) => line.startsWith("| US-01 |"));
    const openRow = lines.find((line) => line.startsWith("| US-31 |"));
    assert.equal(openRow.split("|")[4], doneRow.split("|")[4]);
    assert.match(openRow, /\| US-04, US-13\s+\| US-15, US-25, US-33\s+\|/);
    const heading = lines.indexOf(
      "### US-31 — Proxy browser API calls through Next.js with Axios",
    );
    assert.equal(
      lines[heading + 2],
      "**Status:** Done ([PR #99](https://github.com/bassetthnd824/pickem/pull/99)).",
    );
    assert.equal(
      lines[heading + 4],
      "**User story.** As a player, I want the browser to call only Next.js.",
    );
    assert.ok(
      lines.includes("- Browser network traces stay on the Next.js origin."),
    );
    assert.equal(
      lines.filter((line) => line.startsWith("**Status:**")).length,
      2,
    );
  });

  test("leaves an already done story and its original pull request link", () => {
    const { markdown, changed } = markStoryDone(fixture, {
      storyId: "US-01",
      prNumber: 99,
      prUrl,
    });
    assert.equal(changed, false);
    assert.equal(markdown, fixture);
  });

  test("adds a section status when the index is already Done", () => {
    const lines = fixture
      .split("\n")
      .filter((line) => !line.startsWith("**Status:**"));
    const source = lines.join("\n");
    const { markdown, changed } = markStoryDone(source, {
      storyId: "US-01",
      prNumber: 4,
      prUrl: "https://github.com/bassetthnd824/pickem/pull/4",
    });
    assert.equal(changed, true);
    assert.match(
      markdown,
      /\*\*Status:\*\* Done \(\[PR #4\]\(https:\/\/github.com\/bassetthnd824\/pickem\/pull\/4\)\)\./,
    );
    const doneCell = fixture
      .split("\n")
      .find((line) => line.startsWith("| US-01 |"))
      .split("|")[4];
    assert.equal(
      markdown
        .split("\n")
        .find((line) => line.startsWith("| US-01 |"))
        .split("|")[4],
      doneCell,
    );
  });

  test("replaces a section status when the index is still open", () => {
    const source = fixture.replace(
      "**User story.** As a player, I want the browser to call only Next.js.",
      "**Status:** In progress.\n\n**User story.** As a player, I want the browser to call only Next.js.",
    );
    const { markdown } = markStoryDone(source, {
      storyId: "US-31",
      prNumber: 99,
      prUrl,
    });
    assert.equal(markdown.includes("**Status:** In progress."), false);
    assert.match(markdown, /\*\*Status:\*\* Done \(\[PR #99\]/);
  });

  test("rejects a story that is not in the index", () => {
    assert.throws(
      () =>
        markStoryDone(fixture, {
          storyId: "US-99",
          prNumber: 1,
          prUrl: "https://github.com/acme/pickem/pull/1",
        }),
      /no index row for US-99/,
    );
  });

  test("is idempotent", () => {
    const once = markStoryDone(fixture, {
      storyId: "US-31",
      prNumber: 99,
      prUrl,
    });
    const twice = markStoryDone(once.markdown, {
      storyId: "US-31",
      prNumber: 99,
      prUrl,
    });
    assert.equal(twice.changed, false);
    assert.equal(twice.markdown, once.markdown);
  });
});

test(
  "marks an open story in docs/user-stories.md without touching its neighbors",
  {
    skip: existsSync(storiesPath)
      ? false
      : "docs/user-stories.md is not next to this test copy",
  },
  () => {
    const source = readFileSync(storiesPath, "utf8");
    const prUrl = "https://github.com/bassetthnd824/pickem/pull/99";
    const { markdown, changed } = markStoryDone(source, {
      storyId: "US-31",
      prNumber: 99,
      prUrl,
    });
    assert.equal(changed, true);
    const before = source.split("\n");
    const after = markdown.split("\n");
    const cell = (lines, id) =>
      lines.find((line) => line.startsWith(`| ${id} |`)).split("|")[4];
    assert.equal(cell(after, "US-31"), cell(before, "US-01"));
    assert.equal(cell(after, "US-15"), cell(before, "US-15"));
    assert.equal(
      after.find((line) => line.startsWith("| US-15 |")),
      before.find((line) => line.startsWith("| US-15 |")),
    );
    const heading = after.indexOf(
      before.find((line) => line.startsWith("### US-31 ")),
    );
    assert.equal(after[heading + 1], "");
    assert.equal(after[heading + 2], `**Status:** Done ([PR #99](${prUrl})).`);
    assert.equal(after[heading + 3], "");
    assert.equal(
      after[heading + 4],
      before[
        before.indexOf(before.find((line) => line.startsWith("### US-31 "))) + 2
      ],
    );
    const us15Heading = before.find((line) => line.startsWith("### US-15 "));
    const us15Before = before.indexOf(us15Heading);
    const us15After = after.indexOf(us15Heading);
    const nextHeading = (lines, from) =>
      lines.findIndex((line, index) => index > from && line.startsWith("### "));
    assert.deepEqual(
      after.slice(us15After, nextHeading(after, us15After)),
      before.slice(us15Before, nextHeading(before, us15Before)),
    );
    const again = markStoryDone(source, {
      storyId: "US-01",
      prNumber: 99,
      prUrl,
    });
    assert.equal(again.changed, false);
  },
);
