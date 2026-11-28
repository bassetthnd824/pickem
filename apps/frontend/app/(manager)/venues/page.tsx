import type { Metadata } from 'next'
import { Suspense } from 'react'

import { VenueList } from '@/components/admin/venue-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Venues · Kenney's Pickem" }

export default function VenuesPage() {
  return (
    <TitledPage title='Venues' wide>
      <Suspense fallback={<p>Loading venues.</p>}>
        <VenueList />
      </Suspense>
    </TitledPage>
  )
}
