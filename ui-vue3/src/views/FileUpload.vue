<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件上传测试</span>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card>
            <template #header>上传报告文件</template>
            <el-upload
              class="upload-demo"
              :action="''"
              :http-request="handleReportUpload"
              :show-file-list="false"
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
            <div v-if="reportFile" class="file-result">
              <el-result icon="success" title="上传成功">
                <template #extra>
                  <el-link :href="reportFile.url" target="_blank" type="primary">
                    {{ reportFile.filename }}
                  </el-link>
                  <div class="file-meta">
                    <span>大小: {{ reportFile.size }}</span>
                    <el-button type="danger" size="small" @click="removeReportFile">删除</el-button>
                  </div>
                </template>
              </el-result>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card>
            <template #header>上传图片</template>
            <el-upload
              class="upload-demo"
              :action="''"
              :http-request="handleImageUpload"
              :show-file-list="false"
              accept="image/*"
              drag
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将图片拖到此处，或<em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 JPG、PNG、GIF 格式
                </div>
              </template>
            </el-upload>
            <div v-if="imageFile" class="file-result">
              <el-result icon="success" title="上传成功">
                <template #extra>
                  <el-image 
                    :src="imageFile.url" 
                    style="width: 100px; height: 100px; object-fit: cover;"
                    fit="cover"
                  />
                  <div class="file-meta">
                    <span>{{ imageFile.filename }}</span>
                    <el-button type="danger" size="small" @click="removeImageFile">删除</el-button>
                  </div>
                </template>
              </el-result>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-card style="margin-top: 20px;">
        <template #header>通用文件上传</template>
        <el-form :model="uploadForm" label-width="100px">
          <el-form-item label="存储文件夹">
            <el-select v-model="uploadForm.folder" placeholder="选择文件夹">
              <el-option label="reports" value="reports" />
              <el-option label="images" value="images" />
              <el-option label="documents" value="documents" />
              <el-option label="others" value="others" />
            </el-select>
          </el-form-item>
          <el-form-item label="选择文件">
            <el-upload
              :action="''"
              :http-request="handleGeneralUpload"
              :show-file-list="false"
              accept="*/*"
            >
              <el-button type="primary">选择文件</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
        <div v-if="generalFile" class="file-result">
          <el-result icon="success" title="上传成功">
            <template #extra>
              <div class="file-meta">
                <span>文件Id: {{ generalFile.fileId }}</span>
                <span>名称: {{ generalFile.filename }}</span>
                <span>大小: {{ generalFile.size }}</span>
                <span>文件夹: {{ generalFile.folder }}</span>
                <el-button type="danger" size="small" @click="removeGeneralFile">删除</el-button>
              </div>
            </template>
          </el-result>
        </div>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { uploadReportFile, uploadImage, uploadFile, deleteFile } from '../api';
import { ElMessage } from 'element-plus';
import { UploadFilled } from '@element-plus/icons-vue';

const uploadForm = reactive({
  folder: 'others'
});

const reportFile = ref<any>(null);
const imageFile = ref<any>(null);
const generalFile = ref<any>(null);

async function handleReportUpload(options: any) {
  try {
    const { data } = await uploadReportFile(options.file);
    if (data.code === 200) {
      reportFile.value = data.data;
      ElMessage.success('报告文件上传成功');
    } else {
      ElMessage.error(data.message || '上传失败');
    }
  } catch (error) {
    ElMessage.error('上传失败');
  }
}

async function handleImageUpload(options: any) {
  try {
    const { data } = await uploadImage(options.file);
    if (data.code === 200) {
      imageFile.value = data.data;
      ElMessage.success('图片上传成功');
    } else {
      ElMessage.error(data.message || '上传失败');
    }
  } catch (error) {
    ElMessage.error('上传失败');
  }
}

async function handleGeneralUpload(options: any) {
  try {
    const { data } = await uploadFile(options.file, uploadForm.folder);
    if (data.code === 200) {
      generalFile.value = data.data;
      ElMessage.success('文件上传成功');
    } else {
      ElMessage.error(data.message || '上传失败');
    }
  } catch (error) {
    ElMessage.error('上传失败');
  }
}

async function removeReportFile() {
  if (reportFile.value?.url) {
    try {
      await deleteFile(reportFile.value.url);
      reportFile.value = null;
      ElMessage.success('文件删除成功');
    } catch (error) {
      ElMessage.error('文件删除失败');
    }
  }
}

async function removeImageFile() {
  if (imageFile.value?.url) {
    try {
      await deleteFile(imageFile.value.url);
      imageFile.value = null;
      ElMessage.success('文件删除成功');
    } catch (error) {
      ElMessage.error('文件删除失败');
    }
  }
}

async function removeGeneralFile() {
  if (generalFile.value?.fileId) {
    try {
      await deleteFile(generalFile.value.fileId);
      generalFile.value = null;
      ElMessage.success('文件删除成功');
    } catch (error) {
      ElMessage.error('文件删除失败');
    }
  }
}
</script>

<style scoped>
.page { padding: 16px; }
.card-header { font-weight: bold; }
.file-result { margin-top: 16px; }
.file-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
.upload-demo {
  text-align: center;
}
</style>
