import type { Metadata } from 'next'

import { MatchupForm } from '@/components/admin/matchup-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Matchup · Kenney's Pickem" }

export default function NewMatchupPage() {
  return (
    <TitledPage title='Matchup'>
      <MatchupForm mode='add' />
    </TitledPage>
  )
}
