import type { Metadata } from 'next'
import { Suspense } from 'react'

import { MatchupList } from '@/components/admin/matchup-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Matchups · Kenney's Pickem" }

export default function MatchupsPage() {
  return (
    <TitledPage title='Matchups' wide>
      <Suspense fallback={<p>Loading matchups.</p>}>
        <MatchupList />
      </Suspense>
    </TitledPage>
  )
}
