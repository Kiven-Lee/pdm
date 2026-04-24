import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// Vite 配置文件
export default defineConfig({
  plugins: [vue()],
  resolve: {
    // 路径别名：@ 指向 src 目录
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    // 开发环境代理：将 /api 请求转发到后端网关，解决跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
