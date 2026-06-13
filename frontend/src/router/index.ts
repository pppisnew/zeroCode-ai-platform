import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../pages/LoginPage.vue'),
    },
    {
      path: '/',
      name: 'workspace',
      component: () => import('../pages/WorkspacePage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

export default router
