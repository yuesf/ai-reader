<template>
  <div class="tracking-dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>埋点监控面板</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateRangeChange"
          style="width: 240px; margin-right: 10px;"
          size="small"
        />
        <el-button type="primary" @click="refreshData" :loading="loading" size="small">
          刷新数据
        </el-button>
      </div>
    </div>

    <!-- 实时数据卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card today-events">
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.todayEvents || 0 }}</div>
            <div class="stat-label">总事件数</div>
          </div>
          <div class="stat-icon">
            <el-icon><i class="el-icon-data-analysis"></i></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card active-users">
          <div class="stat-content">
            <div class="stat-value">{{ dashboardData.activeUsers || 0 }}</div>
            <div class="stat-label">活跃用户数</div>
          </div>
          <div class="stat-icon">
            <el-icon><i class="el-icon-user"></i></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card top-pages">
          <div class="stat-content">
            <div class="stat-value">{{ (dashboardData.topPages || []).length }}</div>
            <div class="stat-label">热门页面</div>
          </div>
          <div class="stat-icon">
            <el-icon><i class="el-icon-document"></i></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card top-events">
          <div class="stat-content">
            <div class="stat-value">{{ (dashboardData.topEvents || []).length }}</div>
            <div class="stat-label">热门事件</div>
          </div>
          <div class="stat-icon">
            <el-icon><i class="el-icon-cpu"></i></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 今日事件趋势图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>事件趋势</span>
          </template>
          <div ref="eventTrendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 活跃用户趋势图 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>用户活跃趋势</span>
          </template>
          <div ref="activeUserChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据列表 -->
    <el-row :gutter="20" class="tables-section">
      <!-- 热门页面列表 -->
      <el-col :span="12">
        <el-card class="table-card">
          <template #header>
            <span>热门页面 TOP 10</span>
          </template>
          <el-table :data="dashboardData.topPages || []" style="width: 100%" size="small">
            <el-table-column prop="page_path" label="页面路径" width="200">
              <template #default="scope">
                <el-text truncated>{{ scope.row.page_path || scope.row.name || '未知页面' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="访问次数" width="100">
              <template #default="scope">
                <el-tag type="primary">{{ scope.row.count || scope.row.value || 0 }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 热门事件列表 -->
      <el-col :span="12">
        <el-card class="table-card">
          <template #header>
            <span>热门事件 TOP 10</span>
          </template>
          <el-table :data="dashboardData.topEvents || []" style="width: 100%" size="small">
            <el-table-column prop="event_type" label="事件类型" width="150">
              <template #default="scope">
                <el-text truncated>{{ scope.row.event_type || scope.row.name || '未知事件' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="触发次数" width="100">
              <template #default="scope">
                <el-tag type="success">{{ scope.row.count || scope.row.value || 0 }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import { trackingAPI } from '../../api/tracking';

export default {
  name: 'TrackerDashboard',
  setup() {
    const loading = ref(false);
    const dashboardData = ref({
      todayEvents: 0,
      activeUsers: 0,
      topPages: [],
      topEvents: []
    });
    const dateRange = ref(null);

    const eventTrendChart = ref(null);
    const activeUserChart = ref(null);
    let eventChart = null;
    let userChart = null;

    // 获取监控面板数据
    const fetchDashboardData = async () => {
      try {
        loading.value = true;
        
        // 获取基础监控数据
        const dashboardResponse = await trackingAPI.getDashboard();
        if (dashboardResponse.data.code === 200) {
          dashboardData.value = dashboardResponse.data.data;
        }

        // 获取实时活跃用户数
        const userCountResponse = await trackingAPI.getActiveUserCount(30);
        if (userCountResponse.data.code === 200) {
          dashboardData.value.activeUsers = userCountResponse.data.data;
        }

        // 初始化图表
        await nextTick();
        initCharts();
        
      } catch (error) {
        console.error('获取监控数据失败:', error);
        ElMessage.error('获取监控数据失败');
      } finally {
        loading.value = false;
      }
    };

    // 初始化图表
    const initCharts = () => {
      // 事件趋势图
      if (eventTrendChart.value) {
        if (!eventChart) {
          eventChart = echarts.init(eventTrendChart.value);
        }
        
        const option = {
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: generateTimeLabels()
          },
          yAxis: {
            type: 'value'
          },
          series: [{
            data: generateMockTrendData(24, dashboardData.value.todayEvents || 0),
            type: 'line',
            smooth: true,
            name: '事件数',
            areaStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: 'rgba(64, 158, 255, 0.8)' },
                  { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
                ]
              }
            }
          }]
        };
        
        eventChart.setOption(option);
      }

      // 活跃用户趋势图
      if (activeUserChart.value) {
        if (!userChart) {
          userChart = echarts.init(activeUserChart.value);
        }
        
        const option = {
          tooltip: {
            trigger: 'axis'
          },
          xAxis: {
            type: 'category',
            data: generateTimeLabels()
          },
          yAxis: {
            type: 'value'
          },
          series: [{
            data: generateMockTrendData(24, dashboardData.value.activeUsers || 0),
            type: 'bar',
            name: '用户数',
            itemStyle: {
              color: {
                type: 'linear',
                x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                  { offset: 0, color: '#67C23A' },
                  { offset: 1, color: '#67C23A80' }
                ]
              }
            }
          }]
        };
        
        userChart.setOption(option);
      }
    };

    // 生成时间标签（最近24小时）
    const generateTimeLabels = () => {
      const labels = [];
      const now = new Date();
      for (let i = 23; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 60 * 60 * 1000);
        labels.push(time.getHours() + ':00');
      }
      return labels;
    };

    // 生成模拟趋势数据
    const generateMockTrendData = (hours, baseValue) => {
      const data = [];
      for (let i = 0; i < hours; i++) {
        // 模拟真实的使用模式：工作时间较高，夜间较低
        const hour = new Date().getHours();
        const isWorkingTime = hour >= 9 && hour <= 18;
        const variance = isWorkingTime ? baseValue * 0.5 : baseValue * 0.2;
        const value = Math.max(0, Math.round(baseValue + (Math.random() - 0.5) * variance));
        data.push(value);
      }
      return data;
    };

    // 日期范围变化
    const onDateRangeChange = (dates) => {
      // 重新获取数据
      fetchDashboardData();
    };

    // 刷新数据
    const refreshData = () => {
      fetchDashboardData();
      ElMessage.success('数据已刷新');
    };

    // 页面尺寸变化时重新调整图表
    const handleResize = () => {
      if (eventChart) eventChart.resize();
      if (userChart) userChart.resize();
    };

    onMounted(() => {
      fetchDashboardData();
      window.addEventListener('resize', handleResize);
    });

    return {
      loading,
      dashboardData,
      dateRange,
      eventTrendChart,
      activeUserChart,
      refreshData,
      onDateRangeChange
    };
  }
};
</script>

<style scoped>
.tracking-dashboard {
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
}

.stat-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 40px;
  color: #E4E7ED;
}

.today-events {
  border-left: 4px solid #409EFF;
}

.active-users {
  border-left: 4px solid #67C23A;
}

.top-pages {
  border-left: 4px solid #E6A23C;
}

.top-events {
  border-left: 4px solid #F56C6C;
}

.charts-section,
.tables-section {
  margin-bottom: 20px;
}

.chart-card,
.table-card {
  height: 100%;
}

.chart-container {
  height: 300px;
  width: 100%;
}

:deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom: 1px solid #EBEEF5;
  font-weight: 600;
  color: #303133;
}

:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table .cell) {
  padding: 0 8px;
}
</style>
