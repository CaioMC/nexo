import { useCallback, useEffect, useState } from 'react'

import { conversationsApi, type Conversation } from '../api/conversationsApi'

export function useConversations(enabled: boolean) {
  const [conversations, setConversations] = useState<Conversation[]>([])

  const reload = useCallback(async () => {
    setConversations(await conversationsApi.list())
  }, [])

  useEffect(() => {
    if (enabled) {
      void reload().catch(() => undefined)
    }
  }, [enabled, reload])

  const create = useCallback(async () => {
    const created = await conversationsApi.create()

    setConversations((current) => [created, ...current])

    return created
  }, [])

  const remove = useCallback(async (id: string) => {
    await conversationsApi.remove(id)
    setConversations((current) => current.filter((conversation) => conversation.id !== id))
  }, [])

  const rename = useCallback((id: string, title: string) => {
    setConversations((current) => current.map((conversation) => (conversation.id === id ? { ...conversation, title } : conversation)))
  }, [])

  return { conversations, reload, create, remove, rename }
}
