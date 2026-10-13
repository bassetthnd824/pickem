import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { QueryProvider } from "../components/query-provider";
import { resolveThemeId, THEME_COOKIE_NAME } from "../lib/theme";
import "./global.css";

export const metadata = {
  title: "Kenney's Pickem",
  description: "Season-long college football confidence pick'em",
};

export default async function RootLayout({
  children,
}: {
  children: ReactNode;
}) {
  const cookieStore = await cookies();
  // Profile themeId is passed as the first argument once the session user is
  // known during SSR. Until then the theme cookie is the request-time source.
  const themeId = resolveThemeId(
    undefined,
    cookieStore.get(THEME_COOKIE_NAME)?.value,
  );

  return (
    <html lang="en" data-theme={themeId}>
      <body className="min-h-dvh bg-bg text-text antialiased">
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
