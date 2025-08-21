<template>
  <div class="page">
    <el-card>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
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
          <el-input-number v-model="form.fileSize" :min="0" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="form.publishDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="封面">
          <el-input v-model="form.thumbnail" placeholder="图片URL" />
          <el-upload
            class="upload-demo"
            :action="''"
            :http-request="handleImageUpload"
            :show-file-list="false"
            accept="image/*"
            style="margin-top: 8px;"
          >
            <el-button type="primary" size="small">上传封面</el-button>
          </el-upload>
        </el-form-item>
        
        <el-form-item label="报告文件">
          <ReportFileUpload v-model="reportFileInfo" />
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

const form = reactive({
  title: '',
  summary: '',
  source: '',
  category: '',
  pages: undefined as unknown as number,
  fileSize: undefined as unknown as number,
  publishDate: '',
  thumbnail: '',
  tags: [] as string[],
  isFree: true,
  price: 0,
  reportFileUrl: '',
  reportFileName: '',
  reportFileSize: ''
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
});

const tagOptions = ref<string[]>(['AI', '大模型', 'AIGC']);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const reportFileInfo = ref<{
  url: string;
  filename: string;
  size: string;
} | null>(null);

// 监听表单中的文件信息变化，同步到组件
watch(() => [form.reportFileUrl, form.reportFileName, form.reportFileSize], ([url, filename, size]) => {
  if (url && filename && size) {
    reportFileInfo.value = { url, filename, size };
  } else {
    reportFileInfo.value = null;
  }
}, { immediate: true });

function normalize() {
  if (form.isFree) {
    form.price = 0;
  }
}

// 文件上传方法
async function handleImageUpload(options: any) {
  try {
    const { data } = await uploadImage(options.file);
    if (data.code === 200) {
      form.thumbnail = data.data.url;
      ElMessage.success('封面上传成功');
    } else {
      ElMessage.error(data.message || '封面上传失败');
    }
  } catch (error) {
    ElMessage.error('封面上传失败');
  }
}

async function onSubmit() {
  normalize();
  
  // 同步报告文件信息到表单
  if (reportFileInfo.value) {
    form.reportFileUrl = reportFileInfo.value.url;
    form.reportFileName = reportFileInfo.value.filename;
    form.reportFileSize = reportFileInfo.value.size;
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
</style>


