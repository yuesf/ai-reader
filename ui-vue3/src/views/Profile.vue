<template>
  <div class="page">
    <el-card>
      <template #header>个人中心</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ username }}</el-descriptions-item>
        <el-descriptions-item label="显示名">{{ displayName }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ userId }}</el-descriptions-item>
        <el-descriptions-item label="Token 过期">{{ expReadable }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 16px;">
        <el-button type="warning" @click="$router.push('/logout')">退出登录</el-button>
      </div>
    </el-card>
  </div>
  
</template>

<script setup lang="ts">
import { computed } from 'vue';

function parseJwtPayload(): any | null {
  try {
    const token = localStorage.getItem('token');
    if (!token) return null;
    const payload = token.split('.')[1];
    const base = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(atob(base).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

const jwt = computed(() => parseJwtPayload());
const username = computed(() => jwt.value?.username ?? '');
const displayName = computed(() => jwt.value?.displayName ?? '');
const userId = computed(() => jwt.value?.sub ?? '');
const expReadable = computed(() => {
  if (!jwt.value?.exp) return '';
  const d = new Date(jwt.value.exp * 1000);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
});
</script>

<style scoped>
.page { padding: 16px; }
</style>


