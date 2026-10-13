import { AUTH_PATHS, forwardAuth } from "../../../../lib/server/spring-auth";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export function GET(request: Request) {
  return forwardAuth(request, AUTH_PATHS.me);
}
