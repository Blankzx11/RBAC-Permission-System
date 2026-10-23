import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  define: { 'process.env.NODE_ENV': JSON.stringify('production') },
  build: {
    lib: { entry: 'src/main.ts', name: 'Rbac0Prototype', formats: ['iife'], fileName: () => 'rbac0-prototype.js', cssFileName: 'rbac0-prototype' },
    outDir: 'dist', emptyOutDir: true, cssCodeSplit: false,
  },
})
