/**
 * Where Firebase Admin should look. These values are not credentials.
 * A service account key stays in application default credentials.
 */

const DEFAULT_PROJECT_ID = "pickem-local";

/**
 * The dedicated host wins. The public host is only a local fallback.
 * Cloud Run sets `K_SERVICE`; using the public host there would switch
 * Admin to the emulator signature verifier.
 */
export function resolveAuthEmulatorHost(env: NodeJS.ProcessEnv): string | null {
  const dedicated = env.FIREBASE_AUTH_EMULATOR_HOST?.trim() ?? "";
  if (dedicated !== "") {
    return dedicated;
  }
  if ((env.K_SERVICE?.trim() ?? "") !== "") {
    return null;
  }
  const published = env.NEXT_PUBLIC_FIREBASE_AUTH_EMULATOR_HOST?.trim() ?? "";
  return published === "" ? null : published;
}

export function resolveFirebaseProjectId(env: NodeJS.ProcessEnv): string {
  const candidates = [
    env.GOOGLE_CLOUD_PROJECT,
    env.GCLOUD_PROJECT,
    env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  ];
  for (const candidate of candidates) {
    const projectId = candidate?.trim() ?? "";
    if (projectId !== "") {
      return projectId;
    }
  }
  return DEFAULT_PROJECT_ID;
}
