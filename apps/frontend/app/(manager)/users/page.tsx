import type { Metadata } from 'next'
import { Suspense } from 'react'

import { UserList } from '@/components/admin/user-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Users · Kenney's Pickem" }

export default function UsersPage() {
  return (
    <TitledPage title='Users' wide>
      <Suspense fallback={<p>Loading users.</p>}>
        <UserList />
      </Suspense>
    </TitledPage>
  )
}
