<template>
  <div class="page">
    <el-card class="toolbar">
      <div class="toolbar-row">
        <el-input v-model="query.keyword" placeholder="关键词" clearable style="width: 220px" />
        <el-select v-model="query.category" placeholder="分类" clearable style="width: 160px; margin-left: 12px;">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.source" placeholder="来源" clearable style="width: 160px; margin-left: 12px;">
          <el-option v-for="s in sources" :key="s" :label="s" :value="s" />
        </el-select>
        <el-button type="primary" style="margin-left: 12px" @click="loadData">搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="success" @click="$router.push('/reports/create')">新增</el-button>
        <el-button type="info" @click="$router.push('/upload')">文件上传</el-button>
        <el-popconfirm title="确认批量删除选中项？" @confirm="onBatchDelete">
          <template #reference>
            <el-button type="danger" :disabled="!selection.length">批量删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </el-card>

    <el-card>
      <el-table :data="rows" @selection-change="val => selection = val" height="calc(100vh - 280px)">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="title" label="标题" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <el-image v-if="row.thumbnail" :src="row.thumbnail" fit="cover" style="width: 40px; height: 40px; margin-right: 8px" />
              <div class="col">
                <div class="t1">{{ row.title }}</div>
                <div class="t2">{{ row.summary }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="source" label="来源" width="140" />
        <el-table-column prop="publishDate" label="发布日期" width="120" />
        <el-table-column prop="viewCount" label="浏览" width="90" />
        <el-table-column prop="downloadCount" label="下载" width="90" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确认删除该报告？" @confirm="() => onDelete(row)">
              <template #reference>
                <el-button type="danger" link>删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { fetchReports, deleteReport, batchDelete, type ReportItem, type ReportListRequest } from '../api';
import { ElMessage } from 'element-plus';

const query = reactive<ReportListRequest>({ page: 1, pageSize: 10, sortBy: 'publishDate', sortOrder: 'desc' });
const rows = ref<ReportItem[]>([]);
const total = ref(0);
const selection = ref<ReportItem[]>([]);

const categories = ref<string[]>(['行业报告', '技术白皮书', '投研报告']);
const sources = ref<string[]>(['艾瑞', '麦肯锡', '某咨询']);

async function loadData() {
  const { data } = await fetchReports(query);
  if (data.code === 200) {
    rows.value = data.data.list;
    total.value = data.data.total as unknown as number;
  } else {
    ElMessage.error(data.message || '加载失败');
  }
}

function reset() {
  Object.assign(query, { page: 1, pageSize: 10, keyword: '', category: '', source: '' });
  loadData();
}

async function onDelete(row: ReportItem) {
  const { data } = await deleteReport(row.id);
  if (data.code === 200) {
    ElMessage.success('删除成功');
    loadData();
  } else {
    ElMessage.error(data.message || '删除失败');
  }
}

async function onBatchDelete() {
  const ids = selection.value.map(s => s.id);
  if (!ids.length) return;
  const { data } = await batchDelete(ids);
  if (data.code === 200) {
    ElMessage.success(`已删除 ${data.data} 条`);
    selection.value = [];
    loadData();
  } else {
    ElMessage.error(data.message || '批量删除失败');
  }
}

onMounted(loadData);
</script>

<style scoped>
.page { padding: 16px; }
.toolbar { margin-bottom: 12px; }
.toolbar-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.title-cell { display: flex; align-items: center; }
.col { display: flex; flex-direction: column; }
.t1 { font-weight: 600; }
.t2 { color: #8b8b8b; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 520px; }
.pager { display: flex; justify-content: flex-end; padding-top: 12px; }
</style>


