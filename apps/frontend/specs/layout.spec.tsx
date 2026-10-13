import { renderToStaticMarkup } from "react-dom/server";
import { cookies } from "next/headers";
import RootLayout from "../app/layout";

vi.mock("../app/global.css", () => ({}));

vi.mock("next/headers", () => ({
  cookies: vi.fn<() => Promise<{ get: (name: string) => unknown }>>(),
}));

function cookieJar(value: string | undefined) {
  return {
    get: (name: string) =>
      name === "themeId" && value !== undefined ? { name, value } : undefined,
  } as never;
}

describe("RootLayout", () => {
  it("sets data-theme from the theme cookie in the server markup", async () => {
    vi.mocked(cookies).mockResolvedValue(cookieJar("alabama"));

    const html = renderToStaticMarkup(
      await RootLayout({ children: <p>Home</p> }),
    );

    expect(html).toContain('<html lang="en" data-theme="alabama">');
    expect(html).toContain("bg-bg");
    expect(html).toContain("text-text");
  });

  it("uses Light when the cookie is missing or unknown", async () => {
    vi.mocked(cookies).mockResolvedValue(cookieJar(undefined));
    expect(
      renderToStaticMarkup(await RootLayout({ children: <p>Home</p> })),
    ).toContain('data-theme="light"');

    vi.mocked(cookies).mockResolvedValue(cookieJar("not-a-theme"));
    expect(
      renderToStaticMarkup(await RootLayout({ children: <p>Home</p> })),
    ).toContain('data-theme="light"');
  });
});
