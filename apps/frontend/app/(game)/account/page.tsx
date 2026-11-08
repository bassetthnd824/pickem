import type { Metadata } from 'next'

import { ThemePicker } from '@/components/theme-picker'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "My Account · Kenney's Pickem" }

export default function AccountPage() {
  return (
    <TitledPage title='My Account'>
      <ThemePicker />
    </TitledPage>
  )
}
