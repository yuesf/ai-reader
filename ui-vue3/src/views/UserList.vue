<template>
  <div class="page">
    <el-card class="toolbar">
      <div class="toolbar-row">
        <el-input v-model="query.keyword" placeholder="搜索用户名/显示名" clearable style="width: 240px" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="openCreate">新建用户</el-button>
      </div>
    </el-card>

    <el-card>
      <el-table :data="rows" height="calc(100vh - 300px)">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="180" />
        <el-table-column prop="displayName" label="显示名" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该用户？" @confirm="() => onDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          v-model:page-size="query.pageSize"
          v-model:current-page="query.page"
          :page-sizes="[10,20,30,50]"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新建用户'" width="520">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="editing" />
        </el-form-item>
        <el-form-item label="密码" :required="!editing">
          <el-input v-model="form.password" type="password" show-password placeholder="留空则不修改" />
        </el-form-item>
        <el-form-item label="确认密码" :required="!editing || !!form.password">
          <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入密码（仅修改密码时必填）" />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusSwitch" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchUsers, createUser, updateUser, deleteUserApi, type UserItem, type UserListReq } from '../api';

const query = reactive<UserListReq>({ page: 1, pageSize: 10 });
const rows = ref<UserItem[]>([]);
const total = ref(0);

const dialogVisible = ref(false);
const editing = ref(false);
const saving = ref(false);
const form = reactive<UserItem>({ username: '', password: '', displayName: '', status: 1 });
const confirmPassword = ref('');
const statusSwitch = computed({
  get: () => form.status ?? 1,
  set: (v: number) => form.status = v
});

async function loadData() {
  const { data } = await fetchUsers(query);
  if (data.code === 200) {
    rows.value = data.data.list;
    total.value = data.data.total as unknown as number;
  } else {
    ElMessage.error(data.message || '加载失败');
  }
}

function reset() {
  Object.assign(query, { page: 1, pageSize: 10, keyword: '', status: undefined });
  loadData();
}

function openCreate() {
  editing.value = false;
  Object.assign(form, { id: undefined, username: '', password: '', displayName: '', status: 1 });
  confirmPassword.value = '';
  dialogVisible.value = true;
}

function openEdit(row: UserItem) {
  editing.value = true;
  Object.assign(form, { id: row.id, username: row.username, password: '', displayName: row.displayName, status: row.status ?? 1 });
  confirmPassword.value = '';
  dialogVisible.value = true;
}

async function onSave() {
  if (!form.username) return ElMessage.warning('请输入用户名');
  if (!editing.value) {
    if (!form.password) return ElMessage.warning('请输入密码');
    if (!confirmPassword.value) return ElMessage.warning('请再次输入密码');
    if (form.password !== confirmPassword.value) return ElMessage.warning('两次输入的密码不一致');
  } else if (form.password) {
    if (!confirmPassword.value) return ElMessage.warning('请再次输入密码');
    if (form.password !== confirmPassword.value) return ElMessage.warning('两次输入的密码不一致');
  }
  saving.value = true;
  try {
    if (editing.value) {
      const payload: UserItem = { id: form.id, username: form.username, displayName: form.displayName, status: form.status };
      if (form.password) payload.password = form.password;
      const { data } = await updateUser(payload);
      if (data.code === 200) {
        ElMessage.success('保存成功');
        dialogVisible.value = false;
        loadData();
      } else {
        ElMessage.error(data.message || '保存失败');
      }
    } else {
      const { data } = await createUser(form);
      if (data.code === 200) {
        ElMessage.success('创建成功');
        dialogVisible.value = false;
        loadData();
      } else {
        ElMessage.error(data.message || '创建失败');
      }
    }
  } finally {
    saving.value = false;
  }
}

async function onDelete(row: UserItem) {
  if (!row.id) return;
  const { data } = await deleteUserApi(row.id);
  if (data.code === 200) {
    ElMessage.success('删除成功');
    loadData();
  } else {
    ElMessage.error(data.message || '删除失败');
  }
}

onMounted(loadData);
</script>

<style scoped>
.page { padding: 16px; }
.toolbar { margin-bottom: 12px; }
.toolbar-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; padding-top: 12px; }
</style>


