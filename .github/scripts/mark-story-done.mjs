/**
 * Mark one user story done in docs/user-stories.md.
 *
 * A pull request specifies a story only in these places:
 * - head branch, as US-NN at the start or after a slash (US-15-auth, feat/US-01-bootstrap)
 * - title, as a US-NN token
 * - a line that is only US-NN in the body or a commit message
 *
 * Story ids mentioned inside a sentence do not count. Zero ids, or more than
 * one distinct id, is not a single story.
 */

import {
  appendFileSync,
  readFileSync,
  realpathSync,
  writeFileSync,
} from "node:fs";
import { fileURLToPath } from "node:url";

const STORY_FILE = "docs/user-stories.md";
const LINE_ID = /^US-(\d{1,3})$/i;
const TITLE_ID = /\bUS-(\d{1,3})\b/gi;
const BRANCH_ID = /(?:^|\/)US-(\d{1,3})(?=-|$)/i;

/**
 * @param {string | number | null | undefined} raw
 * @returns {string | null}
 */
export function normalizeStoryId(raw) {
  const match =
    String(raw ?? "").match(/^US-(\d{1,3})$/i) ??
    String(raw ?? "").match(/^(\d{1,3})$/);
  if (!match) {
    return null;
  }
  const number = Number(match[1]);
  if (!Number.isInteger(number) || number < 1) {
    return null;
  }
  return `US-${String(number).padStart(2, "0")}`;
}

/**
 * @param {string | null | undefined} branch
 * @returns {string | null}
 */
export function storyIdFromBranch(branch) {
  if (!branch) {
    return null;
  }
  const match = branch.match(BRANCH_ID);
  return match ? normalizeStoryId(match[1]) : null;
}

/**
 * @param {string | null | undefined} title
 * @returns {string[]}
 */
export function storyIdsFromTitle(title) {
  if (!title) {
    return [];
  }
  const ids = [];
  for (const match of title.matchAll(TITLE_ID)) {
    const id = normalizeStoryId(match[1]);
    if (id) {
      ids.push(id);
    }
  }
  return ids;
}

/**
 * @param {string | null | undefined} text
 * @returns {string[]}
 */
export function storyIdsFromExactLines(text) {
  if (!text) {
    return [];
  }
  const ids = [];
  for (const line of text.split(/\r?\n/)) {
    const match = line.trim().match(LINE_ID);
    if (!match) {
      continue;
    }
    const id = normalizeStoryId(match[1]);
    if (id) {
      ids.push(id);
    }
  }
  return ids;
}

/**
 * @param {{
 *   branch?: string | null,
 *   title?: string | null,
 *   body?: string | null,
 *   commitMessages?: string[] | null,
 * }} input
 * @returns {string[]}
 */
export function collectExplicitStoryIds(input) {
  const ids = new Set();
  const branchId = storyIdFromBranch(input.branch);
  if (branchId) {
    ids.add(branchId);
  }
  for (const id of storyIdsFromTitle(input.title)) {
    ids.add(id);
  }
  for (const id of storyIdsFromExactLines(input.body)) {
    ids.add(id);
  }
  for (const message of input.commitMessages ?? []) {
    for (const id of storyIdsFromExactLines(message)) {
      ids.add(id);
    }
  }
  return [...ids].sort();
}

/**
 * @param {{
 *   branch?: string | null,
 *   title?: string | null,
 *   body?: string | null,
 *   commitMessages?: string[] | null,
 * }} input
 * @returns {{ storyId: string | null, reason: "none" | "unique" | "conflict", ids: string[] }}
 */
export function resolveStoryId(input) {
  const ids = collectExplicitStoryIds(input);
  if (ids.length === 0) {
    return { storyId: null, reason: "none", ids };
  }
  if (ids.length > 1) {
    return { storyId: null, reason: "conflict", ids };
  }
  return { storyId: ids[0], reason: "unique", ids };
}

/**
 * @param {string | null | undefined} link
 * @returns {string | null}
 */
export function nextPageUrl(link) {
  if (!link) {
    return null;
  }
  for (const part of link.split(",")) {
    const match = part.match(/<([^>]+)>;\s*rel="next"/);
    if (match) {
      return match[1];
    }
  }
  return null;
}

/**
 * @param {string} repo owner/name
 * @param {number} prNumber
 * @param {string} token
 * @param {typeof fetch} [fetchImpl]
 * @returns {Promise<string[]>}
 */
export async function fetchCommitMessages(
  repo,
  prNumber,
  token,
  fetchImpl = fetch,
) {
  const messages = [];
  let url = `https://api.github.com/repos/${repo}/pulls/${prNumber}/commits?per_page=100`;
  while (url) {
    const response = await fetchImpl(url, {
      headers: {
        Accept: "application/vnd.github+json",
        Authorization: `Bearer ${token}`,
        "User-Agent": "pickem-mark-story-done",
        "X-GitHub-Api-Version": "2022-11-28",
      },
    });
    if (!response.ok) {
      const detail = await response.text();
      throw new Error(
        `GitHub API ${response.status} reading commits for PR ${prNumber}: ${detail.slice(0, 300)}`,
      );
    }
    const commits = await response.json();
    if (!Array.isArray(commits)) {
      throw new Error(
        `GitHub API returned an unexpected commit list for PR ${prNumber}`,
      );
    }
    for (const commit of commits) {
      messages.push(commit?.commit?.message ?? "");
    }
    url = nextPageUrl(response.headers.get("link"));
  }
  return messages;
}

