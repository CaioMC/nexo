/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em desenvolvimento o front e o servidor parecem uma origem só (BFF): /api, /ws e o fluxo OAuth
// vão para o nexo-server (que só escuta em 127.0.0.1:8080). Abra http://localhost:5173.
const server = 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': server,
      '/oauth2': server,
      '/ws': { target: 'ws://127.0.0.1:8080', ws: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/setupTests.ts'],
  },
})
