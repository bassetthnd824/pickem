import type { Metadata } from 'next'
import { Suspense } from 'react'

import { TeamList } from '@/components/admin/team-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Teams · Kenney's Pickem" }

export default function TeamsPage() {
  return (
    <TitledPage title='Teams' wide>
      <Suspense fallback={<p>Loading teams.</p>}>
        <TeamList />
      </Suspense>
    </TitledPage>
  )
}
