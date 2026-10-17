import { renderToStaticMarkup } from "react-dom/server";
import { cookies } from "next/headers";
import RootLayout from "../app/layout";
import type { AuthProfile } from "../lib/auth-profile";
import { verifySessionClaims } from "../lib/server/firebase-session";
import { fetchAuthProfile } from "../lib/server/spring-auth";
import type { SessionClaims } from "../lib/session-gate";

vi.mock("../app/global.css", () => ({}));

vi.mock("next/headers", () => ({
  cookies: vi.fn<() => Promise<{ get: (name: string) => unknown }>>(),
}));

vi.mock("../lib/server/spring-auth", () => ({
  fetchAuthProfile: vi.fn<() => Promise<AuthProfile | null>>(async () => null),
}));

vi.mock("../lib/server/firebase-session", () => ({
  verifySessionClaims: vi.fn<() => Promise<SessionClaims>>(async () => ({
    signedIn: false,
    isManager: false,
  })),
}));

function cookieJar(values: Record<string, string | undefined>) {
  return {
    get: (name: string) => {
      const value = values[name];
      return value === undefined ? undefined : { name, value };
    },
  } as never;
}

describe("RootLayout", () => {
  beforeEach(() => {
    vi.mocked(fetchAuthProfile).mockReset();
    vi.mocked(fetchAuthProfile).mockResolvedValue(null);
    vi.mocked(verifySessionClaims).mockReset();
    vi.mocked(verifySessionClaims).mockResolvedValue({
      signedIn: false,
      isManager: false,
    });
  });

  it("sets data-theme from the theme cookie in the server markup", async () => {
    vi.mocked(cookies).mockResolvedValue(cookieJar({ themeId: "alabama" }));

    const html = renderToStaticMarkup(
      await RootLayout({ children: <p>Home</p> }),
    );

    expect(html).toContain('<html lang="en" data-theme="alabama">');
    expect(html).toContain("bg-bg");
    expect(html).toContain("text-text");
  });

  it("uses Light when the cookie is missing or unknown", async () => {
    vi.mocked(cookies).mockResolvedValue(cookieJar({}));
    expect(
      renderToStaticMarkup(await RootLayout({ children: <p>Home</p> })),
    ).toContain('data-theme="light"');

    vi.mocked(cookies).mockResolvedValue(cookieJar({ themeId: "not-a-theme" }));
    expect(
      renderToStaticMarkup(await RootLayout({ children: <p>Home</p> })),
    ).toContain('data-theme="light"');
  });

  it("does not load a profile when Admin rejects the cookie", async () => {
    vi.mocked(cookies).mockResolvedValue(
      cookieJar({ __session: "forged", themeId: "alabama" }),
    );

    const html = renderToStaticMarkup(
      await RootLayout({ children: <p>Home</p> }),
    );

    expect(verifySessionClaims).toHaveBeenCalledWith("forged");
    expect(fetchAuthProfile).not.toHaveBeenCalled();
    expect(html).toContain('data-theme="alabama"');
  });

  it("prefers the signed-in profile theme during SSR", async () => {
    vi.mocked(verifySessionClaims).mockResolvedValue({
      signedIn: true,
      isManager: false,
    });
    vi.mocked(cookies).mockResolvedValue(
      cookieJar({ __session: "session-value", themeId: "light" }),
    );
    vi.mocked(fetchAuthProfile).mockResolvedValue({
      uid: "uid-1",
      emailAddr: "a@example.com",
      firstName: "Ada",
      lastName: "Lovelace",
      nickName: "Ada",
      themeId: "georgia",
      roles: ["player"],
    });

    const html = renderToStaticMarkup(
      await RootLayout({ children: <p>Home</p> }),
    );

    expect(html).toContain('data-theme="georgia"');
    expect(fetchAuthProfile).toHaveBeenCalledWith("session-value");
  });
});
