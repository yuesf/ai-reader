<template>
  <div class="report-file-upload">
    
    <el-upload
      class="upload-demo"
      :action="''"
      :http-request="handleReportUpload"
      :show-file-list="false"
      :before-upload="beforeUpload"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx"
      drag
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、Word、Excel、PowerPoint 格式，文件大小不超过 100MB
        </div>
      </template>
    </el-upload>
    
    <div v-if="fileInfo" class="file-result">
      <el-result icon="success" title="上传成功">
        <template #extra>
          <div class="file-meta">
            <span>文件Id: {{ fileInfo.fileId }}</span>
            <span>名称: {{ fileInfo.filename }}</span>
            <span>大小: {{ fileInfo.size }}</span>
            <el-button type="danger" size="small" @click="removeFile">删除</el-button>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { uploadReportFile, deleteFile } from '../api';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';

interface FileInfo {
  fileId: string;
  filename: string;
  size: string;
}

interface Props {
  modelValue?: FileInfo | null;
}

interface Emits {
  (e: 'update:modelValue', value: FileInfo | null): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const fileInfo = ref<FileInfo | null>(props.modelValue || null);

// 监听外部传入的值变化
watch(() => props.modelValue, (newVal) => {
  fileInfo.value = newVal || null;
});

// 监听内部值变化，同步到外部
watch(fileInfo, (newVal) => {
  emit('update:modelValue', newVal);
}, { deep: true });

function beforeUpload(file: File) {
  console.log('beforeUpload 触发，文件信息:', {
    name: file.name,
    size: file.size,
    type: file.type
  });
  
  // 检查文件大小 (100MB = 100 * 1024 * 1024)
  const isLt100M = file.size / 1024 / 1024 < 100;
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB!');
    return false;
  }
  
  // 检查文件类型
  const allowedTypes = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'];
  const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
  if (!allowedTypes.includes(fileExtension)) {
    ElMessage.error('只支持 PDF、Word、Excel、PowerPoint 格式!');
    return false;
  }
  
  return true;
}

async function handleReportUpload(options: any) {
  console.log('handleReportUpload 被调用，options:', options);
  
  try {
    if (!options.file) {
      throw new Error('没有选择文件');
    }
    
    console.log('开始上传文件:', {
      name: options.file.name,
      size: options.file.size,
      type: options.file.type
    });
    
    const { data } = await uploadReportFile(options.file);
    console.log('上传响应:', data);
    
    if (data.code === 200) {
      fileInfo.value = data.data;
      ElMessage.success('报告文件上传成功');
    } else {
      ElMessage.error(data.message || '上传失败');
    }
  } catch (error) {
    console.error('上传错误详情:', error);
    
    if (error.response) {
      console.error('响应状态:', error.response.status);
      console.error('响应数据:', error.response.data);
      ElMessage.error(`上传失败: ${error.response.data?.message || error.response.statusText}`);
    } else if (error.request) {
      console.error('请求错误:', error.request);
      console.error('请求配置:', error.config);
      ElMessage.error('网络错误，请检查网络连接');
    } else {
      console.error('其他错误:', error.message);
      ElMessage.error(`上传失败: ${error.message}`);
    }
  }
}

async function removeFile() {
  if (fileInfo.value?.fileId) {
    try {
      await deleteFile(fileInfo.value.fileId);
      fileInfo.value = null;
      ElMessage.success('文件删除成功');
    } catch (error) {
      ElMessage.error('文件删除失败');
    }
  }
}

// 测试API连接
async function testApi() {
  try {
    console.log('测试API连接...');
    
    // 测试1: 使用fetch测试基础连接
    console.log('测试1: 使用fetch测试基础连接');
    try {
      const response = await fetch('/health');
      const data = await response.json();
      console.log('健康检查响应:', data);
      ElMessage.success('基础连接正常');
    } catch (error) {
      console.error('fetch测试失败:', error);
      ElMessage.warning('基础连接失败');
    }
    
    // 测试2: 使用axios测试（与上传使用相同的配置）
    console.log('测试2: 使用axios测试');
    try {
      const axios = (await import('axios')).default;
      const response = await axios.get('/health');
      console.log('axios健康检查响应:', response.data);
      ElMessage.success('axios连接正常');
    } catch (error) {
      console.error('axios测试失败:', error);
      ElMessage.warning('axios连接失败');
    }
    
    // 测试3: 直接测试后端地址
    console.log('测试3: 直接测试后端地址');
    try {
      const response = await fetch('https://yuesf.cn/health');
      const data = await response.json();
      console.log('直接后端连接响应:', data);
      ElMessage.success('直接后端连接正常');
    } catch (error) {
      console.error('直接后端连接失败:', error);
      ElMessage.error('直接后端连接失败，后端服务可能未启动');
    }
    
  } catch (error) {
    console.error('API连接测试失败:', error);
    ElMessage.error('API连接失败，请检查后端服务');
  }
}
</script>

<style scoped>
.report-file-upload {
  width: 100%;
}
.upload-demo {
  text-align: center;
}
.file-result {
  margin-top: 16px;
}
.file-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
