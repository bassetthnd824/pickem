import "server-only";
import axios, { type AxiosResponse, isAxiosError } from "axios";
import { NextResponse } from "next/server";

const DEFAULT_SPRING_API_BASE_URL = "http://localhost:8080";

const METADATA_IDENTITY_URL =
  "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity";

const API_ERRORS = {
  unconfigured: "API service is not configured",
  unavailable: "API service is unavailable",
} as const;

type ParsedCookie = {
  name: string;
  value: string;
  httpOnly: boolean;
  secure: boolean;
  sameSite: "lax" | "strict" | "none";
  maxAge?: number;
};

export type ProxyErrorMessages = {
  unconfigured: string;
  unavailable: string;
};

export type SpringCall =
  | {
      ok: true;
      status: number;
      body: ArrayBuffer;
      contentType: string | null;
      setCookies: string[];
    }
  | { ok: false; reason: "unconfigured" | "unavailable" };

/**
 * Shared server client. `proxy: false` keeps the metadata call and the
 * Spring call off any ambient HTTP proxy.
 */
export const springHttp = axios.create({
  validateStatus: () => true,
  maxRedirects: 0,
  timeout: 15_000,
  proxy: false,
});

export function springApiBaseUrl(): string {
  const raw = process.env.SPRING_API_BASE_URL;
  const value = raw === undefined ? DEFAULT_SPRING_API_BASE_URL : raw.trim();
  if (value === "") {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  if (
    (url.protocol !== "http:" && url.protocol !== "https:") ||
    url.username !== "" ||
    url.password !== "" ||
    url.origin === "null"
  ) {
    throw new Error("SPRING_API_BASE_URL is not configured");
  }
  return url.origin;
}

export function parseSetCookie(header: string): ParsedCookie | null {
  const parts = header
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part !== "");
  const first = parts[0];
  if (!first) {
    return null;
  }
  const eq = first.indexOf("=");
  if (eq <= 0) {
    return null;
  }
  const name = first.slice(0, eq).trim();
  let value = first.slice(eq + 1).trim();
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    value = value.slice(1, -1);
  }
  if (name === "") {
    return null;
  }
  let httpOnly = false;
  let secure = false;
  let sameSite: ParsedCookie["sameSite"] = "lax";
  let maxAge: number | undefined;
  for (const part of parts.slice(1)) {
    const keyEq = part.indexOf("=");
    const key = (keyEq === -1 ? part : part.slice(0, keyEq))
      .trim()
      .toLowerCase();
    const raw = keyEq === -1 ? "" : part.slice(keyEq + 1).trim();
    if (key === "httponly") {
      httpOnly = true;
    } else if (key === "secure") {
      secure = true;
    } else if (key === "samesite") {
      const normalized = raw.toLowerCase();
      if (
        normalized === "lax" ||
        normalized === "strict" ||
        normalized === "none"
      ) {
        sameSite = normalized;
      }
    } else if (key === "max-age") {
      const parsed = Number(raw);
      if (Number.isFinite(parsed)) {
        maxAge = parsed;
      }
    }
  }
  return { name, value, httpOnly, secure, sameSite, maxAge };
}

/**
 * Drops `Domain` and forces `Path=/` so the browser keeps the cookie
 * host-only on the Next.js origin.
 */
export function hostOnlySetCookie(header: string): string {
  const parts = header
    .split(";")
    .map((part) => part.trim())
    .filter((part) => part !== "");
  const first = parts[0];
  if (!first) {
    return header;
  }
  const kept = [first];
  let sawPath = false;
  for (const part of parts.slice(1)) {
    const keyEq = part.indexOf("=");
    const key = (keyEq === -1 ? part : part.slice(0, keyEq))
      .trim()
      .toLowerCase();
    if (key === "domain") {
      continue;
    }
    if (key === "path") {
      kept.push("Path=/");
      sawPath = true;
      continue;
    }
    kept.push(part);
  }
  if (!sawPath) {
    kept.push("Path=/");
  }
  return kept.join("; ");
}

export function authError(status: number, detail: string): NextResponse {
  return NextResponse.json({ detail }, { status });
}

