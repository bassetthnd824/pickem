import { getApps, initializeApp } from "firebase/app";
import {
  GoogleAuthProvider,
  connectAuthEmulator,
  getAuth,
  getRedirectResult,
  signInWithPopup,
  signInWithRedirect,
  signOut,
} from "firebase/auth";
import {
  emulatorUrl,
  readAuthEmulatorHost,
  readFirebaseWebConfig,
} from "./firebase-config";
import { FirebaseNotConfiguredError } from "./google-auth-error";

let emulatorConnected = false;
let redirectRead: Promise<string | null> | null = null;

function clientAuth() {
  const config = readFirebaseWebConfig();
  if (!config) {
    throw new FirebaseNotConfiguredError();
  }
  const app = getApps()[0] ?? initializeApp(config);
  const auth = getAuth(app);
  const emulatorHost = readAuthEmulatorHost();
  if (emulatorHost && !emulatorConnected) {
    connectAuthEmulator(auth, emulatorUrl(emulatorHost), {
      disableWarnings: true,
    });
    emulatorConnected = true;
  }
  return auth;
}

export async function signInWithGooglePopup(): Promise<string> {
  const result = await signInWithPopup(clientAuth(), new GoogleAuthProvider());
  return result.user.getIdToken();
}

export async function signInWithGoogleRedirect(): Promise<void> {
  redirectRead = null;
  await signInWithRedirect(clientAuth(), new GoogleAuthProvider());
}

/**
 * Popup-blocked browsers fall back to a redirect. `getRedirectResult` can
 * be read once, and React strict mode runs the effect twice, so the
 * in-flight read is shared. A full page load clears this cache.
 */
export function consumeGoogleRedirect(): Promise<string | null> {
  if (!redirectRead) {
    redirectRead = readRedirectResult().finally(() => {
      redirectRead = null;
    });
  }
  return redirectRead;
}

async function readRedirectResult(): Promise<string | null> {
  if (!readFirebaseWebConfig()) {
    return null;
  }
  const result = await getRedirectResult(clientAuth());
  if (!result) {
    return null;
  }
  return result.user.getIdToken();
}

export async function signOutFromGoogle(): Promise<void> {
  if (!readFirebaseWebConfig()) {
    return;
  }
  try {
    // A reloaded page has not initialized Auth yet. Sign-out still has to
    // clear the persisted Google user, or the next visit restores it.
    await signOut(clientAuth());
  } catch {
    // The session cookie is the app login. A client sign-out failure
    // still leaves the HttpOnly cookie cleared by POST /api/auth/logout.
  }
}
