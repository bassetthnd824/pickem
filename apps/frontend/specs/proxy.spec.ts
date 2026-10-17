import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { NextRequest } from "next/server";
import { config, proxy } from "../proxy";
import { verifySessionClaims } from "../lib/server/firebase-session";
import {
  GAME_ROUTE_PREFIXES,
  MANAGER_ROUTE_PREFIXES,
  type SessionClaims,
} from "../lib/session-gate";

vi.mock("../lib/server/firebase-session", () => ({
  verifySessionClaims: vi.fn<
    (sessionCookie: string | undefined) => Promise<SessionClaims>
  >(),
}));

const verify = vi.mocked(verifySessionClaims);

function request(path: string, session?: string): NextRequest {
  const headers = new Headers();
  if (session !== undefined) {
    headers.set("cookie", `__session=${session}`);
  }
  return new NextRequest(`http://localhost${path}`, { headers });
}

describe("proxy", () => {
  beforeEach(() => {
    verify.mockReset();
  });

  it("sends a missing or rejected cookie to login", async () => {
    verify.mockResolvedValue({ signedIn: false, isManager: false });

    const missing = await proxy(request("/main"));
    expect(missing.status).toBe(307);
    expect(missing.headers.get("location")).toBe("http://localhost/login");
    expect(verify).toHaveBeenCalledWith(undefined);

    const forged = await proxy(
      request(
        "/users",
        "eyJhbGciOiJub25lIn0.eyJzdWIiOiJtIiwiZXhwIjo5OTk5OTk5OTk5LCJtYW5hZ2VyIjp0cnVlfQ.sig",
      ),
    );
    expect(forged.status).toBe(307);
    expect(forged.headers.get("location")).toBe("http://localhost/login");
    expect(forged.headers.get("x-middleware-rewrite")).toBeNull();
  });

  it("rewrites a verified player away from a manager route", async () => {
    verify.mockResolvedValue({ signedIn: true, isManager: false });

    const response = await proxy(request("/users", "player-cookie"));

    expect(verify).toHaveBeenCalledWith("player-cookie");
    expect(response.headers.get("location")).toBeNull();
    expect(response.headers.get("x-middleware-rewrite")).toBe(
      "http://localhost/restricted",
    );
  });

  it("allows a verified manager on a manager route", async () => {
    verify.mockResolvedValue({ signedIn: true, isManager: true });

    const response = await proxy(request("/users", "manager-cookie"));

    expect(response.headers.get("x-middleware-next")).toBe("1");
    expect(response.headers.get("location")).toBeNull();
    expect(response.headers.get("x-middleware-rewrite")).toBeNull();
  });

  it("sends a verified session from login to main", async () => {
    verify.mockResolvedValue({ signedIn: true, isManager: false });

    const response = await proxy(request("/login", "player-cookie"));

    expect(response.status).toBe(307);
    expect(response.headers.get("location")).toBe("http://localhost/main");
  });
});

describe("proxy matcher", () => {
  it("includes every game and manager route", () => {
    expect(Array.isArray(config.matcher)).toBe(true);
    if (!Array.isArray(config.matcher)) {
      return;
    }
    for (const prefix of [
      "/",
      "/login",
      ...GAME_ROUTE_PREFIXES,
      ...MANAGER_ROUTE_PREFIXES,
    ]) {
      expect(config.matcher).toContain(prefix);
    }
  });

  it("verifies the cookie in the request gate", () => {
    const source = readFileSync(
      join(dirname(fileURLToPath(import.meta.url)), "../proxy.ts"),
      "utf8",
    );
    expect(source).toContain("verifySessionClaims");
    expect(source).not.toContain("readSessionClaims");
    expect(source).not.toContain("atob");
  });
});
