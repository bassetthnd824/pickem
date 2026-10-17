import type { NextRequest } from "next/server";
import { handleInternalJob } from "../../../../../lib/server/internal-job";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type RouteContext = {
  params: Promise<{ job: string }>;
};

export function POST(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return context.params.then((params) =>
    handleInternalJob(request, params.job),
  );
}
