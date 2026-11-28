import type { Metadata } from 'next'

import { SeasonWeekForm } from '@/components/admin/season-week-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Season Week · Kenney's Pickem" }

export default function NewSeasonWeekPage() {
  return (
    <TitledPage title='Season Week'>
      <SeasonWeekForm mode='add' />
    </TitledPage>
  )
}
