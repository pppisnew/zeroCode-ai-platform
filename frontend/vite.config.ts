import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    chunkSizeWarningLimit: 3000,
    rolldownOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('/node_modules/@arco-design/')) {
            return 'arco-vendor'
          }
          if (
            id.includes('/node_modules/@vue/')
            || id.includes('/node_modules/vue/')
            || id.includes('/node_modules/pinia/')
          ) {
            return 'vue-vendor'
          }
        },
      },
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        timeout: 180000,
      },
    },
  },
})
