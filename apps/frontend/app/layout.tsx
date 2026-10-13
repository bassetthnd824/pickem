import { cookies } from "next/headers";
import type { ReactNode } from "react";
import { AuthProvider } from "../components/auth-provider";
import { QueryProvider } from "../components/query-provider";
import { fetchAuthProfile } from "../lib/server/spring-auth";
import { readSessionClaims, SESSION_COOKIE_NAME } from "../lib/session-gate";
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
  const sessionCookie = cookieStore.get(SESSION_COOKIE_NAME)?.value;
  const sessionIsManager = readSessionClaims(sessionCookie).isManager;
  const profile = await fetchAuthProfile(sessionCookie);
  // A known profile theme wins. Otherwise the theme cookie is used.
  // A first visit, with neither, stays Light.
  const themeId = resolveThemeId(
    profile?.themeId,
    cookieStore.get(THEME_COOKIE_NAME)?.value,
  );

  return (
    <html lang="en" data-theme={themeId}>
      <body className="min-h-dvh bg-bg text-text antialiased">
        <QueryProvider>
          <AuthProvider
            initialProfile={profile}
            sessionPresent={Boolean(sessionCookie)}
            sessionIsManager={sessionIsManager}
          >
            {children}
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
