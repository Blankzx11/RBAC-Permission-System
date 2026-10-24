import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

const apiTarget = process.env.AUDIT_API_TARGET ?? 'http://127.0.0.1:8080'
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: { host: '127.0.0.1', proxy: { '/api': apiTarget } },
  preview: { host: '127.0.0.1', proxy: { '/api': apiTarget } },
})
