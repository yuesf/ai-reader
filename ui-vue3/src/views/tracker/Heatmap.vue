<template>
  <div class="heatmap-page">
    <div class="page-header">
      <h2>页面热力图分析</h2>
      <el-button type="primary" @click="refreshData" :loading="loading" size="small">
        刷新数据
      </el-button>
    </div>

    <el-card class="filter-card" style="margin-bottom: 20px;">
      <el-form :model="filterForm" label-width="100px" size="default">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="页面路径">
              <el-select
                v-model="filterForm.pagePath"
                placeholder="请选择页面"
                clearable
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="page in pageList"
                  :key="page"
                  :label="page"
                  :value="page"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="onDateRangeChange"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据限制">
              <el-input-number
                v-model="filterForm.limit"
                :min="10"
                :max="500"
                :step="10"
                controls-position="right"
                style="width: 100%;"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" style="text-align: center;">
            <el-button type="primary" @click="searchData" :loading="loading">
              查询
            </el-button>
            <el-button @click="resetFilter" style="margin-left: 10px;">
              重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <span>热力图数据</span>
      </template>
      <p>热力图功能开发中...</p>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { trackingAPI } from '../../api/tracking';

export default {
  name: 'Heatmap',
  setup() {
    const loading = ref(false);
    const pageList = ref([
      '/pages/index/index',
      '/pages/reportList/reportList',
      '/pages/reportDetail/reportDetail',
      '/pages/login/login',
      '/pages/pdfPreview/pdfPreview'
    ]);

    const filterForm = reactive({
      pagePath: '',
      startDate: '',
      endDate: '',
      limit: 100
    });

    const dateRange = ref(null);

    const searchData = async () => {
      try {
        loading.value = true;
        const response = await trackingAPI.getHeatmapData(
          filterForm.pagePath || undefined,
          filterForm.startDate || undefined,
          filterForm.endDate || undefined
        );
        if (response.data.code === 200) {
          ElMessage.success('获取热力图数据成功');
        } else {
          ElMessage.error(response.data.message || '获取数据失败');
        }
      } catch (error) {
        console.error('获取热力图数据失败:', error);
        ElMessage.error('获取热力图数据失败');
      } finally {
        loading.value = false;
      }
    };

    const onDateRangeChange = (dates) => {
      if (dates) {
        filterForm.startDate = dates[0];
        filterForm.endDate = dates[1];
      } else {
        filterForm.startDate = '';
        filterForm.endDate = '';
      }
    };

    const resetFilter = () => {
      filterForm.pagePath = '';
      filterForm.startDate = '';
      filterForm.endDate = '';
      filterForm.limit = 100;
      dateRange.value = null;
    };

    const refreshData = () => {
      searchData();
    };

    onMounted(() => {
      // 页面加载时的初始化
    });

    return {
      loading,
      pageList,
      filterForm,
      dateRange,
      searchData,
      onDateRangeChange,
      resetFilter,
      refreshData
    };
  }
};
</script>

<style scoped>
.heatmap-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.filter-card {
  margin-bottom: 20px;
}
</style>
