import type { Metadata } from 'next'

import { GoogleSignIn } from '@/components/google-sign-in'

export const metadata: Metadata = {
  title: "Sign in · Kenney's Pickem",
}

export default function LoginPage() {
  return (
    <main className='mx-auto flex min-h-dvh max-w-md flex-col justify-center px-6 py-16'>
      <section className='surface-card'>
        <h1 className='type-display text-heading'>Kenney&apos;s Pickem</h1>
        <h2 className='type-section'>Sign in</h2>
        <p className='text-base text-muted'>
          Use your Google account to enter the pick&apos;em.
        </p>
        <GoogleSignIn />
      </section>
    </main>
  )
}
