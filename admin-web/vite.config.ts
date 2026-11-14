import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址：默认 18090（本机 18080 常被其他项目守护脚本占用），可用 VITE_API_TARGET 覆盖
const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:18090'

// 开发期通过代理直连后端，避免跨域与租户子域名困扰
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
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
    chunkSizeWarningLimit: 1500
  }
})
