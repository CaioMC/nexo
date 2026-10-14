import type { EventEnvelope } from '../domain/events'
import { ensureCsrf, request } from './http'

export interface Conversation {
  id: string
  title: string
  lastSeq: number
  activeTurn: { id: string; status: 'RUNNING' | 'WAITING_APPROVAL' } | null
  createdAt: string
  updatedAt: string
}

export const conversationsApi = {
  list: () => request<Conversation[]>('GET', '/api/conversations?limit=50'),
  get: (id: string) => request<Conversation>('GET', `/api/conversations/${id}`),
  create: async (title?: string) => {
    await ensureCsrf()

    return request<Conversation>('POST', '/api/conversations', title ? { title } : {})
  },
  rename: async (id: string, title: string) => {
    await ensureCsrf()

    return request<Conversation>('PATCH', `/api/conversations/${id}`, { title })
  },
  remove: async (id: string) => {
    await ensureCsrf()

    return request<void>('DELETE', `/api/conversations/${id}`)
  },
  events: (id: string, afterSeq = 0) => request<EventEnvelope[]>('GET', `/api/conversations/${id}/events?afterSeq=${afterSeq}`),
}
