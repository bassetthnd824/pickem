import type { NextRequest } from "next/server";
import { proxyApiRequest } from "../../../lib/server/spring-proxy";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type RouteContext = {
  params: Promise<{ path: string[] }>;
};

function forward(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return context.params.then((params) =>
    proxyApiRequest(request, params.path),
  );
}

export function GET(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return forward(request, context);
}

export function POST(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return forward(request, context);
}

export function PUT(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return forward(request, context);
}

export function DELETE(
  request: NextRequest,
  context: RouteContext,
): Promise<Response> {
  return forward(request, context);
}
