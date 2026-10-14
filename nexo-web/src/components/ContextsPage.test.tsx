import { act, fireEvent, render, screen } from '@testing-library/react'

import type { Context } from '../domain/contexts'
import { useContexts } from '../hooks/useContexts'
import { ContextsPage } from './ContextsPage'

const contexts: Context[] = [
  { key: 'calendar', name: 'Calendar', description: 'Agenda', enabled: true, scopesGranted: true, scopes: ['https://www.googleapis.com/auth/calendar'], toolCount: 7 },
  { key: 'gmail', name: 'Gmail', description: 'E-mails', enabled: false, scopesGranted: false, scopes: ['https://www.googleapis.com/auth/gmail.modify'], toolCount: 7 },
]

function Page({ go }: { go: (url: string) => void }) {
  const { contexts: loaded, busy, error, toggle } = useContexts(go)

  return <ContextsPage contexts={loaded} busy={busy} error={error} onToggle={(context) => void toggle(context)} />
}

describe('ContextsPage', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('ligar um contexto sem escopo leva ao consentimento do Google', async () => {
    const go = vi.fn()
    const fetch = vi.fn(async (url: string, init?: RequestInit) => {
      if (url === '/api/contexts') {
        return new Response(JSON.stringify(contexts), { status: 200 })
      }

      if (url === '/api/contexts/gmail/enable' && init?.method === 'POST') {
        return new Response(JSON.stringify({ status: 'consent_required', context: 'gmail', authorizationUrl: '/oauth2/authorization/google?context=gmail' }), { status: 200 })
      }

      return new Response('{}', { status: 200 })
    })

    vi.stubGlobal('fetch', fetch)
    document.cookie = 'XSRF-TOKEN=abc123'

    render(<Page go={go} />)

    const gmail = await screen.findByRole('switch', { name: 'Ligar Gmail' })

    expect(screen.getByRole('switch', { name: 'Desligar Calendar' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByText('Ao ligar, o Google vai pedir sua permissão.')).toBeInTheDocument()

    await act(async () => {
      fireEvent.click(gmail)
    })

    expect(go).toHaveBeenCalledWith('/oauth2/authorization/google?context=gmail')

    const enableCall = fetch.mock.calls.find(([url]) => url === '/api/contexts/gmail/enable')!

    expect((enableCall[1]!.headers as Record<string, string>)['X-XSRF-TOKEN']).toBe('abc123')
  })
})
