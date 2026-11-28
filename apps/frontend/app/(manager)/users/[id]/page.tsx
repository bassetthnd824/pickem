import type { Metadata } from 'next'

import { UserForm } from '@/components/admin/user-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "User · Kenney's Pickem" }

export default async function UserDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = await params
  return (
    <TitledPage title='User'>
      <UserForm id={id} />
    </TitledPage>
  )
}
