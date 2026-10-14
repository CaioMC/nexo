import { request } from './http'

export interface CatalogOverview {
  contexts: number
  namespaces: number
  tools: number
  dependencies: number
}

export const catalogApi = {
  overview: () => request<CatalogOverview>('GET', '/api/catalog'),
}
