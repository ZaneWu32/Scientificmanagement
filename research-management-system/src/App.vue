<template>
  <el-config-provider :locale="zhCn">
    <ErrorBoundary>
      <router-view />
    </ErrorBoundary>
  </el-config-provider>
</template>

<script setup lang="ts">
import ErrorBoundary from '@/components/ErrorBoundary.vue';
import { useUserStore } from '@/stores/user';
import { ElConfigProvider } from "element-plus";
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import { onMounted } from 'vue';

const userStore = useUserStore()

onMounted(() => {
  userStore.initUserInfo()
})
</script>
