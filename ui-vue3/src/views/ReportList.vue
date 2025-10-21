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
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="onGenerateSummary(row)" :loading="generatingSummaryIds.has(row.id)">
              生成摘要
            </el-button>
            <el-button type="success" link @click="onPublishToWeChat(row)" :loading="publishingIds.has(row.id)">
              发布公众号
            </el-button>
            <el-button type="warning" link @click="onEdit(row)">
              编辑
            </el-button>
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

    <!-- 编辑报告对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑报告" width="800px" :close-on-click-modal="false">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input v-model="editForm.title" placeholder="请输入报告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="editForm.category" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="来源" prop="source">
              <el-select v-model="editForm.source" placeholder="请选择来源" style="width: 100%">
                <el-option v-for="s in sources" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页数" prop="pages">
              <el-input-number v-model="editForm.pages" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布日期" prop="publishDate">
              <el-date-picker v-model="editForm.publishDate" type="date" placeholder="选择发布日期" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否免费" prop="isFree">
              <el-switch v-model="editForm.isFree" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" v-if="!editForm.isFree">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="editForm.price" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="editForm.summary" type="textarea" :rows="4" placeholder="请输入报告摘要" />
        </el-form-item>
        
        <el-form-item label="标签" prop="tags">
          <el-select v-model="editForm.tags" multiple filterable allow-create default-first-option placeholder="请输入标签" style="width: 100%">
            <el-option v-for="tag in editForm.tags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="onSaveEdit" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { fetchReports, deleteReport, batchDelete, generateSummary, updateReport, publishToWeChat, checkPublishStatus, type ReportItem, type ReportListRequest } from '../api';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';

const query = reactive<ReportListRequest>({ page: 1, pageSize: 10, sortBy: 'publishDate', sortOrder: 'desc' });
const rows = ref<ReportItem[]>([]);
const total = ref(0);
const selection = ref<ReportItem[]>([]);
const generatingSummaryIds = ref<Set<string>>(new Set());
const publishingIds = ref<Set<string>>(new Set());

// 编辑相关状态
const editDialogVisible = ref(false);
const editFormRef = ref<FormInstance>();
const saving = ref(false);
const editForm = reactive<Partial<ReportItem>>({});

// 表单验证规则
const editRules: FormRules = {
  title: [
    { required: true, message: '请输入报告标题', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  source: [
    { required: true, message: '请选择来源', trigger: 'change' }
  ]
};

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

async function onGenerateSummary(row: ReportItem) {
  try {
    // 设置加载状态
    generatingSummaryIds.value.add(row.id);
    
    const { data } = await generateSummary(row.id);
    if (data.code === 200) {
      ElMessage.success('摘要生成成功');
      // 更新本地数据
      row.summary = data.data;
      // 重新加载数据以确保数据同步
      loadData();
    } else {
      ElMessage.error(data.message || '摘要生成失败');
    }
  } catch (error) {
    ElMessage.error('摘要生成失败');
    console.error('生成摘要失败:', error);
  } finally {
    // 清除加载状态
    generatingSummaryIds.value.delete(row.id);
  }
}

async function onPublishToWeChat(row: ReportItem) {
  try {
    // 先检查发布状态
    const statusResult = await checkPublishStatus(row.id);
    if (statusResult.data.code !== 200) {
      ElMessage.error(statusResult.data.message || '报告状态检查失败');
      return;
    }
    
    // 设置加载状态
    publishingIds.value.add(row.id);
    
    const { data } = await publishToWeChat(row.id);
    if (data.code === 200) {
      ElMessage.success(`发布成功！草稿媒体ID: ${data.data.mediaId}`);
    } else {
      ElMessage.error(data.message || '发布到公众号失败');
    }
  } catch (error: any) {
    console.error('发布到公众号失败:', error);
    
    // 尝试从错误响应中获取更详细的错误信息
    let errorMessage = '发布到公众号失败';
    if (error?.response?.data?.message) {
      errorMessage = error.response.data.message;
    } else if (error?.message) {
      errorMessage = error.message;
    }
    
    ElMessage.error(errorMessage);
  } finally {
    // 清除加载状态
    publishingIds.value.delete(row.id);
  }
}

async function onEdit(row: ReportItem) {
  // 复制数据到编辑表单
  Object.assign(editForm, {
    id: row.id,
    title: row.title,
    summary: row.summary,
    source: row.source,
    category: row.category,
    pages: row.pages,
    fileSize: row.fileSize,
    publishDate: row.publishDate,
    updateDate: row.updateDate,
    thumbnail: row.thumbnail,
    tags: row.tags ? [...row.tags] : [],
    isFree: row.isFree,
    price: row.price
  });
  
  editDialogVisible.value = true;
}

async function onSaveEdit() {
  if (!editFormRef.value) return;
  
  try {
    await editFormRef.value.validate();
    
    saving.value = true;
    
    const { data } = await updateReport(editForm.id!, editForm);
    if (data.code === 200) {
      ElMessage.success('更新成功');
      editDialogVisible.value = false;
      // 重新加载数据
      loadData();
    } else {
      ElMessage.error(data.message || '更新失败');
    }
  } catch (error) {
    if (error !== false) { // 不是表单验证错误
      ElMessage.error('更新失败');
      console.error('更新失败:', error);
    }
  } finally {
    saving.value = false;
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


