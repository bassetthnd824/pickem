import type { ReactNode } from 'react'

export function TitledPage({
  title,
  wide = false,
  children,
}: {
  title: string
  wide?: boolean
  children?: ReactNode
}) {
  const narrow = wide ? '' : 'page-wrap-narrow'

  return (
    <main>
      <header className='hero-band bg-primary text-primary-fg'>
        <div className={`page-frame ${narrow}`}>
          <h1 className='type-display text-primary-fg'>{title}</h1>
        </div>
      </header>
      <div className={`page-wrap ${narrow}`}>{children}</div>
    </main>
  )
}