export function authProxyResponse(
  status: number,
  body: ArrayBuffer,
  contentType: string | null,
  setCookies: readonly string[],
): NextResponse {
  const response = new NextResponse(
    status === 204 ? null : new Uint8Array(body),
    { status },
  );
  if (contentType) {
    response.headers.set("content-type", contentType);
  }
  for (const raw of setCookies) {
    const parsed = parseSetCookie(hostOnlySetCookie(raw));
    if (!parsed) {
      continue;
    }
    response.cookies.set({
      name: parsed.name,
      value: parsed.value,
      path: "/",
      httpOnly: parsed.httpOnly,
      secure: parsed.secure,
      sameSite: parsed.sameSite,
      ...(parsed.maxAge === undefined ? {} : { maxAge: parsed.maxAge }),
    });
  }
  return response;
}

function runsOnCloudRun(): boolean {
  return Boolean(process.env.K_SERVICE?.trim());
}

function safeErrorCode(error: unknown): string {
  if (!isAxiosError(error)) {
    return "network";
  }
  const code = error.code ?? "network";
  return /^[A-Z0-9_]+$/.test(code) ? code : "network";
}

function isBinaryBuffer(data: unknown): data is ArrayBuffer {
  return Object.prototype.toString.call(data) === "[object ArrayBuffer]";
}

function isArrayBufferView(data: unknown): data is ArrayBufferView {
  if (ArrayBuffer.isView(data)) {
    return true;
  }
  if (typeof data !== "object" || data === null || !("buffer" in data)) {
    return false;
  }
  const view = data as ArrayBufferView;
  return (
    isBinaryBuffer(view.buffer) &&
    typeof view.byteOffset === "number" &&
    typeof view.byteLength === "number"
  );
}

function toArrayBuffer(data: unknown): ArrayBuffer {
  if (typeof data === "string") {
    return new TextEncoder().encode(data).buffer as ArrayBuffer;
  }
  let view: Uint8Array | null = null;
  if (isBinaryBuffer(data)) {
    view = new Uint8Array(data);
  } else if (isArrayBufferView(data)) {
    view = new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
  }
  if (!view) {
    return new ArrayBuffer(0);
  }
  const copy = new Uint8Array(view.byteLength);
  copy.set(view);
  return copy.buffer.slice(
    copy.byteOffset,
    copy.byteOffset + copy.byteLength,
  ) as ArrayBuffer;
}

function headerString(
  headers: AxiosResponse["headers"],
  name: string,
): string | null {
  const raw =
    typeof headers.get === "function"
      ? headers.get(name)
      : (headers as Record<string, unknown>)[name];
  if (typeof raw === "string" && raw.trim() !== "") {
    return raw;
  }
  if (Array.isArray(raw)) {
    const first = raw.find(
      (item): item is string => typeof item === "string" && item.trim() !== "",
    );
    return first ?? null;
  }
  return null;
}

function headerList(
  headers: AxiosResponse["headers"],
  name: string,
): string[] {
  if (
    name.toLowerCase() === "set-cookie" &&
    typeof headers.getSetCookie === "function"
  ) {
    const values = headers.getSetCookie();
    if (values.length > 0) {
      return values;
    }
  }
  const raw = (headers as Record<string, unknown>)[name];
  if (typeof raw === "string") {
    return [raw];
  }
  if (Array.isArray(raw)) {
    return raw.filter((item): item is string => typeof item === "string");
  }
  return [];
}

function toCall(response: AxiosResponse<unknown>): SpringCall {
  return {
    ok: true,
    status: response.status,
    body: toArrayBuffer(response.data),
    contentType: headerString(response.headers, "content-type"),
    setCookies: headerList(response.headers, "set-cookie"),
  };
}

/**
 * Cloud Run sets `K_SERVICE`. The token audience is the Spring origin so
 * the backend can require `roles/run.invoker`. Local runs skip this.
 */
