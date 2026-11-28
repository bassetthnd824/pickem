import type { Metadata } from 'next'
import { Suspense } from 'react'

import { SeasonWeekList } from '@/components/admin/season-week-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = {
  title: "Season Weeks · Kenney's Pickem",
}

export default function SeasonWeeksPage() {
  return (
    <TitledPage title='Season Weeks' wide>
      <Suspense fallback={<p>Loading season weeks.</p>}>
        <SeasonWeekList />
      </Suspense>
    </TitledPage>
  )
}
