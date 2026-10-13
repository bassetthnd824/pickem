import {
  AUTH_PATHS,
  finishLogout,
  forwardAuth,
} from "../../../../lib/server/spring-auth";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function POST(request: Request) {
  return finishLogout(await forwardAuth(request, AUTH_PATHS.logout));
}
