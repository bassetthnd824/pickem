import { AxiosError } from "axios";
import { NextResponse } from "next/server";
import {
  AUTH_PATHS,
  fetchAuthProfile,
  finishLogout,
  forwardAuth,
  hostOnlySetCookie,
  parseSetCookie,
  springApiBaseUrl,
} from "../lib/server/spring-auth";
import { springHttp } from "../lib/server/spring-proxy";

const ORIGINAL_BASE = process.env.SPRING_API_BASE_URL;
const requestMock = vi.spyOn(springHttp, "request");

afterEach(() => {
  if (ORIGINAL_BASE === undefined) {
    delete process.env.SPRING_API_BASE_URL;
  } else {
    process.env.SPRING_API_BASE_URL = ORIGINAL_BASE;
  }
  requestMock.mockReset();
  vi.unstubAllGlobals();
});

afterAll(() => {
  requestMock.mockRestore();
});

describe("springApiBaseUrl", () => {
  it("defaults to local Spring and rejects a missing or unusable value", () => {
    delete process.env.SPRING_API_BASE_URL;
    expect(springApiBaseUrl()).toBe("http://localhost:8080");

    process.env.SPRING_API_BASE_URL = "   ";
    expect(() => springApiBaseUrl()).toThrow(/not configured/);

    process.env.SPRING_API_BASE_URL = "http://user:secret@spring.internal";
    let message = "";
    try {
      springApiBaseUrl();
    } catch (error) {
      message = error instanceof Error ? error.message : String(error);
    }
    expect(message).toBe("SPRING_API_BASE_URL is not configured");
    expect(message).not.toContain("spring.internal");
    expect(message).not.toContain("secret");
  });
});

describe("hostOnlySetCookie", () => {
  it("drops Domain and forces Path=/", () => {
    const header = hostOnlySetCookie(
      "__session=abc; Path=/api; Domain=spring.internal; HttpOnly; Secure; SameSite=Lax",
    );
    expect(header.toLowerCase()).not.toContain("domain=");
    expect(header).not.toContain("spring.internal");
    expect(header).toContain("Path=/");
    expect(header).not.toContain("Path=/api");
    expect(parseSetCookie(header)).toMatchObject({
      name: "__session",
      value: "abc",
      httpOnly: true,
      secure: true,
      sameSite: "lax",
    });
  });

  it("keeps a zero max age so logout clears the cookie", () => {
    expect(
      parseSetCookie("__session=; Path=/; Max-Age=0; HttpOnly; Secure"),
    ).toMatchObject({ value: "", maxAge: 0, httpOnly: true });
  });
});

describe("forwardAuth", () => {
  it("forwards the session post and copies a host-only cookie", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    requestMock.mockResolvedValue({
      status: 204,
      data: new ArrayBuffer(0),
      headers: {
        "set-cookie": [
          "__session=session-value; Path=/api; Domain=spring.internal; Max-Age=1209600; HttpOnly; Secure; SameSite=Lax",
        ],
      },
      statusText: "No Content",
      config: {},
    });

    const response = await forwardAuth(
      new Request("http://pickem.local/api/auth/session", {
        method: "POST",
        headers: {
          "content-type": "application/json",
          cookie: "__session=old",
        },
        body: JSON.stringify({ idToken: "id-token" }),
      }),
      AUTH_PATHS.session,
    );

    expect(requestMock).toHaveBeenCalledTimes(1);
    const config = requestMock.mock.calls[0]?.[0];
    expect(config?.url).toBe("http://spring.internal:8080/api/auth/session");
    expect(config?.method).toBe("POST");
    expect(Buffer.from(config?.data as Buffer).toString("utf8")).toBe(
      JSON.stringify({ idToken: "id-token" }),
    );
    expect(config?.headers).toMatchObject({ cookie: "__session=old" });
    expect(config?.headers).not.toHaveProperty("authorization");

    expect(response.status).toBe(204);
    const setCookie = response.headers.getSetCookie().join(";");
    expect(setCookie).toContain("__session=session-value");
    expect(setCookie.toLowerCase()).toContain("path=/");
    expect(setCookie.toLowerCase()).not.toContain("domain=");
    expect(setCookie).not.toContain("spring.internal");
    expect(await response.text()).not.toContain("spring.internal");
  });

  it("does not reveal the Spring URL when the upstream call fails", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    requestMock.mockRejectedValue(
      new AxiosError(
        "connect ECONNREFUSED spring.internal:8080",
        "ECONNREFUSED",
      ),
    );

    const response = await forwardAuth(
      new Request("http://pickem.local/api/auth/me"),
      AUTH_PATHS.me,
    );
    const text = await response.text();
    expect(response.status).toBe(503);
    expect(text).toContain("Sign-in service is unavailable");
    expect(text).not.toContain("spring.internal");
    expect(text).not.toContain("localhost:8080");
    expect(errorSpy.mock.calls.flat().join(" ")).not.toContain(
      "spring.internal",
    );
    errorSpy.mockRestore();
  });
});

describe("finishLogout", () => {
  it("clears the session cookie when Spring rejects it", () => {
    const response = finishLogout(
      NextResponse.json(
        { detail: "Authentication is required" },
        { status: 401 },
      ),
    );
    expect(response.status).toBe(204);
    const setCookie = response.headers.getSetCookie().join(";");
    expect(setCookie).toContain("__session=");
    expect(setCookie.toLowerCase()).toContain("max-age=0");
    expect(setCookie.toLowerCase()).toContain("path=/");
    expect(setCookie.toLowerCase()).toContain("httponly");
    expect(setCookie.toLowerCase()).toContain("secure");
    expect(setCookie.toLowerCase()).not.toContain("domain=");
    expect(setCookie).not.toContain("Authentication is required");
  });

  it("keeps a successful logout response", () => {
    const upstream = new NextResponse(null, { status: 204 });
    expect(finishLogout(upstream)).toBe(upstream);
  });
});

describe("fetchAuthProfile", () => {
  it("returns the profile for a session cookie", async () => {
    process.env.SPRING_API_BASE_URL = "http://spring.internal:8080";
    const payload = {
      uid: "uid-1",
      emailAddr: "a@example.com",
      firstName: "Ada",
      lastName: "Lovelace",
      nickName: "Ada",
      themeId: "georgia",
      roles: ["player", "manager"],
    };
    requestMock.mockResolvedValue({
      status: 200,
      data: new TextEncoder().encode(JSON.stringify(payload)).buffer,
      headers: { "content-type": "application/json" },
      statusText: "OK",
      config: {},
    });

    await expect(fetchAuthProfile("session-value")).resolves.toMatchObject({
      uid: "uid-1",
      themeId: "georgia",
      roles: ["player", "manager"],
    });
    const config = requestMock.mock.calls[0]?.[0];
    expect(config?.url).toBe("http://spring.internal:8080/api/auth/me");
    expect(config?.headers).toMatchObject({
      cookie: "__session=session-value",
    });
  });

  it("returns null when the cookie is unsafe or the profile is missing", async () => {
    await expect(fetchAuthProfile("bad;cookie")).resolves.toBeNull();
    expect(requestMock).not.toHaveBeenCalled();

    requestMock.mockResolvedValue({
      status: 401,
      data: new ArrayBuffer(0),
      headers: {},
      statusText: "Unauthorized",
      config: {},
    });
    await expect(fetchAuthProfile("session-value")).resolves.toBeNull();
  });
});
