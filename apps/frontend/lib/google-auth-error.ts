export class FirebaseNotConfiguredError extends Error {
  constructor() {
    super("Google sign-in is not configured");
    this.name = "FirebaseNotConfiguredError";
  }
}

const REDIRECT_FALLBACK_CODES = new Set([
  "auth/popup-blocked",
  "auth/operation-not-supported-in-this-environment",
]);

export function isRedirectFallback(error: unknown): boolean {
  if (!error || typeof error !== "object" || !("code" in error)) {
    return false;
  }
  return REDIRECT_FALLBACK_CODES.has(String(error.code));
}
