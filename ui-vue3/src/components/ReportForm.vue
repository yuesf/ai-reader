<template>
  <div class="report-form">
    <!-- 报告文件 -->
    <div class="form-group">
      <label for="report-file">报告文件</label>
      <input type="file" id="report-file" @change="handleFileChange" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx" />
      <p v-if="fileName">{{ fileName }}</p>
    </div>

    <!-- 标题 -->
    <div class="form-group">
      <label for="title">* 标题</label>
      <input type="text" id="title" v-model="title" :disabled="!!fileName" />
    </div>

    <!-- 文件大小 -->
    <div class="form-group">
      <label for="file-size">文件大小</label>
      <input type="text" id="file-size" v-model="fileSize" disabled />
    </div>

    <!-- 摘要 -->
    <div class="form-group">
      <label for="abstract">摘要</label>
      <textarea id="abstract"></textarea>
    </div>

    <!-- 来源 -->
    <div class="form-group">
      <label for="source">来源</label>
      <input type="text" id="source" />
    </div>

    <!-- 分类 -->
    <div class="form-group">
      <label for="category">分类</label>
      <input type="text" id="category" placeholder="选择分类" />
    </div>

    <!-- 页数 -->
    <div class="form-group">
      <label for="page-count">页数</label>
      <button @click="decreasePageCount">-</button>
      <input type="number" id="page-count" v-model="pageCount" />
      <button @click="increasePageCount">+</button>
    </div>

    <!-- 发布日期 -->
    <div class="form-group">
      <label for="publish-date">发布日期</label>
      <input type="date" id="publish-date" v-model="publishDate" />
    </div>

    <!-- 封面 -->
    <div class="form-group">
      <label for="cover-url">封面</label>
      <input type="text" id="cover-url" placeholder="图片URL" />
      <button>上传封面</button>
    </div>

    <!-- 标签 -->
    <div class="form-group">
      <label for="tags">标签</label>
      <input type="text" id="tags" placeholder="输入或选择标签" />
    </div>

    <!-- 是否免费 -->
    <div class="form-group">
      <label for="is-free">是否免费</label>
      <input type="checkbox" id="is-free" v-model="isFree" />
    </div>

    <!-- 按钮 -->
    <div class="form-group">
      <button @click="save">保存</button>
      <button @click="goBack">返回</button>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      title: '',
      fileSize: '',
      fileName: '',
      pageCount: 0,
      publishDate: new Date().toISOString().split('T')[0],
      isFree: false,
    };
  },
  methods: {
    handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        this.fileName = file.name;
        this.title = file.name;
        this.fileSize = `${(file.size / 1024 / 1024).toFixed(2)} MB`;
      }
    },
    decreasePageCount() {
      if (this.pageCount > 0) {
        this.pageCount--;
      }
    },
    increasePageCount() {
      this.pageCount++;
    },
    save() {
      // 保存逻辑
    },
    goBack() {
      // 返回逻辑
    },
  },
};
</script>

<style scoped>
.report-form {
  padding: 20px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 5px;
}

input[type="text"],
input[type="number"],
input[type="date"],
textarea {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}

button {
  padding: 8px 15px;
  cursor: pointer;
}
</style>