export async function cloudRunAuthorization(
  audience: string,
): Promise<string | undefined> {
  if (!runsOnCloudRun()) {
    return undefined;
  }
  const response = await springHttp.request<string>({
    url: METADATA_IDENTITY_URL,
    method: "GET",
    params: { audience },
    headers: { "Metadata-Flavor": "Google" },
    responseType: "text",
    timeout: 2_000,
  });
  if (response.status !== 200 || typeof response.data !== "string") {
    throw new Error("Cloud Run identity token request failed");
  }
  const token = response.data.trim();
  if (token === "" || /\s/.test(token)) {
    throw new Error("Cloud Run identity token request failed");
  }
  return `Bearer ${token}`;
}

function hasControlCharacter(segment: string): boolean {
  for (let index = 0; index < segment.length; index += 1) {
    const code = segment.charCodeAt(index);
    if (code <= 31 || code === 127) {
      return true;
    }
  }
  return false;
}

export function apiProxyPath(
  segments: readonly string[],
  search: string,
): string | null {
  if (segments.length === 0 || (search !== "" && !search.startsWith("?"))) {
    return null;
  }
  const encoded: string[] = [];
  for (const segment of segments) {
    if (
      segment === "" ||
      segment === "." ||
      segment === ".." ||
      segment.includes("/") ||
      segment.includes("\\") ||
      hasControlCharacter(segment)
    ) {
      return null;
    }
    encoded.push(encodeURIComponent(segment));
  }
  return `/api/${encoded.join("/")}${search}`;
}

export async function callSpring(
  request: Request,
  pathAndQuery: string,
): Promise<SpringCall> {
  let base: string;
  try {
    base = springApiBaseUrl();
  } catch (error) {
    console.error(
      error instanceof Error
        ? error.message
        : "SPRING_API_BASE_URL is not configured",
    );
    return { ok: false, reason: "unconfigured" };
  }

  let target: URL;
  try {
    if (!pathAndQuery.startsWith("/") || pathAndQuery.startsWith("//")) {
      throw new Error("invalid path");
    }
    target = new URL(pathAndQuery, base);
    if (target.origin !== base) {
      throw new Error("invalid path");
    }
  } catch {
    console.error("Spring proxy rejected the request path");
    return { ok: false, reason: "unavailable" };
  }

  let authorization: string | undefined;
  try {
    authorization = await cloudRunAuthorization(base);
  } catch {
    console.error("Cloud Run identity token request failed");
    return { ok: false, reason: "unavailable" };
  }

  const headers: Record<string, string> = {};
  const accept = request.headers.get("accept");
  headers.accept =
    accept && accept.trim() !== "" ? accept : "application/json";
  const cookie = request.headers.get("cookie");
  if (cookie) {
    headers.cookie = cookie;
  }
  if (authorization) {
    headers.authorization = authorization;
  }

  const method = request.method.toUpperCase();
  let data: Buffer | undefined;
  if (method !== "GET" && method !== "HEAD") {
    const raw = await request.arrayBuffer();
    if (raw.byteLength > 0) {
      data = Buffer.from(raw);
      const contentType = request.headers.get("content-type");
      if (contentType) {
        headers["content-type"] = contentType;
      }
    }
  }

  try {
    const response = await springHttp.request<ArrayBuffer>({
      url: target.toString(),
      method,
      headers,
      data,
      responseType: "arraybuffer",
    });
    return toCall(response);
  } catch (error) {
    if (isAxiosError(error) && error.response) {
      return toCall(error.response);
    }
    console.error(`Spring proxy request failed (${safeErrorCode(error)})`);
    return { ok: false, reason: "unavailable" };
  }
}

export async function proxyToSpring(
  request: Request,
  pathAndQuery: string,
  messages: ProxyErrorMessages,
): Promise<NextResponse> {
  const result = await callSpring(request, pathAndQuery);
  if (!result.ok) {
    const status = result.reason === "unconfigured" ? 500 : 503;
    const detail =
      result.reason === "unconfigured"
        ? messages.unconfigured
        : messages.unavailable;
    return authError(status, detail);
  }
  return authProxyResponse(
    result.status,
    result.body,
    result.contentType,
    result.setCookies,
  );
}

export function proxyApiRequest(
  request: Request,
  segments: readonly string[],
): Promise<NextResponse> {
  const path = apiProxyPath(segments, new URL(request.url).search);
  if (!path) {
    return Promise.resolve(authError(404, "Not found"));
  }
  return proxyToSpring(request, path, API_ERRORS);
}
