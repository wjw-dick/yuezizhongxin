import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：默认 8080，可用 VITE_API_TARGET 覆盖（本机端口被占用时常用）
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5180,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      },
      '/files': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 900
  }
})