/**
 * @param {number} prNumber
 * @param {string} prUrl
 * @returns {string}
 */
export function statusLine(prNumber, prUrl) {
  return `**Status:** Done ([PR #${prNumber}](${prUrl})).`;
}

/**
 * @param {string} cell
 * @returns {string}
 */
export function padDone(cell) {
  const label = " Done";
  if (label.length >= cell.length) {
    return `${label} `;
  }
  return label + " ".repeat(cell.length - label.length);
}

/**
 * @param {string} markdown
 * @param {{ storyId: string, prNumber: number, prUrl: string }} update
 * @returns {{ markdown: string, changed: boolean }}
 */
export function markStoryDone(markdown, update) {
  const storyId = normalizeStoryId(update.storyId);
  if (!storyId) {
    throw new Error(`Invalid story id: ${update.storyId}`);
  }
  if (!Number.isInteger(update.prNumber) || update.prNumber < 1) {
    throw new Error(`Invalid pull request number: ${update.prNumber}`);
  }
  if (
    !new RegExp(`^https://[^\\s)]+/pull/${update.prNumber}$`).test(update.prUrl)
  ) {
    throw new Error(`Invalid pull request URL: ${update.prUrl}`);
  }

  const newline = markdown.match(/\r?\n/)?.[0] ?? "\n";
  const lines = markdown.split(/\r?\n/);
  const rowIndex = findIndexRow(lines, storyId);
  const headingIndex = findHeading(lines, storyId);
  const alreadyDone = markIndexDone(lines, rowIndex, storyId);
  upsertStatusLine(
    lines,
    headingIndex,
    statusLine(update.prNumber, update.prUrl),
    alreadyDone,
  );

  const updated = lines.join(newline);
  return { markdown: updated, changed: updated !== markdown };
}

/**
 * @param {string[]} lines
 * @param {string} storyId
 * @returns {number}
 */
function findIndexRow(lines, storyId) {
  const start = lines.findIndex((line) => line.trim() === "## Index");
  if (start === -1) {
    throw new Error(`${STORY_FILE} has no ## Index section`);
  }
  let end = lines.length;
  for (let i = start + 1; i < lines.length; i += 1) {
    if (lines[i].startsWith("## ")) {
      end = i;
      break;
    }
  }
  const rowPattern = new RegExp(`^\\|\\s*${storyId}\\s*\\|`);
  let rowIndex = -1;
  for (let i = start + 1; i < end; i += 1) {
    if (!rowPattern.test(lines[i])) {
      continue;
    }
    if (rowIndex !== -1) {
      throw new Error(
        `${STORY_FILE} has more than one index row for ${storyId}`,
      );
    }
    rowIndex = i;
  }
  if (rowIndex === -1) {
    throw new Error(`${STORY_FILE} has no index row for ${storyId}`);
  }
  return rowIndex;
}

/**
 * @param {string[]} lines
 * @param {string} storyId
 * @returns {number}
 */
function findHeading(lines, storyId) {
  const headingPattern = new RegExp(`^### ${storyId}(?:\\s|$)`);
  let headingIndex = -1;
  for (let i = 0; i < lines.length; i += 1) {
    if (!headingPattern.test(lines[i])) {
      continue;
    }
    if (headingIndex !== -1) {
      throw new Error(`${STORY_FILE} has more than one section for ${storyId}`);
    }
    headingIndex = i;
  }
  if (headingIndex === -1) {
    throw new Error(`${STORY_FILE} has no section heading for ${storyId}`);
  }
  return headingIndex;
}

/**
 * @param {string[]} lines
 * @param {number} rowIndex
 * @param {string} storyId
 * @returns {boolean} true when the index cell was already Done
 */
function markIndexDone(lines, rowIndex, storyId) {
  const parts = lines[rowIndex].split("|");
  if (parts.length < 6) {
    throw new Error(`Index row for ${storyId} has no status column`);
  }
  const alreadyDone = parts[4].trim() === "Done";
  parts[4] = padDone(parts[4]);
  lines[rowIndex] = parts.join("|");
  return alreadyDone;
}

/**
 * Insert the section status under the heading. When the index was already
 * Done, keep an existing status line so a later pull request does not
 * replace the original link.
 *
 * @param {string[]} lines
 * @param {number} headingIndex
 * @param {string} nextStatus
 * @param {boolean} alreadyDone
 */
