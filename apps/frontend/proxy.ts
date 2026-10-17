import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { RESTRICTED_PATH } from "./lib/nav-links";
import { verifySessionClaims } from "./lib/server/firebase-session";
import { SESSION_COOKIE_NAME, gateDecision } from "./lib/session-gate";

export async function proxy(request: NextRequest) {
  const claims = await verifySessionClaims(
    request.cookies.get(SESSION_COOKIE_NAME)?.value,
  );
  const decision = gateDecision(request.nextUrl.pathname, claims);
  if (decision === "login") {
    return NextResponse.redirect(new URL("/login", request.url));
  }
  if (decision === "main") {
    return NextResponse.redirect(new URL("/main", request.url));
  }
  if (decision === "restricted") {
    return NextResponse.rewrite(new URL(RESTRICTED_PATH, request.url));
  }
  return NextResponse.next();
}

// Literal matchers so Next.js can analyze them at build time.
export const config = {
  matcher: [
    "/",
    "/login",
    "/main",
    "/main/:path*",
    "/leaderboard",
    "/leaderboard/:path*",
    "/team-schedule",
    "/team-schedule/:path*",
    "/account",
    "/account/:path*",
    "/restricted",
    "/restricted/:path*",
    "/users",
    "/users/:path*",
    "/themes",
    "/themes/:path*",
    "/teams",
    "/teams/:path*",
    "/venues",
    "/venues/:path*",
    "/rivalries",
    "/rivalries/:path*",
    "/seasons",
    "/seasons/:path*",
    "/season-weeks",
    "/season-weeks/:path*",
    "/matchups",
    "/matchups/:path*",
  ],
};
