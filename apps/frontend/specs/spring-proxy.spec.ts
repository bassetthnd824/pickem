import { readFileSync, readdirSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { AxiosError, type AxiosRequestConfig } from "axios";
import { NextRequest } from "next/server";
import { DELETE, GET, POST, PUT } from "../app/api/[...path]/route";
import {
  apiProxyPath,
  proxyApiRequest,
  springHttp,
} from "../lib/server/spring-proxy";

const ORIGINAL_BASE = process.env.SPRING_API_BASE_URL;
const ORIGINAL_SERVICE = process.env.K_SERVICE;
const requestMock = vi.spyOn(springHttp, "request");

function restoreEnv(): void {
  if (ORIGINAL_BASE === undefined) {
    delete process.env.SPRING_API_BASE_URL;
  } else {
    process.env.SPRING_API_BASE_URL = ORIGINAL_BASE;
  }
  if (ORIGINAL_SERVICE === undefined) {
    delete process.env.K_SERVICE;
  } else {
    process.env.K_SERVICE = ORIGINAL_SERVICE;
  }
}

afterEach(() => {
  restoreEnv();
  requestMock.mockReset();
});

afterAll(() => {
  requestMock.mockRestore();
});

function configOf(call: number): AxiosRequestConfig {
  const config = requestMock.mock.calls[call]?.[0];
  if (!config || typeof config === "string") {
    throw new Error("expected an axios request config");
  }
  return config;
}

function jsonResponse(status: number, body: unknown) {
  return {
    status,
    data: new TextEncoder().encode(JSON.stringify(body)).buffer,
    headers: { "content-type": "application/json" },
    statusText: "OK",
    config: {},
  };
}

describe("apiProxyPath", () => {
  it("builds an /api path and keeps the query string", () => {
    expect(apiProxyPath(["game", "leaderboard"], "?seasonId=2026")).toBe(
      "/api/game/leaderboard?seasonId=2026",
    );
  });

  it("rejects empty, parent, and broken segments", () => {
    expect(apiProxyPath([], "")).toBeNull();
    expect(apiProxyPath([".."], "")).toBeNull();
    expect(apiProxyPath(["game", ""], "")).toBeNull();
    expect(apiProxyPath(["a/b"], "")).toBeNull();
    expect(apiProxyPath(["game"], "seasonId=2026")).toBeNull();
  });
});

describe("proxyApiRequest", () => {
  it("round-trips GET, POST, PUT, and DELETE with the session cookie", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    const cases = [
      {
        method: "GET",
        path: "/api/game/leaderboard?seasonId=2026",
        segments: ["game", "leaderboard"],
        body: undefined,
        status: 200,
        payload: { rank: 1 },
      },
      {
        method: "POST",
        path: "/api/game/picks",
        segments: ["game", "picks"],
        body: { picks: [{ rank: 1 }] },
        status: 400,
        payload: { detail: "invalid" },
      },
      {
        method: "PUT",
        path: "/api/manager/teams/team-1",
        segments: ["manager", "teams", "team-1"],
        body: { name: "Georgia" },
        status: 409,
        payload: { detail: "conflict" },
      },
      {
        method: "DELETE",
        path: "/api/manager/venues/venue-1",
        segments: ["manager", "venues", "venue-1"],
        body: undefined,
        status: 204,
        payload: undefined,
      },
    ] as const;

    for (const item of cases) {
      requestMock.mockReset();
      requestMock.mockResolvedValue(
        item.payload === undefined
          ? {
              status: item.status,
              data: new ArrayBuffer(0),
              headers: {},
              statusText: "OK",
              config: {},
            }
          : jsonResponse(item.status, item.payload),
      );
      const response = await proxyApiRequest(
        new Request(`http://pickem.local${item.path}`, {
          method: item.method,
          headers: {
            cookie: "__session=session-value",
            ...(item.body
              ? { "content-type": "application/json" }
              : {}),
          },
          body: item.body ? JSON.stringify(item.body) : undefined,
        }),
        item.segments,
      );
      const config = configOf(0);
      const forwardedBody =
        item.body === undefined
          ? undefined
          : Buffer.from(config.data as Buffer).toString("utf8");
      const text = await response.text();
      const parsed = item.payload === undefined ? text : JSON.parse(text);
      expect(config.url).toBe(`http://spring.internal:8080${item.path}`);
      expect(config.method).toBe(item.method);
      expect(config.headers).toMatchObject({
        cookie: "__session=session-value",
      });
      expect(config.headers).not.toHaveProperty("authorization");
      expect(forwardedBody).toEqual(
        item.body === undefined ? undefined : JSON.stringify(item.body),
      );
      expect(response.status).toBe(item.status);
      expect(parsed).toEqual(item.payload === undefined ? "" : item.payload);
      expect(text).not.toContain("spring.internal");
    }
  });

  it("copies a host-only session cookie from Spring", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    requestMock.mockResolvedValue({
      status: 204,
      data: new ArrayBuffer(0),
      headers: {
        "set-cookie": [
          "__session=minted; Path=/api; Domain=spring.internal; HttpOnly; Secure; SameSite=Lax",
        ],
      },
      statusText: "No Content",
      config: {},
    });

    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/auth/session", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ idToken: "id-token" }),
      }),
      ["auth", "session"],
    );
    const setCookie = response.headers.getSetCookie().join(";");
    expect(setCookie).toContain("__session=minted");
    expect(setCookie.toLowerCase()).toContain("path=/");
    expect(setCookie.toLowerCase()).not.toContain("domain=");
    expect(setCookie).not.toContain("spring.internal");
    expect(setCookie).not.toContain("Path=/api");
  });

  it("logs a missing base URL and hides it from the client", async () => {
    process.env.SPRING_API_BASE_URL = "http://user:secret@spring.internal";
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/game/main"),
      ["game", "main"],
    );
    const text = await response.text();
    expect(response.status).toBe(500);
    expect(text).toContain("API service is not configured");
    expect(text).not.toContain("spring.internal");
    expect(text).not.toContain("secret");
    expect(text).not.toContain("localhost");
    expect(requestMock).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalledWith("SPRING_API_BASE_URL is not configured");
    errorSpy.mockRestore();
  });

  it("hides the Spring host when the upstream call fails", async () => {
    process.env.SPRING_API_BASE_URL = "https://pickem-api.run.app";
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    requestMock.mockRejectedValue(
      new AxiosError("connect ECONNREFUSED pickem-api.run.app", "ECONNREFUSED"),
    );

    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/manager/seasons"),
      ["manager", "seasons"],
    );
    const text = await response.text();
    expect(response.status).toBe(503);
    expect(text).toContain("API service is unavailable");
    expect(text).not.toContain("run.app");
    expect(errorSpy.mock.calls.flat().join(" ")).not.toContain("run.app");
    errorSpy.mockRestore();
  });

  it("sends a Cloud Run identity token only when K_SERVICE is set", async () => {
    process.env.SPRING_API_BASE_URL = "https://pickem-api.run.app";
    process.env.K_SERVICE = "frontend";
    requestMock.mockImplementation(async (config) => {
      const url = typeof config === "string" ? config : String(config?.url);
      if (url.includes("metadata.google.internal")) {
        return {
          status: 200,
          data: "header.payload.sig",
          headers: {},
          statusText: "OK",
          config: {},
        };
      }
      return jsonResponse(200, { ok: true });
    });

    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/game/main", {
        headers: { cookie: "__session=session-value" },
      }),
      ["game", "main"],
    );
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ ok: true });
    const metadata = configOf(0);
    expect(String(metadata.url)).toContain("metadata.google.internal");
    expect(metadata.params).toEqual({ audience: "https://pickem-api.run.app" });
    expect(metadata.headers).toMatchObject({ "Metadata-Flavor": "Google" });
    const upstream = configOf(1);
    expect(upstream.url).toBe("https://pickem-api.run.app/api/game/main");
    expect(upstream.headers).toMatchObject({
      authorization: "Bearer header.payload.sig",
      cookie: "__session=session-value",
    });
  });

  it("fails closed when the identity token is missing and hides the audience", async () => {
    process.env.SPRING_API_BASE_URL = "https://pickem-api.run.app";
    process.env.K_SERVICE = "frontend";
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    requestMock.mockResolvedValue({
      status: 500,
      data: "metadata failed for https://pickem-api.run.app",
      headers: {},
      statusText: "Error",
      config: {},
    });

    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/game/main"),
      ["game", "main"],
    );
    const text = await response.text();
    expect(response.status).toBe(503);
    expect(text).not.toContain("run.app");
    expect(text).not.toContain("metadata");
    expect(requestMock).toHaveBeenCalledTimes(1);
    expect(errorSpy.mock.calls.flat().join(" ")).not.toContain("run.app");
    errorSpy.mockRestore();
  });

  it("does not call Spring for a path that could escape the API prefix", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    const response = await proxyApiRequest(
      new Request("http://pickem.local/api/../admin"),
      ["..", "admin"],
    );
    expect(response.status).toBe(404);
    expect(requestMock).not.toHaveBeenCalled();
  });
});

