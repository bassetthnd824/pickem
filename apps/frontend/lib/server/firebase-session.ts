import {
  applicationDefault,
  getApps,
  initializeApp,
  type App,
} from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { claimsFromVerifiedToken, type SessionClaims } from "../session-gate";
import {
  resolveAuthEmulatorHost,
  resolveFirebaseProjectId,
} from "./firebase-admin-env";

const SIGNED_OUT: SessionClaims = { signedIn: false, isManager: false };

/** `verifySessionCookie`'s second argument. Revocation is always checked. */
const CHECK_REVOKED = true;

/**
 * `server-only` is not imported: the Next.js 16 proxy bundler does not
 * apply the `react-server` condition, and that package would throw.
 */
export async function verifySessionClaims(
  sessionCookie: string | undefined,
): Promise<SessionClaims> {
  if (!sessionCookie || sessionCookie.trim() === "") {
    return SIGNED_OUT;
  }
  try {
    const decoded = await getAuth(ensureApp()).verifySessionCookie(
      sessionCookie,
      CHECK_REVOKED,
    );
    return claimsFromVerifiedToken(decoded);
  } catch (error) {
    if (!isRejectedCookie(error)) {
      logUnavailable(error);
    }
    return SIGNED_OUT;
  }
}

function ensureApp(): App {
  const existing = getApps()[0];
  if (existing) {
    return existing;
  }
  const emulatorHost = publishEmulatorHost();
  const projectId = resolveFirebaseProjectId(process.env);
  try {
    if (emulatorHost) {
      return initializeApp({ projectId });
    }
    return initializeApp({
      projectId,
      credential: applicationDefault(),
    });
  } catch (error) {
    const raced = getApps()[0];
    if (raced) {
      return raced;
    }
    throw error;
  }
}

/** The Admin SDK reads this variable when Auth is constructed. */
function publishEmulatorHost(): string | null {
  const host = resolveAuthEmulatorHost(process.env);
  if (!host) {
    return null;
  }
  if ((process.env.FIREBASE_AUTH_EMULATOR_HOST?.trim() ?? "") === "") {
    process.env.FIREBASE_AUTH_EMULATOR_HOST = host;
  }
  return host;
}

/** Cookie outcomes. Other `auth/` and `app/` failures are logged. */
const QUIET_COOKIE_CODES: ReadonlySet<string> = new Set([
  "auth/argument-error",
  "auth/session-cookie-expired",
  "auth/session-cookie-revoked",
  "auth/user-disabled",
  "auth/user-not-found",
]);

function isRejectedCookie(error: unknown): boolean {
  if (!error || typeof error !== "object" || !("code" in error)) {
    return false;
  }
  const code = error.code;
  return typeof code === "string" && QUIET_COOKIE_CODES.has(code);
}

function logUnavailable(error: unknown): void {
  const detail = error instanceof Error ? error.message : "unknown error";
  console.error(`Session cookie verification is unavailable: ${detail}`);
}
