import { api } from "./api-client";

export async function establishSession(idToken: string): Promise<void> {
  const token = idToken.trim();
  if (token === "") {
    throw new Error("Google sign-in failed");
  }
  await api.post("/api/auth/session", { idToken: token });
}

export async function endSession(): Promise<void> {
  await api.post("/api/auth/logout");
}
