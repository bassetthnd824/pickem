import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { verifySessionClaims } from "../lib/server/firebase-session";

const admin = vi.hoisted(() => ({
  apps: [] as unknown[],
  initializeApp: vi.fn<(options: unknown) => { name: string }>(() => ({
    name: "app",
  })),
  applicationDefault: vi.fn<() => { kind: string }>(() => ({ kind: "adc" })),
  verifySessionCookie: vi.fn<(...args: unknown[]) => Promise<unknown>>(),
}));

vi.mock("firebase-admin/app", () => ({
  getApps: () => admin.apps,
  initializeApp: (options: unknown) => {
    const app = admin.initializeApp(options);
    admin.apps.push(app);
    return app;
  },
  applicationDefault: () => admin.applicationDefault(),
}));

vi.mock("firebase-admin/auth", () => ({
  getAuth: () => ({
    verifySessionCookie: (...args: unknown[]) =>
      admin.verifySessionCookie(...args),
  }),
}));

const ENV_KEYS = [
  "FIREBASE_AUTH_EMULATOR_HOST",
  "NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST",
  "GOOGLE_CLOUD_PROJECT",
  "GCLOUD_PROJECT",
  "NEXT_PUBLIC_FIREBASE_PROJECT_ID",
  "K_SERVICE",
] as const;

describe("verifySessionClaims", () => {
  const saved: Partial<Record<(typeof ENV_KEYS)[number], string | undefined>> =
    {};

  beforeEach(() => {
    admin.apps.length = 0;
    admin.initializeApp.mockClear();
    admin.applicationDefault.mockClear();
    admin.verifySessionCookie.mockReset();
    for (const key of ENV_KEYS) {
      saved[key] = process.env[key];
      delete process.env[key];
    }
  });

  afterEach(() => {
    for (const key of ENV_KEYS) {
      const value = saved[key];
      if (value === undefined) {
        delete process.env[key];
      } else {
        process.env[key] = value;
      }
    }
  });

  it("does not call Admin for a missing cookie", async () => {
    await expect(verifySessionClaims(undefined)).resolves.toEqual({
      signedIn: false,
      isManager: false,
    });
    await expect(verifySessionClaims("  ")).resolves.toEqual({
      signedIn: false,
      isManager: false,
    });
    expect(admin.verifySessionCookie).not.toHaveBeenCalled();
    expect(admin.initializeApp).not.toHaveBeenCalled();
  });

  it("checks revocation and reads the manager claim", async () => {
    admin.verifySessionCookie.mockResolvedValue({
      uid: "manager-1",
      manager: true,
      roles: ["manager"],
    });

    await expect(verifySessionClaims("manager-cookie")).resolves.toEqual({
      signedIn: true,
      isManager: true,
    });
    expect(admin.verifySessionCookie).toHaveBeenCalledWith(
      "manager-cookie",
      true,
    );
  });

  it("treats a verified roles entry as the manager claim", async () => {
    admin.verifySessionCookie.mockResolvedValue({
      uid: "manager-1",
      roles: ["player", "Manager"],
    });

    await expect(verifySessionClaims("cookie")).resolves.toEqual({
      signedIn: true,
      isManager: true,
    });
  });

  it("signs in a verified player", async () => {
    admin.verifySessionCookie.mockResolvedValue({
      uid: "player-1",
      roles: ["player"],
    });

    await expect(verifySessionClaims("player-cookie")).resolves.toEqual({
      signedIn: true,
      isManager: false,
    });
  });

  it("rejects cookie failures without logging", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    const codes = [
      "auth/argument-error",
      "auth/session-cookie-expired",
      "auth/session-cookie-revoked",
      "auth/user-disabled",
      "auth/user-not-found",
    ];
    for (const code of codes) {
      admin.verifySessionCookie.mockRejectedValueOnce(
        Object.assign(new Error(code), { code }),
      );
      await expect(verifySessionClaims(code)).resolves.toEqual({
        signedIn: false,
        isManager: false,
      });
    }
    expect(errorSpy).not.toHaveBeenCalled();
    errorSpy.mockRestore();
  });

  it("logs every non-cookie verification failure", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    admin.verifySessionCookie
      .mockRejectedValueOnce(
        Object.assign(new Error("auth backend unreachable"), {
          code: "auth/internal-error",
        }),
      )
      .mockRejectedValueOnce(
        Object.assign(new Error("revocation lookup failed"), {
          code: "auth/invalid-credential",
        }),
      );

    const signedOut = { signedIn: false, isManager: false };
    await expect(verifySessionClaims("one")).resolves.toEqual(signedOut);
    await expect(verifySessionClaims("two")).resolves.toEqual(signedOut);
    expect(errorSpy).toHaveBeenCalledTimes(2);
    errorSpy.mockRestore();
  });

  it("uses the auth emulator without application default credentials", async () => {
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099";
    process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID = "pickem-local";
    admin.verifySessionCookie.mockResolvedValue({
      uid: "player-1",
      roles: ["player"],
    });

    await verifySessionClaims("emulator-cookie");

    expect(process.env.FIREBASE_AUTH_EMULATOR_HOST).toBe("127.0.0.1:9099");
    expect(admin.applicationDefault).not.toHaveBeenCalled();
    expect(admin.initializeApp).toHaveBeenCalledWith({
      projectId: "pickem-local",
    });
    expect(admin.verifySessionCookie).toHaveBeenCalledWith(
      "emulator-cookie",
      true,
    );
  });

  it("ignores the public emulator host on Cloud Run", async () => {
    process.env.K_SERVICE = "frontend";
    process.env.NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099";
    process.env.GOOGLE_CLOUD_PROJECT = "pickem-prod";
    admin.verifySessionCookie.mockResolvedValue({
      uid: "player-1",
      roles: ["player"],
    });

    await verifySessionClaims("prod-cookie");

    expect(process.env.FIREBASE_AUTH_EMULATOR_HOST).toBeUndefined();
    expect(admin.applicationDefault).toHaveBeenCalledOnce();
    expect(admin.initializeApp).toHaveBeenCalledWith({
      projectId: "pickem-prod",
      credential: { kind: "adc" },
    });
  });

  it("uses application default credentials when no emulator is configured", async () => {
    process.env.GOOGLE_CLOUD_PROJECT = "pickem-prod";
    admin.verifySessionCookie.mockResolvedValue({
      uid: "player-1",
      roles: ["player"],
    });

    await verifySessionClaims("prod-cookie");

    expect(admin.applicationDefault).toHaveBeenCalledOnce();
    expect(admin.initializeApp).toHaveBeenCalledWith({
      projectId: "pickem-prod",
      credential: { kind: "adc" },
    });
  });

  it("fails closed when Admin credentials are missing", async () => {
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
    admin.applicationDefault.mockImplementationOnce(() => {
      throw new Error("Could not load the default credentials");
    });

    await expect(verifySessionClaims("cookie")).resolves.toEqual({
      signedIn: false,
      isManager: false,
    });
    expect(errorSpy).toHaveBeenCalledWith(
      expect.stringContaining("Session cookie verification is unavailable"),
    );
    errorSpy.mockRestore();
  });
});

