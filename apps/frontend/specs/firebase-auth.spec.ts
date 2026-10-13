import { initializeApp } from "firebase/app";
import { signOut } from "firebase/auth";
import { signOutFromGoogle } from "../lib/firebase-auth";

vi.mock("firebase/app", () => ({
  getApps: () => [],
  initializeApp: vi.fn<(config: unknown) => { name: string }>(() => ({
    name: "pickem",
  })),
}));

vi.mock("firebase/auth", () => ({
  GoogleAuthProvider: class GoogleAuthProvider {},
  connectAuthEmulator: vi.fn<() => void>(),
  getAuth: vi.fn<() => { app: string }>(() => ({ app: "pickem" })),
  getRedirectResult: vi.fn<() => Promise<null>>(async () => null),
  signInWithPopup: vi.fn<() => Promise<void>>(),
  signInWithRedirect: vi.fn<() => Promise<void>>(),
  signOut: vi.fn<() => Promise<void>>(async () => undefined),
}));

const KEYS = [
  "NEXT_PUBLIC_FIREBASE_API_KEY",
  "NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN",
  "NEXT_PUBLIC_FIREBASE_PROJECT_ID",
  "NEXT_PUBLIC_FIREBASE_APP_ID",
  "NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST",
] as const;

describe("signOutFromGoogle", () => {
  const previous = new Map<string, string | undefined>();

  beforeEach(() => {
    vi.mocked(initializeApp).mockClear();
    vi.mocked(signOut).mockClear();
    for (const key of KEYS) {
      previous.set(key, process.env[key]);
      delete process.env[key];
    }
  });

  afterEach(() => {
    for (const key of KEYS) {
      const value = previous.get(key);
      if (value === undefined) {
        delete process.env[key];
      } else {
        process.env[key] = value;
      }
    }
  });

  it("initializes Auth and signs out even when this page has not used Firebase", async () => {
    process.env.NEXT_PUBLIC_FIREBASE_API_KEY = "demo-api-key";
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN = "localhost";
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID = "pickem-local";
    process.env.NEXT_PUBLIC_FIREBASE_APP_ID = "demo-app-id";

    await signOutFromGoogle();

    expect(initializeApp).toHaveBeenCalled();
    expect(signOut).toHaveBeenCalled();
  });

  it("does nothing when Google sign-in is not configured", async () => {
    await signOutFromGoogle();
    expect(initializeApp).not.toHaveBeenCalled();
    expect(signOut).not.toHaveBeenCalled();
  });
});
