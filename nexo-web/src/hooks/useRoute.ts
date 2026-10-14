import { useCallback, useEffect, useState } from 'react'

/**
 * Rotas do app (doc 12, seção 1) sobre a History API, sem biblioteca.
 */
export type Route =
  | { page: 'login' }
  | { page: 'home' }
  | { page: 'conversation'; id: string }
  | { page: 'contexts' }
  | { page: 'account' }

export function parseRoute(pathname: string): Route {
  const conversation = /^\/c\/([0-9a-f-]{36})$/i.exec(pathname)

  if (conversation) {
    return { page: 'conversation', id: conversation[1]! }
  }

  switch (pathname) {
    case '/login':
      return { page: 'login' }
    case '/settings/contexts':
      return { page: 'contexts' }
    case '/settings/account':
      return { page: 'account' }
    default:
      return { page: 'home' }
  }
}

export function useRoute(): { route: Route; search: URLSearchParams; navigate: (path: string) => void } {
  const [location, setLocation] = useState(() => ({ pathname: window.location.pathname, search: window.location.search }))

  useEffect(() => {
    const onPop = () => setLocation({ pathname: window.location.pathname, search: window.location.search })

    window.addEventListener('popstate', onPop)

    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const navigate = useCallback((path: string) => {
    window.history.pushState(null, '', path)
    setLocation({ pathname: window.location.pathname, search: window.location.search })
  }, [])

  return { route: parseRoute(location.pathname), search: new URLSearchParams(location.search), navigate }
}
