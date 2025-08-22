<template>
  <div class="page">
    <el-card>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <!-- 报告文件放在第一行 -->
        <el-form-item label="报告文件">
          <ReportFileUpload v-model="reportFileInfo" />
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input type="textarea" v-model="form.summary" :rows="4" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="行业报告" value="行业报告" />
            <el-option label="技术白皮书" value="技术白皮书" />
            <el-option label="投研报告" value="投研报告" />
          </el-select>
        </el-form-item>
        <el-form-item label="页数">
          <el-input-number v-model="form.pages" :min="1" />
        </el-form-item>
        <el-form-item label="文件大小">
          <el-input v-model="form.fileSize" disabled />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="form.thumbnail" placeholder="图片URL" />
          <div class="paste-upload-area" 
               @paste="handlePasteImage" 
               @click="focusPasteArea"
               :class="{ 'has-image': form.thumbnail }">
            <div v-if="!form.thumbnail" class="paste-placeholder">
              <el-icon><Picture /></el-icon>
              <span>粘贴图片到此处</span>
            </div>
            <img v-else :src="form.thumbnail" class="preview-image" />
          </div>
        </el-form-item>
        
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option placeholder="输入或选择标签">
            <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否免费">
          <el-switch v-model="form.isFree" />
        </el-form-item>
        <el-form-item v-if="!form.isFree" label="价格">
          <el-input-number v-model="form.price" :min="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { createReport, uploadImage } from '../api';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import ReportFileUpload from '../components/ReportFileUpload.vue';
import { Picture } from '@element-plus/icons-vue';

const form = reactive({
  title: '',
  summary: '',
  source: '',
  category: '',
  pages: undefined as unknown as string,
  fileSize: undefined as unknown as string,
  publishDate: new Date().toISOString().split('T')[0], // 默认当天
  thumbnail: '',
  thumbnailId: '',
  tags: [] as string[],
  isFree: true,
  price: 0,
  reportFileId: ''
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
});

const tagOptions = ref<string[]>(['AI', '大模型', 'AIGC']);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const reportFileInfo = ref<{
  fileId: string;
  filename: string;
  size: string;
} | null>(null);

// 监听表单中的文件信息变化，同步到组件
watch(() => [form.reportFileId, form.title, form.fileSize], ([fileId, filename, size]) => {
  if (fileId && filename && size) {
    reportFileInfo.value = { fileId, filename, size };
  } else {
    reportFileInfo.value = null;
  }
}, { immediate: true });

// 监听reportFileInfo变化，同步到form中
watch(reportFileInfo, (newVal) => {
  if (newVal) {
    form.reportFileId = newVal.fileId;
    form.title = newVal.filename; // 文件名赋值到标题
    form.fileSize = newVal.size;  // 文件大小赋值到文件大小字段
  }
}, { deep: true });

function normalize() {
  if (form.isFree) {
    form.price = 0;
  }
}

// 粘贴图片处理方法
async function handlePasteImage(event: ClipboardEvent) {
  const items = event.clipboardData?.items;
  if (!items) return;
  
  for (let i = 0; i < items.length; i++) {
    if (items[i].type.indexOf('image') !== -1) {
      const file = items[i].getAsFile();
      if (file) {
        try {
          const { data } = await uploadImage(file);
          if (data.code === 200) {
            form.thumbnail = data.data.thumbnail;
            form.thumbnailId = data.data.fileId;
            ElMessage.success('封面粘贴上传成功');
          } else {
            ElMessage.error(data.message || '封面粘贴上传失败');
          }
        } catch (error) {
          ElMessage.error('封面粘贴上传失败');
        }
      }
      break;
    }
  }
}

// 聚焦粘贴区域
function focusPasteArea() {
  // 可以添加一些视觉反馈
}


async function onSubmit() {
  normalize();
  
  // 同步报告文件信息到表单
  if (reportFileInfo.value) {
    form.reportFileId = reportFileInfo.value.fileId;
  }
  
  await formRef.value?.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const { data } = await createReport(form);
      if (data.code === 200) {
        ElMessage.success('创建成功');
        history.back();
      } else {
        ElMessage.error(data.message || '创建失败');
      }
    } finally {
      submitting.value = false;
    }
  });
}
</script>

<style scoped>
.page { padding: 16px; }

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.file-size {
  color: #909399;
  font-size: 12px;
}

.paste-upload-area {
  margin-top: 8px;
  width: 200px;
  height: 120px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
}

.paste-upload-area:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.paste-upload-area.has-image {
  border-style: solid;
  border-color: #67c23a;
  background: #f0f9ff;
}

.paste-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
}

.paste-placeholder .el-icon {
  font-size: 24px;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
  border-radius: 4px;
}
</style>