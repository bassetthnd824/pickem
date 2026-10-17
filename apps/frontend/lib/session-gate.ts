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
 * Claims taken from a session cookie Firebase Admin has already verified.
 * A missing uid is signed out. Expiry and revocation are Admin's job.
 */
export type VerifiedSessionToken = {
  uid?: unknown;
  manager?: unknown;
  roles?: unknown;
};

export function claimsFromVerifiedToken(
  token: VerifiedSessionToken | null | undefined,
): SessionClaims {
  if (!token || typeof token.uid !== "string" || token.uid.trim() === "") {
    return SIGNED_OUT;
  }
  return { signedIn: true, isManager: tokenIsManager(token) };
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

function tokenIsManager(token: VerifiedSessionToken): boolean {
  if (token.manager === true) {
    return true;
  }
  return (
    Array.isArray(token.roles) &&
    token.roles.some(
      (role) => typeof role === "string" && role.toLowerCase() === "manager",
    )
  );
}
