import type { Metadata } from 'next'

import { TeamForm } from '@/components/admin/team-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Team · Kenney's Pickem" }

export default async function TeamDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='Team'>
      <TeamForm mode='edit' id={id} />
    </TitledPage>
  )
}
