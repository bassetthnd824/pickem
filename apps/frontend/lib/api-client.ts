/**
 * Browser client for same-origin `/api`.
 * There is no base-URL option. Spring and Cloud Run origins stay on the server.
 */

const SAME_ORIGIN_BASE = "http://pickem.local";

export class ApiError extends Error {
  readonly status: number;
  readonly body: unknown;

  constructor(status: number, body: unknown) {
    super(`API request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

export type ApiMethod = "GET" | "POST" | "PUT" | "DELETE";

export type ApiRequest = {
  method?: ApiMethod;
  body?: unknown;
  headers?: HeadersInit;
  signal?: AbortSignal;
};

export function resolveApiPath(path: string): string {
  if (path.trim() === "") {
    throw new Error("API path is required");
  }

  const trimmed = path.trim();
  if (
    trimmed.startsWith("//") ||
    trimmed.startsWith("\\\\") ||
    /^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(trimmed)
  ) {
    throw new Error("API client only accepts a same-origin relative path");
  }

  let url: URL;
  try {
    url = new URL(trimmed, SAME_ORIGIN_BASE);
  } catch {
    throw new Error("API client only accepts a same-origin relative path");
  }

  if (url.origin !== SAME_ORIGIN_BASE) {
    throw new Error("API client only accepts a same-origin relative path");
  }

  const pathname = url.pathname.startsWith("/")
    ? url.pathname
    : `/${url.pathname}`;
  const resolved = `${pathname}${url.search}`;
  if (
    resolved === "/api" ||
    resolved.startsWith("/api/") ||
    resolved.startsWith("/api?")
  ) {
    return resolved;
  }
  return `/api${resolved}`;
}

function mergeHeaders(init: HeadersInit | undefined, body: unknown): Headers {
  const headers = new Headers(init);
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  return headers;
}

function parseMaybeJson(text: string): unknown {
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

export async function apiRequest<T>(
  path: string,
  request: ApiRequest = {},
): Promise<T> {
  if (typeof window === "undefined") {
    throw new Error("The API client runs in the browser only");
  }

  const method = request.method ?? "GET";
  const body = method === "GET" ? undefined : request.body;
  const response = await fetch(resolveApiPath(path), {
    method,
    credentials: "include",
    cache: "no-store",
    headers: mergeHeaders(request.headers, body),
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: request.signal,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!response.ok) {
    const errorBody = text.trim() === "" ? undefined : parseMaybeJson(text);
    throw new ApiError(response.status, errorBody);
  }

  if (text.trim() === "") {
    return undefined as T;
  }

  try {
    return JSON.parse(text) as T;
  } catch (error) {
    throw new Error("API response was not JSON", { cause: error });
  }
}

type ApiCall = Omit<ApiRequest, "method" | "body">;

export const api = {
  get<T>(path: string, request?: ApiCall): Promise<T> {
    return apiRequest<T>(path, { ...request, method: "GET" });
  },
  post<T>(path: string, body?: unknown, request?: ApiCall): Promise<T> {
    return apiRequest<T>(path, { ...request, method: "POST", body });
  },
  put<T>(path: string, body?: unknown, request?: ApiCall): Promise<T> {
    return apiRequest<T>(path, { ...request, method: "PUT", body });
  },
  delete<T>(path: string, request?: ApiCall): Promise<T> {
    return apiRequest<T>(path, { ...request, method: "DELETE" });
  },
};
