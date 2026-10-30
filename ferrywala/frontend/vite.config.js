/* global process */
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Dev server proxies the API and WebSocket to the Spring backend, so the app
// never hard-codes a host. In production put both behind one origin (see
// deploy/nginx.conf) or set VITE_API_BASE_URL / VITE_WS_URL at build time.
const target = process.env.DEV_API_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  test: { environment: 'jsdom', globals: false, setupFiles: ['./src/test/setup.js'], css: false },
  define: { global: 'globalThis' }, // sockjs-client expects a `global`
  server: {
    proxy: {
      '/api': { target, changeOrigin: true },
      '/ws': { target, changeOrigin: true, ws: true },
    },
  },
  preview: {
    proxy: {
      '/api': { target, changeOrigin: true },
      '/ws': { target, changeOrigin: true, ws: true },
    },
  },
})
