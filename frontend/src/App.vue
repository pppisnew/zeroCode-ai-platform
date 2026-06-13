<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StarfieldBackground from './components/StarfieldBackground.vue'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const auth = useAuthStore()

onMounted(async () => {
  if (auth.isAuthenticated) {
    const valid = await auth.fetchMe()
    if (!valid && router.currentRoute.value.path !== '/login') {
      router.replace('/login')
    }
  }
})
</script>

<template>
  <StarfieldBackground />
  <router-view />
</template>
