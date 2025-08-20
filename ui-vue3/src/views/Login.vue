<template>
  <div class="login-page">
    <el-card class="card">
      <div class="title">后台登录</div>
      <el-form :model="form" @keyup.enter="onSubmit" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="onSubmit" style="width:100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login } from '../api';

const router = useRouter();
const form = reactive({ username: 'admin', password: 'admin123' });
const loading = ref(false);

async function onSubmit() {
  if (!form.username || !form.password) return;
  loading.value = true;
  try {
    const { data } = await login(form.username, form.password);
    if (data.code === 200) {
      localStorage.setItem('token', data.data.token as unknown as string);
      ElMessage.success('登录成功');
      router.replace('/');
    } else {
      ElMessage.error(data.message || '登录失败');
    }
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f3f5;
}
.card { width: 360px; }
.title { font-size: 20px; font-weight: 600; margin-bottom: 16px; text-align: center; }
</style>


