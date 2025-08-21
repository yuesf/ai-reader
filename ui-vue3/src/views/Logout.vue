<template>
  <div class="logout-page">正在退出...</div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { logout } from '../api';

const router = useRouter();

onMounted(async () => {
  try {
    await logout();
  } catch {}
  localStorage.removeItem('token');
  router.replace('/login');
});
</script>

<style scoped>
.logout-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}
</style>


