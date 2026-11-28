import type { Metadata } from 'next'

import { VenueForm } from '@/components/admin/venue-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Venue · Kenney's Pickem" }

export default function NewVenuePage() {
  return (
    <TitledPage title='Venue'>
      <VenueForm mode='add' />
    </TitledPage>
  )
}