describe("catch-all route", () => {
  it("forwards each method through the route handler", async () => {
    process.env.SPRING_API_BASE_URL = "http://127.0.0.1:8080";
    const handlers = { GET, POST, PUT, DELETE };
    for (const method of ["GET", "POST", "PUT", "DELETE"] as const) {
      requestMock.mockReset();
      requestMock.mockResolvedValue(jsonResponse(201, { method }));
      const request = new NextRequest(
        "http://pickem.local/api/manager/seasons?name=2026",
        {
          method,
          headers: { cookie: "__session=session-value" },
          body: method === "GET" ? undefined : JSON.stringify({ name: "2026" }),
        },
      );
      const response = await handlers[method](request, {
        params: Promise.resolve({ path: ["manager", "seasons"] }),
      });
      expect(response.status).toBe(201);
      expect(configOf(0).url).toBe(
        "http://127.0.0.1:8080/api/manager/seasons?name=2026",
      );
      expect(configOf(0).method).toBe(method);
    }
  });
});

describe("axios stays on the server", () => {
  it("imports axios only from the server proxy", () => {
    const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
    const files: string[] = [];
    const walk = (dir: string) => {
      for (const name of readdirSync(dir)) {
        if (name === "node_modules" || name === ".next") {
          continue;
        }
        const path = join(dir, name);
        if (statSync(path).isDirectory()) {
          walk(path);
        } else if (path.endsWith(".ts") || path.endsWith(".tsx")) {
          files.push(path);
        }
      }
    };
    walk(root);

    const axiosImporters: string[] = [];
    for (const path of files) {
      const rel = relative(root, path).replaceAll("\\", "/");
      if (rel.startsWith("specs/")) {
        continue;
      }
      const source = readFileSync(path, "utf8");
      const importsAxios =
        /from ["']axios["']/.test(source) ||
        /import ["']axios["']/.test(source);
      if (!importsAxios) {
        continue;
      }
      axiosImporters.push(rel);
      if (source.startsWith('"use client"') || rel === "lib/api-client.ts") {
        throw new Error(`${rel} imports axios`);
      }
    }
    expect(axiosImporters).toEqual(["lib/server/spring-proxy.ts"]);
    const proxy = readFileSync(join(root, "lib/server/spring-proxy.ts"), "utf8");
    expect(proxy).toContain('import "server-only"');
  });
});
