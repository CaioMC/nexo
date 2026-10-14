import { useCallback, useEffect, useState } from 'react'

import { ApiError, onUnauthorized } from '../api/http'
import { meApi, type Me } from '../api/meApi'

export type Session = { status: 'loading' } | { status: 'anonymous' } | { status: 'signed-in'; me: Me }

/**
 * Quem está logado. `GET /api/me` → 401 vira anônimo (o app mostra o login).
 */
export function useSession() {
  const [session, setSession] = useState<Session>({ status: 'loading' })

  const refresh = useCallback(async () => {
    try {
      setSession({ status: 'signed-in', me: await meApi.me() })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setSession({ status: 'anonymous' })
      } else {
        throw error
      }
    }
  }, [])

  useEffect(() => {
    void refresh()

    return onUnauthorized(() => setSession({ status: 'anonymous' }))
  }, [refresh])

  const logout = useCallback(async () => {
    await meApi.logout()
    setSession({ status: 'anonymous' })
  }, [])

  const disconnect = useCallback(async () => {
    await meApi.disconnect()
    setSession({ status: 'anonymous' })
  }, [])

  return { session, refresh, logout, disconnect }
}
