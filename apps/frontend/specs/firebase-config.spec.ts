import { emulatorUrl, readFirebaseWebConfig } from "../lib/firebase-config";

describe("readFirebaseWebConfig", () => {
  const keys = [
    "NEXT_PUBLIC_FIREBASE_API_KEY",
    "NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN",
    "NEXT_PUBLIC_FIREBASE_PROJECT_ID",
    "NEXT_PUBLIC_FIREBASE_APP_ID",
  ] as const;
  const previous = new Map<string, string | undefined>();

  beforeEach(() => {
    for (const key of keys) {
      previous.set(key, process.env[key]);
      delete process.env[key];
    }
  });

  afterEach(() => {
    for (const key of keys) {
      const value = previous.get(key);
      if (value === undefined) {
        delete process.env[key];
      } else {
        process.env[key] = value;
      }
    }
  });

  it("returns null until every public field is set", () => {
    expect(readFirebaseWebConfig()).toBeNull();
    process.env.NEXT_PUBLIC_FIREBASE_API_KEY = "demo-api-key";
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN = "localhost";
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID = "pickem-local";
    process.env.NEXT_PUBLIC_FIREBASE_APP_ID = "demo-app-id";
    expect(readFirebaseWebConfig()).toEqual({
      apiKey: "demo-api-key",
      authDomain: "localhost",
      projectId: "pickem-local",
      appId: "demo-app-id",
    });
  });
});

describe("emulatorUrl", () => {
  it("adds http when the host has no scheme", () => {
    expect(emulatorUrl("127.0.0.1:9099")).toBe("http://127.0.0.1:9099");
    expect(emulatorUrl("http://127.0.0.1:9099/")).toBe("http://127.0.0.1:9099");
  });
});
