import { renderToStaticMarkup } from "react-dom/server";
import { cookies } from "next/headers";
import RootLayout from "../app/layout";
import { fetchAuthProfile } from "../lib/server/spring-auth";
import type { AuthProfile } from "../lib/auth-profile";

vi.mock("../app/global.css", () => ({}));

vi.mock("next/headers", () => ({
  cookies: vi.fn<() => Promise<{ get: (name: string) => unknown }>>(),
}));

vi.mock("../lib/server/spring-auth", () => ({
  fetchAuthProfile: vi.fn<() => Promise<AuthProfile | null>>(async () => null),
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

  it("prefers the signed-in profile theme during SSR", async () => {
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
