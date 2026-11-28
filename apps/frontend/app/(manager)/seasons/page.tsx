import type { Metadata } from 'next'
import { Suspense } from 'react'

import { SeasonList } from '@/components/admin/season-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Seasons · Kenney's Pickem" }

export default function SeasonsPage() {
  return (
    <TitledPage title='Seasons' wide>
      <Suspense fallback={<p>Loading seasons.</p>}>
        <SeasonList />
      </Suspense>
    </TitledPage>
  )
}
