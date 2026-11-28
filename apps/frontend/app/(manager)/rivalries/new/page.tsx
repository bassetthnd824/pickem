import type { Metadata } from 'next'

import { RivalryForm } from '@/components/admin/rivalry-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Rivalry · Kenney's Pickem" }

export default function NewRivalryPage() {
  return (
    <TitledPage title='Rivalry'>
      <RivalryForm mode='add' />
    </TitledPage>
  )
}
