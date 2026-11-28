import type { Metadata } from 'next'

import { TeamForm } from '@/components/admin/team-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Team · Kenney's Pickem" }

export default function NewTeamPage() {
  return (
    <TitledPage title='Team'>
      <TeamForm mode='add' />
    </TitledPage>
  )
}
