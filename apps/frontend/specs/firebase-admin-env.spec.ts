import {
  resolveAuthEmulatorHost,
  resolveFirebaseProjectId,
} from "../lib/server/firebase-admin-env";

describe("resolveAuthEmulatorHost", () => {
  it("prefers the server emulator host", () => {
    expect(
      resolveAuthEmulatorHost({
        FIREBASE_AUTH_EMULATOR_HOST: "10.0.0.8:9099",
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099",
      }),
    ).toBe("10.0.0.8:9099");
  });

  it("falls back to the public emulator host for local Next.js", () => {
    expect(
      resolveAuthEmulatorHost({
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: " 127.0.0.1:9099 ",
      }),
    ).toBe("127.0.0.1:9099");
  });

  it("ignores the public emulator host on Cloud Run", () => {
    expect(
      resolveAuthEmulatorHost({
        K_SERVICE: "frontend",
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099",
      }),
    ).toBeNull();
    expect(
      resolveAuthEmulatorHost({
        K_SERVICE: "  ",
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099",
      }),
    ).toBe("127.0.0.1:9099");
  });

  it("keeps an explicit server emulator host on Cloud Run", () => {
    expect(
      resolveAuthEmulatorHost({
        K_SERVICE: "frontend",
        FIREBASE_AUTH_EMULATOR_HOST: "10.0.0.8:9099",
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: "127.0.0.1:9099",
      }),
    ).toBe("10.0.0.8:9099");
  });

  it("returns null when neither host is set", () => {
    expect(resolveAuthEmulatorHost({})).toBeNull();
    expect(
      resolveAuthEmulatorHost({
        FIREBASE_AUTH_EMULATOR_HOST: "  ",
        NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST: "",
      }),
    ).toBeNull();
  });
});

describe("resolveFirebaseProjectId", () => {
  it("uses the first configured project id", () => {
    expect(
      resolveFirebaseProjectId({
        GOOGLE_CLOUD_PROJECT: "pickem-prod",
        GCLOUD_PROJECT: "other",
        NEXT_PUBLIC_FIREBASE_PROJECT_ID: "web",
      }),
    ).toBe("pickem-prod");
    expect(
      resolveFirebaseProjectId({
        NEXT_PUBLIC_FIREBASE_PROJECT_ID: "pickem-local",
      }),
    ).toBe("pickem-local");
  });

  it("defaults to the local project", () => {
    expect(resolveFirebaseProjectId({})).toBe("pickem-local");
  });
});
