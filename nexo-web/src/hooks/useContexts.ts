import { useCallback, useEffect, useState } from 'react'

import { contextsApi } from '../api/contextsApi'
import type { Context } from '../domain/contexts'

/**
 * Contextos do usuário. Ligar sem escopo leva ao Google ({@code consent_required}).
 */
export function useContexts(navigateAway: (url: string) => void = (url) => window.location.assign(url)) {
  const [contexts, setContexts] = useState<Context[]>([])
  const [busy, setBusy] = useState<string | undefined>()
  const [error, setError] = useState<string | undefined>()

  const reload = useCallback(async () => {
    setContexts(await contextsApi.list())
  }, [])

  useEffect(() => {
    void reload().catch((cause: Error) => setError(cause.message))
  }, [reload])

  const toggle = useCallback(
    async (context: Context) => {
      setBusy(context.key)
      setError(undefined)

      try {
        if (context.enabled) {
          const updated = await contextsApi.disable(context.key)

          setContexts((current) => current.map((item) => (item.key === updated.key ? updated : item)))

          return
        }

        const activation = await contextsApi.enable(context.key)

        if (activation.status === 'consent_required') {
          navigateAway(activation.authorizationUrl)

          return
        }

        await reload()
      } catch (cause) {
        setError((cause as Error).message)
      } finally {
        setBusy(undefined)
      }
    },
    [navigateAway, reload],
  )

  return { contexts, busy, error, toggle, reload }
}
