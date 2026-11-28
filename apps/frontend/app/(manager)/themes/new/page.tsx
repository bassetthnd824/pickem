import type { Metadata } from 'next'

import { ThemeForm } from '@/components/admin/theme-form'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Theme · Kenney's Pickem" }

export default function NewThemePage() {
  return (
    <TitledPage title='Theme'>
      <ThemeForm mode='add' />
    </TitledPage>
  )
}
