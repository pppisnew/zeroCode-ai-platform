import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface User {
  id: number
  username: string
  role: string
  createTime: string
}

const TOKEN_KEY = 'zerocode-token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => token.value != null)

  function setAuth(newToken: string, newUser: User) {
    token.value = newToken
    user.value = newUser
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  async function login(username: string, password: string) {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    const payload = await response.json()
    if (!response.ok || payload.code !== 0) {
      throw new Error(payload.message || 'Login failed')
    }
    setAuth(payload.data.token, payload.data.user)
  }

  async function register(username: string, password: string) {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    const payload = await response.json()
    if (!response.ok || payload.code !== 0) {
      throw new Error(payload.message || 'Registration failed')
    }
    setAuth(payload.data.token, payload.data.user)
  }

  async function logout() {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'ZeroCode-Auth': token.value },
      })
    } finally {
      clearAuth()
    }
  }

  async function fetchMe(): Promise<boolean> {
    if (!token.value) return false
    try {
      const response = await fetch('/api/auth/me', {
        headers: { 'ZeroCode-Auth': token.value },
      })
      const payload = await response.json()
      if (response.ok && payload.code === 0) {
        user.value = payload.data
        return true
      }
    } catch {
      // token invalid
    }
    clearAuth()
    return false
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    register,
    logout,
    fetchMe,
    setAuth,
    clearAuth,
  }
})
