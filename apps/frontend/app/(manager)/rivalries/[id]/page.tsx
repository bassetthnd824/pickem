import type { Metadata } from 'next'

import { RivalryForm } from '@/components/admin/rivalry-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Rivalry · Kenney's Pickem" }

export default async function RivalryDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Rivalry'>
      <RivalryForm mode='edit' id={id} />
    </TitledPage>
  )
}
