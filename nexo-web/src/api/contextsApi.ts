import type { Context, ContextActivation } from '../domain/contexts'
import { ensureCsrf, request } from './http'

export const contextsApi = {
  list: () => request<Context[]>('GET', '/api/contexts'),
  enable: async (key: string) => {
    await ensureCsrf()

    return request<ContextActivation>('POST', `/api/contexts/${encodeURIComponent(key)}/enable`)
  },
  disable: async (key: string) => {
    await ensureCsrf()

    return request<Context>('POST', `/api/contexts/${encodeURIComponent(key)}/disable`)
  },
}
