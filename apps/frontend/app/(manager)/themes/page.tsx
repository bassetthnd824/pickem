import type { Metadata } from 'next'
import { Suspense } from 'react'

import { ThemeList } from '@/components/admin/theme-list'
import { TitledPage } from '@/components/titled-page'

export const metadata: Metadata = { title: "Themes · Kenney's Pickem" }

export default function ThemesPage() {
  return (
    <TitledPage title='Themes' wide>
      <Suspense fallback={<p>Loading themes.</p>}>
        <ThemeList />
      </Suspense>
    </TitledPage>
  )
}
