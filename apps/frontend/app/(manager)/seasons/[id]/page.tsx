import type { Metadata } from 'next'

import { SeasonForm } from '@/components/admin/season-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Season · Kenney's Pickem" }

export default async function SeasonDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Season'>
      <SeasonForm mode='edit' id={id} />
    </TitledPage>
  )
}
