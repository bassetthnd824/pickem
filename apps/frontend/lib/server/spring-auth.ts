import { NextResponse } from "next/server";
import { parseAuthProfile, type AuthProfile } from "../auth-profile";
import { SESSION_COOKIE_NAME } from "../session-gate";

/**
 * Forwards only sign-in, logout, and the current profile.
 * Game and manager calls are not forwarded here.
 */

const DEFAULT_SPRING_API_BASE_URL = "http://localhost:8080";

export const AUTH_PATHS = {
  session: "/api/auth/session",
  logout: "/api/auth/logout",
  me: "/api/auth/me",
} as const;

export type AuthPath = (typeof AUTH_PATHS)[keyof typeof AUTH_PATHS];

type ParsedCookie = {
  name: string;
  value: string;
  httpOnly: boolean;
  secure: boolean;
  sameSite: "lax" | "strict" | "none";
  maxAge?: number;
};

export function springApiBaseUrl(): string {
  const raw = process.env.SPRING_API_BASE_URL;
  const value = raw === undefined ? DEFAULT_SPRING_API_BASE_URL : raw.trim();
  if (value === "") {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  if (
    (url.protocol !== "http:" && url.protocol !== "https:") ||
    url.username !== "" ||
    url.password !== ""
  ) {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  return url.origin;
}

export function parseSetCookie(header: string): ParsedCookie | null {
  const parts = header
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part !== "");
  const first = parts[0];
  if (!first) {
    return null;
  }
  const eq = first.indexOf("=");
  if (eq <= 0) {
    return null;
  }
  const name = first.slice(0, eq).trim();
  let value = first.slice(eq + 1).trim();
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    value = value.slice(1, -1);
  }
  if (name === "") {
    return null;
  }
  let httpOnly = false;
  let secure = false;
  let sameSite: ParsedCookie["sameSite"] = "lax";
  let maxAge: number | undefined;
  for (const part of parts.slice(1)) {
    const keyEq = part.indexOf("=");
    const key = (keyEq === -1 ? part : part.slice(0, keyEq))
      .trim()
      .toLowerCase();
    const raw = keyEq === -1 ? "" : part.slice(keyEq + 1).trim();
    if (key === "httponly") {
      httpOnly = true;
    } else if (key === "secure") {
      secure = true;
    } else if (key === "samesite") {
      const normalized = raw.toLowerCase();
      if (
        normalized === "lax" ||
        normalized === "strict" ||
        normalized === "none"
      ) {
        sameSite = normalized;
      }
    } else if (key === "max-age") {
      const parsed = Number(raw);
      if (Number.isFinite(parsed)) {
        maxAge = parsed;
      }
    }
  }
  return { name, value, httpOnly, secure, sameSite, maxAge };
}

/**
 * Drops `Domain` and forces `Path=/` so the browser keeps the cookie
 * host-only on the Next.js origin.
 */
export function hostOnlySetCookie(header: string): string {
  const parts = header
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part !== "");
  const first = parts[0];
  if (!first) {
    return header;
  }
  const kept = [first];
  let sawPath = false;
  for (const part of parts.slice(1)) {
    const keyEq = part.indexOf("=");
    const key = (keyEq === -1 ? part : part.slice(0, keyEq))
      .trim()
      .toLowerCase();
    if (key === "domain") {
      continue;
    }
    if (key === "path") {
      kept.push("Path=/");
      sawPath = true;
      continue;
    }
    kept.push(part);
  }
  if (!sawPath) {
    kept.push("Path=/");
  }
  return kept.join("; ");
}

export function authError(status: number, detail: string): NextResponse {
  return NextResponse.json({ detail }, { status });
}

/** Host-only clear. Used when Spring rejects logout and does not clear `__session`. */
export function clearSessionResponse(): NextResponse {
  const response = new NextResponse(null, { status: 204 });
  response.cookies.set({
    name: SESSION_COOKIE_NAME,
    value: "",
    path: "/",
    httpOnly: true,
    secure: true,
    sameSite: "lax",
    maxAge: 0,
  });
  return response;
}

/**
 * A rejected cookie never reaches Spring logout, so the 401 has no
 * clearing `Set-Cookie`. Replace it so the browser can leave the shell.
 */
export function finishLogout(upstream: NextResponse): NextResponse {
  if (upstream.status === 401 || upstream.status === 403) {
    return clearSessionResponse();
  }
  return upstream;
}

export function authProxyResponse(
  status: number,
  body: ArrayBuffer,
  contentType: string | null,
  setCookies: readonly string[],
): NextResponse {
  const response = new NextResponse(status === 204 ? null : body, { status });
  if (contentType) {
    response.headers.set("content-type", contentType);
  }
  for (const raw of setCookies) {
    const parsed = parseSetCookie(hostOnlySetCookie(raw));
    if (!parsed) {
      continue;
    }
    response.cookies.set({
      name: parsed.name,
      value: parsed.value,
      path: "/",
      httpOnly: parsed.httpOnly,
      secure: parsed.secure,
      sameSite: parsed.sameSite,
      ...(parsed.maxAge === undefined ? {} : { maxAge: parsed.maxAge }),
    });
  }
  return response;
}

function readSetCookies(headers: Headers): string[] {
  if (typeof headers.getSetCookie === "function") {
    const values = headers.getSetCookie();
    if (values.length > 0) {
      return values;
    }
  }
  const single = headers.get("set-cookie");
  return single ? [single] : [];
}

export async function forwardAuth(
  request: Request,
  path: AuthPath,
): Promise<NextResponse> {
  let base: string;
  try {
    base = springApiBaseUrl();
  } catch {
    return authError(500, "Sign-in service is not configured");
  }

  const headers = new Headers();
  headers.set("accept", "application/json");
  const cookie = request.headers.get("cookie");
  if (cookie) {
    headers.set("cookie", cookie);
  }
  let body: string | undefined;
  if (request.method !== "GET" && request.method !== "HEAD") {
    body = await request.text();
    const contentType = request.headers.get("content-type");
    if (body !== "" && contentType) {
      headers.set("content-type", contentType);
    }
  }

  let upstream: Response;
  try {
    upstream = await fetch(new URL(path, base), {
      method: request.method,
      headers,
      body,
      cache: "no-store",
      redirect: "manual",
    });
  } catch {
    return authError(503, "Sign-in service is unavailable");
  }

  const payload = await upstream.arrayBuffer();
  return authProxyResponse(
    upstream.status,
    payload,
    upstream.headers.get("content-type"),
    readSetCookies(upstream.headers),
  );
}

export async function fetchAuthProfile(
  sessionCookie: string | undefined,
): Promise<AuthProfile | null> {
  if (!sessionCookie || /[;\s,]/.test(sessionCookie)) {
    return null;
  }
  let base: string;
  try {
    base = springApiBaseUrl();
  } catch {
    return null;
  }
  try {
    const response = await fetch(new URL(AUTH_PATHS.me, base), {
      method: "GET",
      headers: {
        accept: "application/json",
        cookie: `${SESSION_COOKIE_NAME}=${sessionCookie}`,
      },
      cache: "no-store",
      redirect: "manual",
    });
    if (!response.ok) {
      return null;
    }
    return parseAuthProfile(await response.json());
  } catch {
    return null;
  }
}
