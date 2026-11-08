import type { Metadata } from 'next'

import { TeamSchedule } from '@/components/team-schedule'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = {
  title: "Team Schedule · Kenney's Pickem",
}

export default function TeamSchedulePage() {
  return (
    <TitledPage title='Team Schedule'>
      <TeamSchedule />
    </TitledPage>
  )
}
