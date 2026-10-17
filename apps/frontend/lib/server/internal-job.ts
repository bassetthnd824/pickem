import "server-only";
import { createHash, timingSafeEqual } from "node:crypto";
import type { NextResponse } from "next/server";
import { SESSION_COOKIE_NAME } from "../session-gate";
import { verifySchedulerOidc } from "./job-oidc";
import {
  authError,
  proxyApiRequest,
  proxyInternalJobCall,
} from "./spring-proxy";

/** Header the BFF and Spring share. Not the session cookie. */
export const JOB_SECRET_HEADER = "x-pickem-job-secret";

export const INTERNAL_JOBS = new Set(["cfbd-scores", "cfbd-matchups"]);

export type JobDecision = "ok" | "forbidden" | "unauthorized";

export function configuredJobSecret(): string {
  return process.env.PICKEM_INTERNAL_JOB_SECRET?.trim() ?? "";
}

export function jobAudience(): string {
  return process.env.PICKEM_INTERNAL_JOB_AUDIENCE?.trim() ?? "";
}

export function configuredOidcEmails(): readonly string[] {
  const raw = process.env.PICKEM_INTERNAL_JOB_OIDC_EMAILS ?? "";
  const emails: string[] = [];
  for (const part of raw.split(",")) {
    const email = part.trim();
    if (email !== "") {
      emails.push(email);
    }
  }
  return emails;
}

export function secretsMatch(provided: string, expected: string): boolean {
  if (expected === "") {
    return false;
  }
  const actual = createHash("sha256").update(provided, "utf8").digest();
  const required = createHash("sha256").update(expected, "utf8").digest();
  return timingSafeEqual(actual, required);
}

export function jobFromSegments(segments: readonly string[]): string {
  const root = segments[0];
  const kind = segments[1];
  const job = segments[2];
  if (
    segments.length !== 3 ||
    root !== "internal" ||
    kind !== "jobs" ||
    job === undefined
  ) {
    return "";
  }
  return job;
}

function hasSessionCookie(request: Request): boolean {
  const header = request.headers.get("cookie");
  if (!header) {
    return false;
  }
  for (const part of header.split(";")) {
    const eq = part.indexOf("=");
    const name = (eq === -1 ? part : part.slice(0, eq)).trim();
    if (name === SESSION_COOKIE_NAME) {
      return true;
    }
  }
  return false;
}

function bearerToken(request: Request): string | null {
  const header = request.headers.get("authorization");
  if (!header) {
    return null;
  }
  const match = /^Bearer\s+(\S+)$/i.exec(header.trim());
  const token = match?.[1];
  if (!token || token.split(".").length !== 3) {
    return null;
  }
  return token;
}

export async function authorizeInternalJob(
  request: Request,
): Promise<JobDecision> {
  const secret = configuredJobSecret();
  const provided = request.headers.get(JOB_SECRET_HEADER)?.trim() ?? "";
  if (secretsMatch(provided, secret)) {
    return "ok";
  }
  const token = bearerToken(request);
  const audience = jobAudience();
  const allowedEmails = configuredOidcEmails();
  if (token && audience !== "" && allowedEmails.length > 0) {
    try {
      if (await verifySchedulerOidc(token, audience, allowedEmails)) {
        return "ok";
      }
    } catch {
      return hasSessionCookie(request) ? "forbidden" : "unauthorized";
    }
  }
  return hasSessionCookie(request) ? "forbidden" : "unauthorized";
}

function rejected(decision: JobDecision): NextResponse {
  if (decision === "forbidden") {
    return authError(403, "Internal jobs do not accept a session cookie");
  }
  return authError(401, "Internal job credential is required");
}

/**
 * Admits a documented job, then forwards it without the session cookie.
 * Local runs send no Cloud Run identity token. The shared secret, when
 * configured, is what Spring checks off loopback.
 */
export async function handleInternalJob(
  request: Request,
  job: string,
): Promise<NextResponse> {
  if (request.method !== "POST") {
    return authError(405, "Method not allowed");
  }
  const decision = await authorizeInternalJob(request);
  if (decision !== "ok") {
    return rejected(decision);
  }
  if (!INTERNAL_JOBS.has(job)) {
    return authError(404, "Not found");
  }
  return proxyInternalJobCall(request, job, configuredJobSecret());
}

export function dispatchCaughtApi(
  request: Request,
  segments: readonly string[],
): Promise<NextResponse> {
  if (segments[0] === "internal") {
    return handleInternalJob(request, jobFromSegments(segments));
  }
  return proxyApiRequest(request, segments);
}
