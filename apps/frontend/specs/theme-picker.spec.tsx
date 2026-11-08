import { useQueryClient } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useEffect } from 'react'

import { AuthProvider } from '@/components/auth-provider'
import { QueryProvider } from '@/components/query-provider'
import { ThemePicker } from '@/components/theme-picker'
import type { AuthProfile } from '@/lib/auth-profile'
import {
  accountQueryKey,
  keepsCurrentTheme,
  themeChoices,
} from '@/lib/theme-picker'

const themes = [
  { id: 'light', themeName: 'Light', themePath: 'light', active: true },
  { id: 'dark', themeName: 'Dark', themePath: 'dark', active: false },
  {
    id: 'custom',
    themeName: 'Custom',
    themePath: 'custom-school',
    active: true,
  },
  { id: 'alabama', themeName: 'Alabama Crimson Tide', themePath: 'alabama' },
]

const account = {
  emailAddr: 'ada@example.com',
  firstName: 'Ada',
  lastName: 'Player',
  nickName: 'Ada',
  themeId: 'dark',
  version: 3,
}

const profile: AuthProfile = {
  uid: 'ada',
  emailAddr: account.emailAddr,
  firstName: account.firstName,
  lastName: account.lastName,
  nickName: account.nickName,
  themeId: account.themeId,
  roles: ['player'],
}

let refetchAccount: (() => Promise<void>) | null = null

function AccountRefetch() {
  const queryClient = useQueryClient()
  useEffect(() => {
    refetchAccount = () =>
      queryClient.refetchQueries({ queryKey: accountQueryKey('ada') })
    return () => {
      refetchAccount = null
    }
  }, [queryClient])
  return null
}

function renderPicker(withRefetch = false) {
  return render(
    <QueryProvider>
      <AuthProvider
        initialProfile={profile}
        sessionPresent
        sessionIsManager={false}
      >
        {withRefetch ? <AccountRefetch /> : null}
        <ThemePicker />
      </AuthProvider>
    </QueryProvider>,
  )
}

describe('themeChoices', () => {
  it('keeps only active keys that have a CSS block', () => {
    expect(themeChoices(themes)).toEqual([
      { id: 'light', themeName: 'Light', themePath: 'light' },
      {
        id: 'alabama',
        themeName: 'Alabama Crimson Tide',
        themePath: 'alabama',
      },
    ])
    expect(themeChoices({ themePath: 'light' })).toEqual([])
    expect(keepsCurrentTheme('dark', themeChoices(themes))).toBe(true)
    expect(keepsCurrentTheme('custom-school', themeChoices(themes))).toBe(false)
    expect(keepsCurrentTheme('light', themeChoices(themes))).toBe(false)
  })
})

describe('ThemePicker', () => {
  afterEach(() => {
    refetchAccount = null
    vi.unstubAllGlobals()
    document.documentElement.removeAttribute('data-theme')
    document.cookie = 'themeId=; Path=/; Max-Age=0'
  })

  it('offers active styled themes and keeps a deactivated theme until another is picked', async () => {
    const puts: unknown[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/themes') {
          return Response.json(themes)
        }
        if (
          url === '/api/game/account' &&
          (!init?.method || init.method === 'GET')
        ) {
          return Response.json(account)
        }
        if (url === '/api/game/account' && init?.method === 'PUT') {
          const body = JSON.parse(String(init.body)) as {
            nickName: string
            themeId: string
            version: number
          }
          puts.push(body)
          return Response.json({
            ...account,
            themeId: body.themeId,
            version: body.version + 1,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderPicker()

    const light = await screen.findByRole('radio', { name: 'Light' })
    const alabama = screen.getByRole('radio', { name: 'Alabama Crimson Tide' })
    expect(screen.queryByRole('radio', { name: 'Dark' })).toBeNull()
    expect(screen.queryByRole('radio', { name: 'Custom' })).toBeNull()
    expect(
      screen.getByText('Your current theme stays until you pick another.'),
    ).toBeTruthy()
    expect(light.getAttribute('aria-checked')).not.toBe('true')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')

    fireEvent.click(alabama)

    await waitFor(() => {
      expect(puts).toEqual([
        { nickName: 'Ada', themeId: 'alabama', version: 3 },
      ])
    })
    expect(document.documentElement.getAttribute('data-theme')).toBe('alabama')
    expect(document.cookie).toContain('themeId=alabama')
    await waitFor(() => {
      expect(alabama.getAttribute('aria-checked')).toBe('true')
    })
    expect(
      screen.queryByText('Your current theme stays until you pick another.'),
    ).toBeNull()
  })

  it('keeps the saved theme when an older account fetch finishes later', async () => {
    let accountGets = 0
    let releaseStale: (() => void) | null = null
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        if (url === '/api/auth/me') {
          return Response.json(profile)
        }
        if (url === '/api/game/themes') {
          return Response.json(themes)
        }
        if (
          url === '/api/game/account' &&
          (!init?.method || init.method === 'GET')
        ) {
          accountGets += 1
          if (accountGets === 1) {
            return Response.json(account)
          }
          return await new Promise<Response>((resolve, reject) => {
            const abort = () =>
              reject(
                new DOMException('The operation was aborted.', 'AbortError'),
              )
            if (init?.signal?.aborted) {
              abort()
              return
            }
            init?.signal?.addEventListener('abort', abort, { once: true })
            releaseStale = () => {
              if (init?.signal?.aborted) {
                abort()
                return
              }
              resolve(Response.json(account))
            }
          })
        }
        if (url === '/api/game/account' && init?.method === 'PUT') {
          const body = JSON.parse(String(init.body)) as { themeId: string }
          return Response.json({
            ...account,
            themeId: body.themeId,
            version: account.version + 1,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderPicker(true)
    const alabama = await screen.findByRole('radio', {
      name: 'Alabama Crimson Tide',
    })
    expect(refetchAccount).toBeTruthy()
    void refetchAccount?.().catch(() => undefined)
    await waitFor(() => {
      expect(accountGets).toBe(2)
    })

    fireEvent.click(alabama)
    await waitFor(() => {
      expect(alabama.getAttribute('aria-checked')).toBe('true')
    })
    releaseStale?.()
    await waitFor(() => {
      expect(document.documentElement.getAttribute('data-theme')).toBe(
        'alabama',
      )
    })
    expect(alabama.getAttribute('aria-checked')).toBe('true')
    expect(
      screen.queryByText('Your current theme stays until you pick another.'),
    ).toBeNull()
  })
})
