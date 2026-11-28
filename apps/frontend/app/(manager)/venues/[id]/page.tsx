import type { Metadata } from 'next'

import { VenueForm } from '@/components/admin/venue-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Venue · Kenney's Pickem" }

export default async function VenueDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Venue'>
      <VenueForm mode='edit' id={id} />
    </TitledPage>
  )
}
