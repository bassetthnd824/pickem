import type { Metadata } from 'next'

import { SeasonForm } from '@/components/admin/season-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Season · Kenney's Pickem" }

export default function NewSeasonPage() {
  return (
    <TitledPage title='Season'>
      <SeasonForm mode='add' />
    </TitledPage>
  )
}
