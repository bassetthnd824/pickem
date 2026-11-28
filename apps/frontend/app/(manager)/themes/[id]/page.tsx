import type { Metadata } from 'next'

import { ThemeForm } from '@/components/admin/theme-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Theme · Kenney's Pickem" }

export default async function ThemeDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Theme'>
      <ThemeForm mode='edit' id={id} />
    </TitledPage>
  )
}
