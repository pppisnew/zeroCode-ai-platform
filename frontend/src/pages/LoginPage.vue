<script setup lang="ts">
import { Message } from '@arco-design/web-vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const isRegister = ref(false)
const username = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

async function handleSubmit() {
  if (!username.value.trim() || !password.value.trim()) {
    errorMessage.value = '请填写用户名和密码'
    return
  }
  isLoading.value = true
  errorMessage.value = ''
  try {
    if (isRegister.value) {
      await auth.register(username.value.trim(), password.value)
      Message.success('注册成功')
    } else {
      await auth.login(username.value.trim(), password.value)
    }
    router.replace('/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '操作失败'
  } finally {
    isLoading.value = false
  }
}

function toggleMode() {
  isRegister.value = !isRegister.value
  errorMessage.value = ''
}
</script>

<template>
  <main class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="brand-mark">Z</div>
        <h1>ZeroCode</h1>
        <p>AI Web App Generator</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <h2>{{ isRegister ? '注册' : '登录' }}</h2>

        <a-input
          v-model="username"
          placeholder="用户名"
          size="large"
          :min-length="3"
          :max-length="64"
        />
        <a-input
          v-model="password"
          type="password"
          placeholder="密码"
          size="large"
          :min-length="6"
        />

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <a-button
          type="primary"
          long
          size="large"
          html-type="submit"
          :loading="isLoading"
        >
          {{ isRegister ? '注册' : '登录' }}
        </a-button>
      </form>

      <p class="login-toggle">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <button type="button" class="text-action" @click="toggleMode">
          {{ isRegister ? '去登录' : '去注册' }}
        </button>
      </p>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
  border: 1px solid var(--gold-mid);
  border-radius: 8px;
  background: var(--bg-panel);
  backdrop-filter: blur(14px);
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 32px;
}

.login-header .brand-mark {
  width: 48px;
  height: 48px;
  font-size: 24px;
}

.login-header h1 {
  margin: 0;
  color: var(--text-warm);
  font-size: 22px;
  font-weight: 700;
}

.login-header p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.login-form h2 {
  margin: 0 0 4px;
  color: var(--gold-400);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.login-toggle {
  margin: 20px 0 0;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
