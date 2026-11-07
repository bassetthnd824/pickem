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
  return (
    <main
      className={`mx-auto flex flex-col gap-4 px-6 py-10 ${wide ? 'max-w-6xl' : 'max-w-3xl'}`}
    >
      <h1 className='text-2xl font-semibold text-text'>{title}</h1>
      {children}
    </main>
  )
}
