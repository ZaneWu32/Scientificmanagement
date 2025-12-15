<template>
  <ErrorBoundary>
    <router-view />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const userStore = useUserStore()

onMounted(() => {
  userStore.initUserInfo()
})
</script>
