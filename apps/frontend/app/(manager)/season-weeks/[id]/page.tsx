import type { Metadata } from 'next'

import { SeasonWeekForm } from '@/components/admin/season-week-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Season Week · Kenney's Pickem" }

export default async function SeasonWeekDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Season Week'>
      <SeasonWeekForm mode='edit' id={id} />
    </TitledPage>
  )
}
