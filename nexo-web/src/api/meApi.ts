import { ensureCsrf, request } from './http'

export interface Me {
  id: string
  email: string
  displayName: string
  pictureUrl: string | null
}

export const meApi = {
  me: () => request<Me>('GET', '/api/me'),
  logout: async () => {
    await ensureCsrf()

    return request<void>('POST', '/api/auth/logout')
  },
  disconnect: async () => {
    await ensureCsrf()

    return request<void>('POST', '/api/auth/disconnect')
  },
}

/**
 * Login e consentimento acontecem no Google, com o navegador: o servidor monta o pedido.
 */
export const LOGIN_URL = '/oauth2/authorization/google'
