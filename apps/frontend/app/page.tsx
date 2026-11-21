import { NavigationMenu } from '@base-ui/react/navigation-menu'
import Link from 'next/link'

import { PickResultIcon } from '@/components/pick-result-icon'
import { buttonVariantClass } from '@/components/ui/button-styles'

export default function HomePage() {
  return (
    <main>
      <header className='hero-band bg-primary text-primary-fg'>
        <div className='page-frame page-wrap-narrow flex flex-col gap-4'>
          <h1 className='type-display text-primary-fg'>Kenney&apos;s Pickem</h1>
          <p className='text-lg text-primary-fg'>
            Season-long college football confidence pick&apos;em.
          </p>
          <NavigationMenu.Root>
            <NavigationMenu.List>
              <NavigationMenu.Item>
                <NavigationMenu.Link
                  className={`${buttonVariantClass.primary} w-fit cursor-pointer`}
                  render={<Link href='/login'>Sign in</Link>}
                />
              </NavigationMenu.Item>
            </NavigationMenu.List>
          </NavigationMenu.Root>
        </div>
      </header>
      <section aria-label='Pick results' className='page-wrap page-wrap-narrow'>
        <div className='surface-card'>
          <div className='flex flex-wrap items-center gap-6'>
            <PickResultIcon result='correct' />
            <PickResultIcon result='incorrect' />
          </div>
        </div>
      </section>
    </main>
  )
}
