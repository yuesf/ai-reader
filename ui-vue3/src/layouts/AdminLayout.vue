<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="left">
        <div class="brand" @click="$router.push('/')">AI Reader 管理后台</div>
        <el-menu mode="horizontal" :default-active="topActive" class="top-menu" @select="onTopSelect">
          <el-menu-item index="content">内容管理</el-menu-item>
          <el-menu-item index="tracking">埋点监控</el-menu-item>
          <el-menu-item index="system">系统管理</el-menu-item>
        </el-menu>
      </div>
      <div class="right">
        <el-dropdown>
          <span class="user-entry">
            <el-avatar size="small">{{ initials }}</el-avatar>
            <span class="name">{{ displayName || username || '用户' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click.native="$router.push('/profile')">个人信息</el-dropdown-item>
              <el-dropdown-item divided @click.native="goLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="aside">
        <el-menu :default-active="route.path" router>
          <el-sub-menu index="content">
            <template #title>报告管理</template>
            <el-menu-item index="/reports">报告列表</el-menu-item>
            <el-menu-item index="/reports/create">新建报告</el-menu-item>
            <el-menu-item index="/upload">文件上传</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="tracking">
            <template #title>埋点监控</template>
            <el-menu-item index="/tracker/dashboard">监控面板</el-menu-item>
            <el-menu-item index="/tracker/user-behavior">用户行为</el-menu-item>
            <el-menu-item index="/tracker/heatmap">页面热力图</el-menu-item>
            <el-menu-item index="/tracker/statistics">数据报表</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="system">
            <template #title>系统管理</template>
            <el-menu-item index="/users">账号管理</el-menu-item>
            <el-menu-item index="/profile">个人中心</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

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
const initials = computed(() => (displayName.value || username.value || 'U').slice(0, 1).toUpperCase());

const topActive = computed(() => {
  if (route.path.startsWith('/profile')) return 'system';
  if (route.path.startsWith('/tracker')) return 'tracking';
  return 'content';
});

function onTopSelect(key: string) {
  if (key === 'content') router.push('/reports');
  if (key === 'tracking') router.push('/tracker/dashboard');
  if (key === 'system') router.push('/profile');
}

function goLogout() {
  router.push('/logout');
}
</script>

<style scoped>
.layout { height: 100vh; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
.left { display: flex; align-items: center; gap: 16px; }
.brand { font-weight: 700; cursor: pointer; }
.top-menu { border-bottom: none; }
.right { display: flex; align-items: center; }
.user-entry { display: inline-flex; align-items: center; gap: 8px; cursor: pointer; }
.name { color: #606266; }
.aside { background: #fff; border-right: 1px solid #ebeef5; }
.main { background: #f5f7fa; }
</style>
