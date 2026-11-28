import type { Metadata } from 'next'
import { Suspense } from 'react'

import { RivalryList } from '@/components/admin/rivalry-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Rivalries · Kenney's Pickem" }

export default function RivalriesPage() {
  return (
    <TitledPage title='Rivalries' wide>
      <Suspense fallback={<p>Loading rivalries.</p>}>
        <RivalryList />
      </Suspense>
    </TitledPage>
  )
}
