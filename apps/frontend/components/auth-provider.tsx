"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  type ReactNode,
} from "react";
import { api } from "../lib/api-client";
import { parseAuthProfile, type AuthProfile } from "../lib/auth-profile";
import { isThemeKey } from "../lib/theme";

export const AUTH_ME_KEY = ["auth", "me"] as const;

type AuthState = {
  profile: AuthProfile | null;
  isManager: boolean;
  sessionPresent: boolean;
};

const AuthContext = createContext<AuthState>({
  profile: null,
  isManager: false,
  sessionPresent: false,
});

export function useAuth(): AuthState {
  return useContext(AuthContext);
}

async function loadProfile(): Promise<AuthProfile> {
  const body: unknown = await api.get("/api/auth/me");
  const profile = parseAuthProfile(body);
  if (!profile) {
    throw new Error("Profile response was invalid");
  }
  return profile;
}

export function AuthProvider({
  initialProfile,
  sessionPresent,
  sessionIsManager,
  children,
}: {
  initialProfile: AuthProfile | null;
  sessionPresent: boolean;
  sessionIsManager: boolean;
  children: ReactNode;
}) {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: AUTH_ME_KEY,
    queryFn: loadProfile,
    initialData: initialProfile ?? undefined,
    enabled: sessionPresent,
  });

  useEffect(() => {
    if (!sessionPresent) {
      queryClient.removeQueries({ queryKey: AUTH_ME_KEY });
      return;
    }
    if (initialProfile) {
      queryClient.setQueryData(AUTH_ME_KEY, initialProfile);
    }
  }, [initialProfile, queryClient, sessionPresent]);

  const profile =
    !sessionPresent || query.isError ? null : (query.data ?? initialProfile);

  useEffect(() => {
    if (!profile || !isThemeKey(profile.themeId)) {
      return;
    }
    document.documentElement.setAttribute("data-theme", profile.themeId);
  }, [profile]);

  const value = useMemo<AuthState>(
    () => ({
      profile,
      // Match the HTML gate. Profile roles can change before the cookie does,
      // and those links would only render the restricted page.
      isManager: sessionPresent && sessionIsManager,
      sessionPresent,
    }),
    [profile, sessionIsManager, sessionPresent],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
