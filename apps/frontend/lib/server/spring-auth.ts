import { NextResponse } from "next/server";
import { parseAuthProfile, type AuthProfile } from "../auth-profile";
import { SESSION_COOKIE_NAME } from "../session-gate";
import { callSpring, proxyToSpring } from "./spring-proxy";

export {
  hostOnlySetCookie,
  parseSetCookie,
  springApiBaseUrl,
} from "./spring-proxy";

/**
 * Sign-in, logout, and the current profile. Game and manager calls use the
 * catch-all route, which shares this Axios proxy.
 */

export const AUTH_PATHS = {
  session: "/api/auth/session",
  logout: "/api/auth/logout",
  me: "/api/auth/me",
} as const;

export type AuthPath = (typeof AUTH_PATHS)[keyof typeof AUTH_PATHS];

const SIGN_IN_ERRORS = {
  unconfigured: "Sign-in service is not configured",
  unavailable: "Sign-in service is unavailable",
} as const;

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

export function forwardAuth(
  request: Request,
  path: AuthPath,
): Promise<NextResponse> {
  const search = new URL(request.url).search;
  return proxyToSpring(request, `${path}${search}`, SIGN_IN_ERRORS);
}

export async function fetchAuthProfile(
  sessionCookie: string | undefined,
): Promise<AuthProfile | null> {
  if (!sessionCookie || /[;\s,]/.test(sessionCookie)) {
    return null;
  }
  const request = new Request("http://localhost/api/auth/me", {
    method: "GET",
    headers: {
      accept: "application/json",
      cookie: `${SESSION_COOKIE_NAME}=${sessionCookie}`,
    },
  });
  const result = await callSpring(request, AUTH_PATHS.me);
  if (!result.ok || result.status < 200 || result.status >= 300) {
    return null;
  }
  try {
    return parseAuthProfile(JSON.parse(new TextDecoder().decode(result.body)));
  } catch {
    return null;
  }
}
