import { useCallback, useEffect, useRef, useState } from 'react'

import type { Me } from '../api/meApi'
import { contextName } from '../domain/contexts'
import type { EventEnvelope } from '../domain/events'
import { useContexts } from '../hooks/useContexts'
import { useConversation } from '../hooks/useConversation'
import { useConversations } from '../hooks/useConversations'
import { useRoute } from '../hooks/useRoute'
import { useSession } from '../hooks/useSession'
import { AccountPage } from './AccountPage'
import { ChatView } from './ChatView'
import { ContextPanel } from './ContextPanel'
import { ContextsPage } from './ContextsPage'
import { ConversationSidebar } from './ConversationSidebar'
import { HomeView } from './HomeView'
import { LoginPage } from './LoginPage'
import { ThemeToggle } from './ThemeToggle'
import { Toast, type ToastMessage } from './Toast'

export function App() {
  const { session, logout, disconnect } = useSession()
  const { route, search, navigate } = useRoute()

  useEffect(() => {
    if (session.status === 'signed-in' && route.page === 'login') {
      navigate('/')
    }
  }, [session.status, route.page, navigate])

  if (session.status === 'loading') {
    return <div className="splash" aria-busy="true" />
  }

  if (session.status === 'anonymous') {
    return <LoginPage error={search.get('error') === 'google'} />
  }

  return <Workspace me={session.me} onLogout={logout} onDisconnect={disconnect} />
}

/**
 * O app logado: barra lateral de conversas, área central (rota) e painel de contextos.
 */
function Workspace({ me, onLogout, onDisconnect }: { me: Me; onLogout: () => Promise<void>; onDisconnect: () => Promise<void> }) {
  const { route, search, navigate } = useRoute()
  const { contexts, busy, error, toggle, reload: reloadContexts } = useContexts()
  const conversations = useConversations(true)
  const [toast, setToast] = useState<ToastMessage | undefined>()
  const [pending, setPending] = useState<{ id: string; text: string } | undefined>()
  const [panelOpen, setPanelOpen] = useState(() => window.innerWidth >= 900)

  useEffect(() => {
    const enabled = search.get('enabled')
    const denied = search.get('denied')

    if (enabled) {
      setToast({ tone: 'success', text: `${contextName(enabled, contexts)} ligado. O assistente já pode usar.` })
    } else if (denied) {
      const reason = search.get('reason') === 'account_mismatch' ? 'Use a mesma conta Google com que você entrou.' : 'O Google não concedeu todas as permissões.'

      setToast({ tone: 'warning', text: `${contextName(denied, contexts)} continua desligado. ${reason}` })
    }

    if (enabled || denied) {
      window.history.replaceState(null, '', window.location.pathname)
      void reloadContexts()
    }
  }, []) // só na chegada do redirecionamento do Google

  const enableContext = useCallback(
    (key: string) => {
      const context = contexts.find((item) => item.key === key)

      if (context && !context.enabled) {
        void toggle(context)
      }
    },
    [contexts, toggle],
  )

  const start = useCallback(
    async (text: string) => {
      const created = await conversations.create()

      setPending({ id: created.id, text })
      navigate(`/c/${created.id}`)
    },
    [conversations, navigate],
  )

  const newConversation = useCallback(() => navigate('/'), [navigate])

  const remove = useCallback(
    async (id: string) => {
      try {
        await conversations.remove(id)

        if (route.page === 'conversation' && route.id === id) {
          navigate('/')
        }
      } catch (cause) {
        setToast({ tone: 'danger', text: (cause as Error).message })
      }
    },
    [conversations, navigate, route],
  )

  return (
    <div className={`shell ${panelOpen ? '' : 'panel-closed'}`}>
      <ConversationSidebar
        me={me}
        conversations={conversations.conversations}
        activeId={route.page === 'conversation' ? route.id : undefined}
        onNew={newConversation}
        onOpen={(id) => navigate(`/c/${id}`)}
        onDelete={(id) => void remove(id)}
        onSettings={() => navigate('/settings/account')}
        onContexts={() => navigate('/settings/contexts')}
      />
      <main className="main">
        <div className="topbar">
          <ThemeToggle />
          <button type="button" className="icon-button" aria-label={panelOpen ? 'Esconder contextos' : 'Mostrar contextos'} onClick={() => setPanelOpen(!panelOpen)}>
            ☰
          </button>
        </div>
        {route.page === 'conversation' ? (
          <ConversationPage
            key={route.id}
            id={route.id}
            title={conversations.conversations.find((conversation) => conversation.id === route.id)?.title ?? 'Conversa'}
            contexts={contexts}
            pending={pending?.id === route.id ? pending.text : undefined}
            onPendingSent={() => setPending(undefined)}
            onEnableContext={enableContext}
            onEvent={(event) => {
              if (event.type === 'conversation.renamed') {
                conversations.rename(route.id, String(event.payload.title))
              }

              if (event.type === 'context.changed') {
                void reloadContexts()
              }

              if (event.type === 'turn.started' || event.type === 'turn.completed' || event.type === 'turn.failed') {
                void conversations.reload()
              }
            }}
          />
        ) : route.page === 'contexts' ? (
          <ContextsPage contexts={contexts} busy={busy} error={error} onToggle={(context) => void toggle(context)} />
        ) : route.page === 'account' ? (
          <AccountPage me={me} onLogout={() => void onLogout()} onDisconnect={() => void onDisconnect()} />
        ) : (
          <HomeView enabledContexts={contexts.filter((context) => context.enabled).length} onStart={(text) => void start(text)} onContexts={() => navigate('/settings/contexts')} />
        )}
      </main>
      {panelOpen && <ContextPanel contexts={contexts} busy={busy} onToggle={(context) => void toggle(context)} onManage={() => navigate('/settings/contexts')} />}
      <Toast toast={toast} onClose={() => setToast(undefined)} />
    </div>
  )
}

function ConversationPage({
  id,
  title,
  contexts,
  pending,
  onPendingSent,
  onEnableContext,
  onEvent,
}: {
  id: string
  title: string
  contexts: ReturnType<typeof useContexts>['contexts']
  pending?: string
  onPendingSent: () => void
  onEnableContext: (key: string) => void
  onEvent: (event: EventEnvelope) => void
}) {
  const { state, connection, error, send, decide, stop } = useConversation(id, onEvent)
  const sent = useRef(false)

  useEffect(() => {
    if (pending && connection === 'open' && !sent.current) {
      sent.current = true
      onPendingSent()
      void send(pending)
    }
  }, [pending, connection, send, onPendingSent])

  return (
    <ChatView
      title={title}
      state={state}
      connection={connection}
      error={error}
      contexts={contexts}
      onSend={(text) => void send(text)}
      onStop={() => void stop()}
      onDecide={(approvalId, decision) => void decide(approvalId, decision)}
      onEnableContext={onEnableContext}
    />
  )
}
