import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'
import './style.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

// Activate Arco Design dark theme for Star Rail overrides
document.body.setAttribute('arco-theme', 'dark')

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(ArcoVue)
app.use(router)

// Auth guard: redirect to /login if route requires auth
router.beforeEach(async (to: { meta: { requiresAuth?: boolean }; path: string }) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      return '/login'
    }
    const valid = await auth.fetchMe()
    if (!valid) {
      return '/login'
    }
  }
})

app.mount('#app')
