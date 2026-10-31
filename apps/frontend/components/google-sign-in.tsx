"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  consumeGoogleRedirect,
  signInWithGooglePopup,
  signInWithGoogleRedirect,
} from "../lib/firebase-auth";
import {
  FirebaseNotConfiguredError,
  isRedirectFallback,
} from "../lib/google-auth-error";
import { establishSession } from "../lib/session-client";
import { Button } from "./ui/button";

export function GoogleSignIn() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void consumeGoogleRedirect()
      .then(async (idToken) => {
        if (!idToken || cancelled) {
          return;
        }
        await establishSession(idToken);
        if (!cancelled) {
          router.replace("/main");
          router.refresh();
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setError(signInMessage(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function onClick() {
    setError(null);
    setPending(true);
    try {
      const idToken = await signInWithGooglePopup();
      await establishSession(idToken);
      router.replace("/main");
      router.refresh();
    } catch (caught) {
      if (isRedirectFallback(caught)) {
        try {
          await signInWithGoogleRedirect();
          return;
        } catch (redirectError) {
          setError(signInMessage(redirectError));
          return;
        }
      }
      setError(signInMessage(caught));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <Button
        type="button"
        onClick={() => {
          void onClick();
        }}
        disabled={pending}
        focusableWhenDisabled
        aria-busy={pending}
      >
        Sign in with Google
      </Button>
      {error ? (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      ) : null}
    </div>
  );
}

function signInMessage(error: unknown): string {
  if (error instanceof FirebaseNotConfiguredError) {
    return "Google sign-in is not configured.";
  }
  return "Google sign-in failed. Try again.";
}