describe("firebase admin stays on the server", () => {
  it("imports firebase-admin only from the session verifier", () => {
    const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
    const files: string[] = [];
    const walk = (dir: string) => {
      for (const name of readdirSync(dir)) {
        if (name === "node_modules" || name === ".next") {
          continue;
        }
        const path = join(dir, name);
        if (statSync(path).isDirectory()) {
          walk(path);
        } else if (path.endsWith(".ts") || path.endsWith(".tsx")) {
          files.push(path);
        }
      }
    };
    walk(root);

    const adminImporters: string[] = [];
    const sessionImporters: string[] = [];
    for (const path of files) {
      const rel = relative(root, path).replaceAll("\\", "/");
      if (rel.startsWith("specs/")) {
        continue;
      }
      const source = readFileSync(path, "utf8");
      if (
        source.startsWith('"use client"') &&
        source.includes("firebase-admin")
      ) {
        throw new Error(`${rel} references firebase-admin`);
      }
      if (/from ["']firebase-admin\//.test(source)) {
        adminImporters.push(rel);
      }
      if (
        source.includes('from "../lib/server/firebase-session"') ||
        source.includes('from "./lib/server/firebase-session"')
      ) {
        sessionImporters.push(rel);
      }
    }

    expect(adminImporters).toEqual(["lib/server/firebase-session.ts"]);
    expect(sessionImporters.sort()).toEqual(["app/layout.tsx", "proxy.ts"]);
  });
});
