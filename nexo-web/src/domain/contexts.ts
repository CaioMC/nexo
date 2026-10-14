/**
 * Contextos Google como o front mostra (doc 05). A chave é a do servidor.
 */

export interface Context {
  key: string
  name: string
  description: string
  enabled: boolean
  scopesGranted: boolean
  scopes: string[]
  toolCount: number
}

export type ContextActivation =
  | { status: 'enabled'; context: string }
  | { status: 'consent_required'; context: string; authorizationUrl: string }

const LOOKS: Record<string, { icon: string; color: string }> = {
  gmail: { icon: '✉', color: '#ea4335' },
  drive: { icon: '▲', color: '#188038' },
  docs: { icon: '≡', color: '#1a73e8' },
  sheets: { icon: '▦', color: '#188038' },
  slides: { icon: '▭', color: '#f9ab00' },
  calendar: { icon: '◷', color: '#1a73e8' },
  chat: { icon: '❝', color: '#00897b' },
  people: { icon: '☺', color: '#5f6368' },
}

export function look(key: string): { icon: string; color: string } {
  return LOOKS[key] ?? { icon: '•', color: '#5f6368' }
}

export function contextName(key: string, contexts: Context[] = []): string {
  return contexts.find((context) => context.key === key)?.name ?? key.charAt(0).toUpperCase() + key.slice(1)
}

/**
 * Escopo legível: só o fim da URL (`calendar`, `gmail.modify`).
 */
export function shortScope(scope: string): string {
  return scope.replace('https://www.googleapis.com/auth/', '')
}
