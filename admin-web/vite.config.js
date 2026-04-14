import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://8.137.160.86',
        changeOrigin: true
      },
      '/uploads': {
        target: 'https://8.137.160.86',
        changeOrigin: true
      }
    }
  }
})
