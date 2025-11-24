<template>
  <div class="statistics-page">
    <div class="page-header">
      <h2>埋点数据统计报表</h2>
      <el-button type="primary" @click="refreshData" :loading="loading" size="small">
        刷新数据
      </el-button>
    </div>

    <el-card class="filter-card" style="margin-bottom: 20px;">
      <el-form :model="filterForm" label-width="100px" size="default">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="统计维度">
              <el-select
                v-model="filterForm.groupBy"
                placeholder="请选择统计维度"
                style="width: 100%;"
              >
                <el-option label="按日期" value="date" />
                <el-option label="按页面" value="page" />
                <el-option label="按事件类型" value="eventType" />
                <el-option label="按小时" value="hour" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
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
          <el-col :span="6">
            <el-form-item label="查看模式">
              <el-radio-group v-model="filterForm.viewMode" size="small">
                <el-radio-button label="chart">图表</el-radio-button>
                <el-radio-button label="table">表格</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="数据限制">
              <el-input-number
                v-model="filterForm.limit"
                :min="10"
                :max="1000"
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
            <el-button @click="exportData" style="margin-left: 10px;" :disabled="statisticsData.length === 0">
              导出数据
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-row :gutter="20" v-if="statisticsData.length > 0">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>统计图表</span>
          </template>
          <div ref="statisticsChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>数据列表</span>
          </template>
          <el-table
            :data="statisticsData"
            style="width: 100%"
            size="small"
            :height="400"
            v-loading="loading"
          >
            <el-table-column :label="getGroupByLabel()" prop="name" width="200" />
            <el-table-column label="数据量" prop="value" />
            <el-table-column label="占比" width="100">
              <template #default="scope">
                {{ ((scope.row.value / totalCount) * 100).toFixed(1) }}%
              </template>
            </el-table-column>
            <el-table-column label="趋势" width="80">
              <template #default="scope">
                <el-tag :type="getTrendType(scope.row)" size="small">
                  {{ getTrendIcon(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-else>
      <template #header>
        <span>统计报表</span>
      </template>
      <div style="text-align: center; padding: 40px; color: #909399;">
        暂无统计数据，请选择条件进行查询
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick, computed } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import { trackingAPI } from '../../api/tracking';

export default {
  name: 'Statistics',
  setup() {
    const loading = ref(false);
    const statisticsData = ref([]);

    const filterForm = reactive({
      groupBy: 'date',
      startDate: '',
      endDate: '',
      viewMode: 'chart',
      limit: 100
    });

    const dateRange = ref(null);

    // 图表引用
    const statisticsChart = ref(null);
    let chartInstance = null;

    const totalCount = computed(() => {
      return statisticsData.value.reduce((sum, item) => sum + item.value, 0);
    });

    const searchData = async () => {
      try {
        loading.value = true;
        const response = await trackingAPI.getStatistics(
          filterForm.groupBy,
          filterForm.startDate || undefined,
          filterForm.endDate || undefined
        );
        if (response.data.code === 200) {
          const data = response.data.data || [];
          statisticsData.value = data;
          renderChart();
          ElMessage.success('获取统计数据成功');
        } else {
          ElMessage.error(response.data.message || '获取统计数据失败');
        }
      } catch (error) {
        console.error('获取统计数据失败:', error);
        ElMessage.error('获取统计数据失败');
      } finally {
        loading.value = false;
      }
    };

    const renderChart = () => {
      nextTick(() => {
        if (!statisticsChart.value || statisticsData.value.length === 0) return;
        
        if (!chartInstance) {
          chartInstance = echarts.init(statisticsChart.value);
        }
        
        const option = getChartOption();
        chartInstance.setOption(option);
      });
    };

    const getChartOption = () => {
      const data = statisticsData.value.slice(0, 20); // 只显示前20条数据
      
      if (filterForm.groupBy === 'date') {
        return {
          title: {
            text: '按日期统计',
            left: 'center'
          },
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: data.map(item => item.name)
          },
          yAxis: {
            type: 'value'
          },
          series: [{
            data: data.map(item => item.value),
            type: 'line',
            smooth: true,
            name: '数据量'
          }]
        };
      } else {
        return {
          title: {
            text: getGroupByLabel(),
            left: 'center'
          },
          tooltip: {
            trigger: 'item',
            formatter: '{a} <br/>{b}: {c} ({d}%)'
          },
          series: [{
            name: '数据分布',
            type: 'pie',
            radius: '50%',
            data: data.map(item => ({
              name: item.name,
              value: item.value
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        };
      }
    };

    const getGroupByLabel = () => {
      const labels = {
        'date': '日期',
        'page': '页面',
        'eventType': '事件类型',
        'hour': '小时'
      };
      return labels[filterForm.groupBy] || '未知';
    };

    const getTrendType = (item) => {
      // 这里可以基于历史数据判断趋势
      return 'info';
    };

    const getTrendIcon = (item) => {
      // 这里可以基于历史数据判断趋势
      return '→';
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
      filterForm.groupBy = 'date';
      filterForm.startDate = '';
      filterForm.endDate = '';
      filterForm.viewMode = 'chart';
      filterForm.limit = 100;
      dateRange.value = null;
      statisticsData.value = [];
    };

    const exportData = () => {
      if (statisticsData.value.length === 0) {
        ElMessage.warning('没有可导出的数据');
        return;
      }
      
      // 生成CSV数据
      const headers = ['名称', '数据量', '占比'];
      const rows = statisticsData.value.map(item => [
        item.name,
        item.value,
        `${((item.value / totalCount.value) * 100).toFixed(1)}%`
      ]);
      
      const csvContent = [headers, ...rows]
        .map(row => row.join(','))
        .join('\n');
      
      // 下载文件
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `埋点统计数据_${filterForm.groupBy}_${new Date().toISOString().split('T')[0]}.csv`;
      link.click();
      
      ElMessage.success('数据导出成功');
    };

    const refreshData = () => {
      searchData();
    };

    // 处理窗口大小变化
    const handleResize = () => {
      if (chartInstance) chartInstance.resize();
    };

    onMounted(() => {
      searchData();
      window.addEventListener('resize', handleResize);
    });

      return {
        loading,
        statisticsData,
        filterForm,
        dateRange,
        statisticsChart,
        totalCount,
        searchData,
        resetFilter,
        exportData,
        refreshData,
        onDateRangeChange,
        getGroupByLabel,
        getTrendType,
        getTrendIcon
      };
  }
};
</script>

<style scoped>
.statistics-page {
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

.chart-container {
  height: 400px;
  width: 100%;
}

:deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom: 1px solid #EBEEF5;
  font-weight: 600;
  color: #303133;
}
</style>
