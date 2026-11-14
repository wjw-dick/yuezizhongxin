import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：默认 8080，可用 VITE_API_TARGET 覆盖（本机端口被占用时常用）
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [vue()],
  // 部署在 /h5/ 子路径；如需独立域名改为 '/'
  base: '/h5/',
  server: {
    port: 5190,
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