function upsertStatusLine(lines, headingIndex, nextStatus, alreadyDone) {
  let cursor = headingIndex + 1;
  if (lines[cursor] === "") {
    cursor += 1;
  }
  if (cursor < lines.length && lines[cursor].startsWith("**Status:**")) {
    if (!alreadyDone) {
      lines[cursor] = nextStatus;
    }
    return;
  }
  const block =
    lines[cursor - 1] === "" ? [nextStatus, ""] : ["", nextStatus, ""];
  lines.splice(cursor, 0, ...block);
}

function setOutput(name, value) {
  const line = `${name}=${value}`;
  const file = process.env.GITHUB_OUTPUT;
  if (!file) {
    console.log(line);
    return;
  }
  appendFileSync(file, `${line}\n`);
}

/**
 * @param {string[]} argv
 * @returns {Record<string, string | boolean>}
 */
function parseArgs(argv) {
  /** @type {Record<string, string | boolean>} */
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === "--resolve") {
      args.resolve = true;
    } else if (token === "--apply") {
      args.apply = true;
    } else if (
      token === "--story" ||
      token === "--pr" ||
      token === "--url" ||
      token === "--file"
    ) {
      const value = argv[i + 1];
      if (!value || value.startsWith("--")) {
        throw new Error(`${token} needs a value`);
      }
      args[token.slice(2)] = value;
      i += 1;
    } else {
      throw new Error(`Unknown argument: ${token}`);
    }
  }
  return args;
}

async function resolveFromEvent() {
  const eventPath = process.env.GITHUB_EVENT_PATH;
  const token = process.env.GITHUB_TOKEN;
  const repo = process.env.GITHUB_REPOSITORY;
  if (!eventPath || !token || !repo) {
    throw new Error(
      "GITHUB_EVENT_PATH, GITHUB_TOKEN, and GITHUB_REPOSITORY are required to resolve a story",
    );
  }
  const event = JSON.parse(readFileSync(eventPath, "utf8"));
  const pullRequest = event.pull_request;
  if (!pullRequest?.merged) {
    setOutput("reason", "none");
    setOutput("story_id", "");
    setOutput("story_ids", "");
    setOutput("pr_number", "");
    setOutput("pr_url", "");
    console.log(
      "Pull request is not merged. Leaving the story index unchanged.",
    );
    return;
  }
  const prNumber = pullRequest.number;
  if (!Number.isInteger(prNumber) || prNumber < 1) {
    throw new Error(`Invalid pull request number in the event: ${prNumber}`);
  }
  const commitMessages = await fetchCommitMessages(repo, prNumber, token);
  const resolved = resolveStoryId({
    branch: pullRequest.head?.ref ?? "",
    title: pullRequest.title ?? "",
    body: pullRequest.body ?? "",
    commitMessages,
  });
  const server = process.env.GITHUB_SERVER_URL ?? "https://github.com";
  const prUrl = `${server}/${repo}/pull/${prNumber}`;
  setOutput("reason", resolved.reason);
  setOutput("story_id", resolved.storyId ?? "");
  setOutput("story_ids", resolved.ids.join(","));
  setOutput("pr_number", String(prNumber));
  setOutput("pr_url", prUrl);
  if (resolved.reason === "unique") {
    console.log(`Pull request specifies ${resolved.storyId}.`);
    return;
  }
  if (resolved.reason === "conflict") {
    console.log(
      `Pull request names more than one user story (${resolved.ids.join(", ")}). Leaving ${STORY_FILE} unchanged.`,
    );
    return;
  }
  console.log(
    `Pull request does not specify a user story. Leaving ${STORY_FILE} unchanged.`,
  );
}

function applyToFile(args) {
  const storyId = normalizeStoryId(String(args.story ?? ""));
  if (!storyId) {
    throw new Error(`Invalid story id: ${args.story}`);
  }
  if (typeof args.pr !== "string" || !/^\d+$/.test(args.pr)) {
    throw new Error(`Invalid pull request number: ${args.pr}`);
  }
  const prNumber = Number(args.pr);
  const prUrl = String(args.url ?? "");
  const file = String(args.file ?? STORY_FILE);
  const current = readFileSync(file, "utf8");
  const updated = markStoryDone(current, { storyId, prNumber, prUrl });
  if (!updated.changed) {
    console.log(`unchanged ${storyId}`);
    return;
  }
  writeFileSync(file, updated.markdown);
  console.log(`changed ${storyId}`);
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.resolve && args.apply) {
    throw new Error("Pass only one of --resolve or --apply");
  }
  if (args.resolve) {
    await resolveFromEvent();
    return;
  }
  if (args.apply) {
    applyToFile(args);
    return;
  }
  throw new Error("Pass --resolve or --apply");
}

function invokedDirectly() {
  const entry = process.argv[1];
  if (!entry) {
    return false;
  }
  try {
    return realpathSync(entry) === realpathSync(fileURLToPath(import.meta.url));
  } catch {
    return false;
  }
}

if (invokedDirectly()) {
  main().catch((error) => {
    console.error(error instanceof Error ? error.message : error);
    process.exitCode = 1;
  });
}
