import type { Metadata } from "next";
import { GoogleSignIn } from "../../../components/google-sign-in";

export const metadata: Metadata = {
  title: "Sign in · Kenney's Pickem",
};

export default function LoginPage() {
  return (
    <main className="mx-auto flex min-h-dvh max-w-md flex-col justify-center gap-6 px-6 py-16">
      <header className="rounded-md bg-primary px-4 py-3">
        <h1 className="text-3xl font-semibold tracking-tight text-primary-fg">
          Kenney&apos;s Pickem
        </h1>
      </header>
      <section className="flex flex-col gap-4 rounded-md border border-border bg-surface px-4 py-6">
        <h2 className="text-xl font-semibold text-text">Sign in</h2>
        <p className="text-sm text-muted">
          Use your Google account to enter the pick&apos;em.
        </p>
        <GoogleSignIn />
      </section>
    </main>
  );
}
