/**
 * Nome de ferramenta para gente: `google_calendar_list_events` → `calendar · list events`.
 */
export function toolLabel(name: string): string {
  const parts = name.replace(/^google_/, '').split('_')
  const context = parts.shift() ?? ''

  return `${context} · ${parts.join(' ')}`
}

export const FAILURE_MESSAGES: Record<string, string> = {
  STOPPED: 'Você parou este turno.',
  LIMIT_MODEL_CALLS: 'O turno passou do limite de chamadas ao modelo.',
  LIMIT_TOOL_CALLS: 'O turno passou do limite de chamadas de ferramenta.',
  TIMEOUT: 'O turno passou do tempo máximo.',
  MODEL_ERROR: 'O modelo falhou ou não respondeu.',
  GOOGLE_RECONNECT_REQUIRED: 'O acesso ao Google foi revogado ou venceu. Reconecte sua conta.',
  SERVER_RESTARTED: 'O servidor reiniciou no meio do turno.',
}
