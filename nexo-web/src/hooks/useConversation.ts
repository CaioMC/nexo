import { useCallback, useEffect, useReducer, useRef, useState } from 'react'

import { CommandRejected, ConversationSocket, type ConnectionState } from '../api/conversationSocket'
import { conversationReducer, emptyConversation, type ConversationState } from '../domain/conversationReducer'
import type { EventEnvelope } from '../domain/events'

type Action = { type: 'event'; event: EventEnvelope } | { type: 'reset' }

function reducer(state: ConversationState, action: Action): ConversationState {
  return action.type === 'reset' ? emptyConversation : conversationReducer(state, action.event)
}

/**
 * Uma conversa ao vivo: socket + reducer. Abre com `lastSeq=0` (o replay monta a timeline inteira)
 * e o socket reconecta sozinho com o maior `seq` recebido.
 */
export function useConversation(conversationId: string | undefined, onEvent?: (event: EventEnvelope) => void) {
  const [state, dispatch] = useReducer(reducer, emptyConversation)
  const [connection, setConnection] = useState<ConnectionState>('connecting')
  const [error, setError] = useState<string | undefined>()
  const socket = useRef<ConversationSocket | undefined>(undefined)
  const listener = useRef(onEvent)

  listener.current = onEvent

  useEffect(() => {
    dispatch({ type: 'reset' })
    setError(undefined)

    if (!conversationId) {
      return
    }

    const channel = new ConversationSocket(conversationId, 0, {
      onEvent: (event) => {
        dispatch({ type: 'event', event })
        listener.current?.(event)
      },
      onState: setConnection,
    })

    socket.current = channel
    channel.connect()

    return () => channel.close()
  }, [conversationId])

  const command = useCallback(async (run: (channel: ConversationSocket) => Promise<unknown>) => {
    setError(undefined)

    try {
      if (socket.current) {
        await run(socket.current)
      }
    } catch (cause) {
      setError(cause instanceof CommandRejected ? cause.message : (cause as Error).message)
    }
  }, [])

  const send = useCallback(
    (text: string) => command((channel) => channel.send('user.message', { text, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone })),
    [command],
  )
  const decide = useCallback(
    (approvalId: string, decision: 'approve' | 'reject') => command((channel) => channel.send('approval.decide', { approvalId, decision })),
    [command],
  )
  const stop = useCallback(() => command((channel) => channel.send('turn.stop')), [command])

  return { state, connection, error, send, decide, stop }
}
