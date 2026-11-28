import type { Metadata } from 'next'

import { MatchupForm } from '@/components/admin/matchup-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Matchup · Kenney's Pickem" }

export default async function MatchupDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Matchup'>
      <MatchupForm mode='edit' id={id} />
    </TitledPage>
  )
}
