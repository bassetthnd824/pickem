import { fireEvent, render, screen, waitFor } from '@testing-library/react'

import { AccountForm } from '@/components/account-form'
import { AuthProvider } from '@/components/auth-provider'
import { QueryProvider } from '@/components/query-provider'
import type { AuthProfile } from '@/lib/auth-profile'
import { nickNameError, parseAccount } from '@/lib/theme-picker'

const themes = [
  { id: 'light', themeName: 'Light', themePath: 'light', active: true },
  { id: 'dark', themeName: 'Dark', themePath: 'dark', active: false },
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

function renderForm() {
  return render(
    <QueryProvider>
      <AuthProvider
        initialProfile={profile}
        sessionPresent
        sessionIsManager={false}
      >
        <AccountForm />
      </AuthProvider>
    </QueryProvider>,
  )
}

describe('parseAccount', () => {
  it('keeps Google email and name with the editable screen name', () => {
    expect(parseAccount(account)).toEqual(account)
    expect(parseAccount({ ...account, emailAddr: 1 })).toBeNull()
    expect(nickNameError('   ')).toBe('nickName is required')
    expect(nickNameError('n'.repeat(41))).toBe(
      'nickName must be 40 characters or fewer',
    )
    expect(nickNameError(' Tide ')).toBeNull()
  })
})

describe('AccountForm', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    document.documentElement.removeAttribute('data-theme')
    document.cookie = 'themeId=; Path=/; Max-Age=0'
  })

  it('shows read-only Google fields, edits the screen name, and has no password fields', async () => {
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
            nickName: body.nickName,
            themeId: body.themeId,
            version: body.version + 1,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderForm()

    const email = await screen.findByLabelText('Email Address')
    const firstName = screen.getByLabelText('First Name')
    const lastName = screen.getByLabelText('Last Name')
    const screenName = screen.getByLabelText('Screen Name')
    expect(email).toHaveProperty('readOnly', true)
    expect(firstName).toHaveProperty('readOnly', true)
    expect(lastName).toHaveProperty('readOnly', true)
    expect(email).toHaveProperty('value', 'ada@example.com')
    expect(firstName).toHaveProperty('value', 'Ada')
    expect(lastName).toHaveProperty('value', 'Player')
    expect(screenName).toHaveProperty('readOnly', false)
    expect(screenName).toHaveProperty('value', 'Ada')
    expect(screen.queryByLabelText(/password/i)).toBeNull()
    expect(document.querySelector('input[type="password"]')).toBeNull()

    fireEvent.change(screenName, { target: { value: 'Tide' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Ada')
    expect(puts).toEqual([])

    fireEvent.change(screen.getByLabelText('Screen Name'), {
      target: { value: '  Tide  ' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(puts).toEqual([{ nickName: 'Tide', themeId: 'dark', version: 3 }])
    })
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Tide')
    expect(screen.getByLabelText('Email Address')).toHaveProperty(
      'value',
      'ada@example.com',
    )
  })

  it('applies a theme immediately without navigation and keeps an unsaved screen name', async () => {
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
            nickName: body.nickName,
            themeId: body.themeId,
            version: body.version + 1,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderForm()
    const href = window.location.href
    const screenName = await screen.findByLabelText('Screen Name')
    fireEvent.change(screenName, { target: { value: 'Tide' } })
    fireEvent.click(
      await screen.findByRole('radio', { name: 'Alabama Crimson Tide' }),
    )

    await waitFor(() => {
      expect(puts).toEqual([
        { nickName: 'Ada', themeId: 'alabama', version: 3 },
      ])
    })
    expect(document.documentElement.getAttribute('data-theme')).toBe('alabama')
    expect(window.location.href).toBe(href)
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Tide')

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Ada')
    expect(document.documentElement.getAttribute('data-theme')).toBe('alabama')

    fireEvent.change(screen.getByLabelText('Screen Name'), {
      target: { value: 'Tide' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(puts).toEqual([
        { nickName: 'Ada', themeId: 'alabama', version: 3 },
        { nickName: 'Tide', themeId: 'alabama', version: 4 },
      ])
    })
  })

  it('rejects a blank screen name without writing', async () => {
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
          puts.push(JSON.parse(String(init.body)))
          return Response.json(account)
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderForm()
    const screenName = await screen.findByLabelText('Screen Name')
    fireEvent.change(screenName, { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByRole('alert')).toHaveProperty(
      'textContent',
      'nickName is required',
    )
    expect(puts).toEqual([])
  })

  it('does not save a theme while the screen name save is in flight', async () => {
    const started: string[] = []
    let releaseNick: (() => void) | null = null
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
          const body = JSON.parse(String(init.body)) as { themeId: string }
          started.push(body.themeId)
          if (body.themeId === 'dark') {
            return await new Promise<Response>((resolve) => {
              releaseNick = () => {
                resolve(
                  Response.json({
                    ...account,
                    nickName: 'Tide',
                    version: account.version + 1,
                  }),
                )
              }
            })
          }
          return Response.json({
            ...account,
            themeId: body.themeId,
            version: account.version + 1,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderForm()
    const screenName = await screen.findByLabelText('Screen Name')
    fireEvent.change(screenName, { target: { value: 'Tide' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(started).toEqual(['dark'])
    })

    fireEvent.click(screen.getByRole('radio', { name: 'Alabama Crimson Tide' }))
    expect(started).toEqual(['dark'])

    releaseNick?.()
    await waitFor(() => {
      expect(screen.getByLabelText('Screen Name')).toHaveProperty(
        'value',
        'Tide',
      )
    })
    fireEvent.click(screen.getByRole('radio', { name: 'Alabama Crimson Tide' }))
    await waitFor(() => {
      expect(started).toEqual(['dark', 'alabama'])
    })
  })

  it('reloads the account after a stale save and retries with the new version', async () => {
    const puts: number[] = []
    let gets = 0
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
          gets += 1
          return Response.json({
            ...account,
            version: gets === 1 ? 3 : 4,
          })
        }
        if (url === '/api/game/account' && init?.method === 'PUT') {
          const body = JSON.parse(String(init.body)) as {
            nickName: string
            version: number
          }
          puts.push(body.version)
          if (body.version !== 4) {
            return Response.json(
              { detail: 'Stale version for users/ada: expected 4 but was 3' },
              { status: 409 },
            )
          }
          return Response.json({
            ...account,
            nickName: body.nickName,
            version: 5,
          })
        }
        return new Response('missing', { status: 404 })
      }),
    )

    renderForm()
    const screenName = await screen.findByLabelText('Screen Name')
    fireEvent.change(screenName, { target: { value: 'Tide' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(
      await screen.findByText('The account changed. Save again.'),
    ).toBeTruthy()
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Tide')
    expect(puts).toEqual([3])

    fireEvent.click(screen.getByRole('button', { name: 'Save' }))
    await waitFor(() => {
      expect(puts).toEqual([3, 4])
    })
    expect(screen.getByLabelText('Screen Name')).toHaveProperty('value', 'Tide')
  })
})
