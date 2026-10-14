import { useEffect, useRef } from 'react'

import type { ConnectionState } from '../api/conversationSocket'
import { LOGIN_URL } from '../api/meApi'
import type { Context } from '../domain/contexts'
import type { ConversationState, TimelineItem } from '../domain/conversationReducer'
import { FAILURE_MESSAGES } from '../domain/tools'
import { ApprovalCard } from './ApprovalCard'
import { Composer } from './Composer'
import { ContextSuggestion } from './ContextSuggestion'
import { MessageBubble } from './MessageBubble'
import { ToolCallCard } from './ToolCallCard'
import { ToolSearchCard } from './ToolSearchCard'

interface ChatViewProps {
  title: string
  state: ConversationState
  connection: ConnectionState
  error?: string
  contexts: Context[]
  onSend: (text: string) => void
  onStop: () => void
  onDecide: (approvalId: string, decision: 'approve' | 'reject') => void
  onEnableContext: (key: string) => void
}

export function ChatView({ title, state, connection, error, contexts, onSend, onStop, onDecide, onEnableContext }: ChatViewProps) {
  const end = useRef<HTMLDivElement>(null)
  const reconnect = state.items.some((item) => item.kind === 'turnEnd' && item.code === 'GOOGLE_RECONNECT_REQUIRED')

  useEffect(() => {
    end.current?.scrollIntoView?.({ block: 'end' })
  }, [state.items.length, state.items.at(-1)])

  return (
    <section className="chat">
      <header className="chat-header">
        <h1>{state.title ?? title}</h1>
        <Connection state={connection} />
      </header>
      {reconnect && (
        <div className="alert alert-warning">
          Reconecte sua conta Google para continuar. <a href={LOGIN_URL}>Reconectar</a>
        </div>
      )}
      {connection === 'gone' && <div className="alert alert-danger">Esta conversa não existe mais.</div>}
      <div className="timeline" aria-live="polite">
        {state.items.length === 0 && <p className="muted center">Nada por aqui ainda. Peça algo sobre sua agenda, e-mails ou documentos.</p>}
        {state.items.map((item) => (
          <Item key={`${item.kind}-${item.seq}`} item={item} contexts={contexts} onDecide={onDecide} onEnableContext={onEnableContext} />
        ))}
        <div ref={end} />
      </div>
      {error && <p className="alert alert-danger">{error}</p>}
      {state.usage && (
        <p className="usage muted small">
          {state.usage.modelCalls} chamada(s) ao modelo · {state.usage.inputTokens} tokens de entrada ({state.usage.cachedInputTokens} em cache) · {state.usage.outputTokens} de saída
        </p>
      )}
      <Composer busy={Boolean(state.activeTurn)} disabled={connection !== 'open'} onSend={onSend} onStop={onStop} />
    </section>
  )
}

function Item({ item, contexts, onDecide, onEnableContext }: { item: TimelineItem; contexts: Context[]; onDecide: ChatViewProps['onDecide']; onEnableContext: ChatViewProps['onEnableContext'] }) {
  switch (item.kind) {
    case 'user':
      return <MessageBubble role="user" text={item.text} />
    case 'assistant':
      return <MessageBubble role="assistant" text={item.text} streaming={item.streaming} />
    case 'toolSearch':
      return <ToolSearchCard item={item} />
    case 'toolCall':
      return <ToolCallCard item={item} />
    case 'approval':
      return <ApprovalCard item={item} onDecide={onDecide} />
    case 'contextSuggestion':
      return <ContextSuggestion contexts={item.contexts} all={contexts} onEnable={onEnableContext} />
    case 'turnEnd':
      return item.ok ? null : <p className="turn-failed">{FAILURE_MESSAGES[item.code ?? ''] ?? item.message}</p>
  }
}

function Connection({ state }: { state: ConnectionState }) {
  const labels: Record<ConnectionState, string> = {
    connecting: 'conectando…',
    open: 'ao vivo',
    reconnecting: 'reconectando…',
    closed: 'desconectado',
    gone: 'indisponível',
  }

  return (
    <span className={`connection connection-${state}`}>
      <span className="pulse" aria-hidden="true" />
      {labels[state]}
    </span>
  )
}
