import { GAME_LINKS, MANAGER_LINKS, RESTRICTED_PATH } from "./nav-links";

/** Host-only session cookie minted by Spring and copied onto this origin. */
export const SESSION_COOKIE_NAME = "__session";

export const GAME_ROUTE_PREFIXES = [
  ...GAME_LINKS.map((link) => link.href),
  RESTRICTED_PATH,
] as const;

export const MANAGER_ROUTE_PREFIXES = MANAGER_LINKS.map(
  (link) => link.href,
) as readonly string[];

export type SessionClaims = {
  signedIn: boolean;
  isManager: boolean;
};

export type GateDecision = "allow" | "login" | "main" | "restricted";

const SIGNED_OUT: SessionClaims = { signedIn: false, isManager: false };

/**
 * HTML route gate only. The payload is decoded for expiry and claims.
 * It is not signature-checked.
 */
export function readSessionClaims(
  token: string | undefined,
  nowMs = Date.now(),
): SessionClaims {
  const payload = readPayload(token);
  if (!payload) {
    return SIGNED_OUT;
  }
  if (typeof payload.sub !== "string" || payload.sub.trim() === "") {
    return SIGNED_OUT;
  }
  if (
    typeof payload.exp !== "number" ||
    !Number.isFinite(payload.exp) ||
    payload.exp * 1000 <= nowMs
  ) {
    return SIGNED_OUT;
  }
  return { signedIn: true, isManager: payloadIsManager(payload) };
}

export function gateDecision(
  pathname: string,
  claims: SessionClaims,
): GateDecision {
  const path = normalizePath(pathname);
  if (path === "/login" || path === "/") {
    return claims.signedIn ? "main" : "allow";
  }
  if (matchesAny(path, MANAGER_ROUTE_PREFIXES)) {
    if (!claims.signedIn) {
      return "login";
    }
    return claims.isManager ? "allow" : "restricted";
  }
  if (matchesAny(path, GAME_ROUTE_PREFIXES)) {
    return claims.signedIn ? "allow" : "login";
  }
  return "allow";
}

function normalizePath(pathname: string): string {
  if (pathname.length > 1 && pathname.endsWith("/")) {
    return pathname.slice(0, -1);
  }
  return pathname;
}

function matchesAny(path: string, prefixes: readonly string[]): boolean {
  return prefixes.some(
    (prefix) => path === prefix || path.startsWith(`${prefix}/`),
  );
}

function readPayload(
  token: string | undefined,
): Record<string, unknown> | null {
  if (!token || token.trim() === "") {
    return null;
  }
  const parts = token.split(".");
  if (parts.length !== 3 || !parts[1]) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(decodeBase64Url(parts[1]));
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return null;
    }
    return parsed as Record<string, unknown>;
  } catch {
    return null;
  }
}

function payloadIsManager(payload: Record<string, unknown>): boolean {
  if (payload.manager === true) {
    return true;
  }
  return (
    Array.isArray(payload.roles) &&
    payload.roles.some(
      (role) => typeof role === "string" && role.toLowerCase() === "manager",
    )
  );
}

function decodeBase64Url(segment: string): string {
  const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
  const pad = (4 - (normalized.length % 4)) % 4;
  const padded = normalized + "=".repeat(pad);
  const binary = atob(padded);
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}
