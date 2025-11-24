<template>
  <div class="user-behavior">
    <!-- 页面标题和搜索 -->
    <div class="page-header">
      <h2>用户行为分析</h2>
      <el-button type="primary" @click="refreshData" :loading="loading" size="small">
        刷新数据
      </el-button>
    </div>

    <!-- 搜索表单 -->
    <el-card class="search-card" style="margin-bottom: 20px;">
      <el-form :model="searchForm" label-width="80px" size="default">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="用户ID">
              <el-input
                v-model="searchForm.userId"
                placeholder="请输入用户ID"
                clearable
                @keyup.enter="searchUser"
              />
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
            <el-form-item label="记录限制">
              <el-input-number
                v-model="searchForm.limit"
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
            <el-button type="primary" @click="searchUser" :loading="loading">
              <el-icon><i class="el-icon-search"></i></el-icon>
              搜索
            </el-button>
            <el-button @click="resetSearch" style="margin-left: 10px;">
              重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 用户行为统计 -->
    <el-row :gutter="20" class="stats-section" v-if="userStats">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ userStats.totalEvents || 0 }}</div>
            <div class="stat-label">总事件数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ userStats.uniquePages || 0 }}</div>
            <div class="stat-label">访问页面数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ formatDuration(userStats.totalDuration) }}</div>
            <div class="stat-label">总使用时长</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ userStats.avgEventsPerHour || 0 }}</div>
            <div class="stat-label">平均每小时事件</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 用户路径图表 -->
    <el-row :gutter="20" class="charts-section" v-if="userEvents.length > 0">
      <el-col :span="24">
        <el-card class="chart-card">
          <template #header>
            <span>用户行为时间线</span>
          </template>
          <div ref="timelineChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 页面访问统计 -->
    <el-row :gutter="20" class="charts-section" v-if="pageStats.length > 0">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>页面访问分布</span>
          </template>
          <div ref="pageChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>事件类型分布</span>
          </template>
          <div ref="eventChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 行为事件详情表格 -->
    <el-card class="events-table">
      <template #header>
        <span>行为事件详情 (共 {{ userEvents.length }} 条记录)</span>
      </template>
      <el-table 
        :data="userEvents" 
        style="width: 100%" 
        size="small"
        :height="400"
        v-loading="loading"
        empty-text="暂无数据，请搜索用户ID获取行为数据"
      >
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="scope">
            {{ formatTimestamp(scope.row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="eventType" label="事件类型" width="120">
          <template #default="scope">
            <el-tag :type="getEventTypeColor(scope.row.eventType)">
              {{ scope.row.eventType || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pagePath" label="页面路径" width="200">
          <template #default="scope">
            <el-text truncated>{{ scope.row.pagePath || '未知页面' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="elementText" label="操作元素" width="150">
          <template #default="scope">
            <el-text truncated>{{ scope.row.elementText || '无' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="networkType" label="网络类型" width="100">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.networkType || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备信息" width="150">
          <template #default="scope">
            <el-text truncated>{{ getDeviceInfo(scope.row.deviceInfo) }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button 
              type="text" 
              size="small" 
              @click="viewEventDetails(scope.row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 事件详情对话框 -->
    <el-dialog v-model="showEventDialog" title="事件详情" width="600px">
      <el-descriptions :column="2" border v-if="selectedEvent">
        <el-descriptions-item label="用户ID">{{ selectedEvent.userId }}</el-descriptions-item>
        <el-descriptions-item label="会话ID">{{ selectedEvent.sessionId }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ selectedEvent.eventType }}</el-descriptions-item>
        <el-descriptions-item label="页面路径">{{ selectedEvent.pagePath }}</el-descriptions-item>
        <el-descriptions-item label="元素ID">{{ selectedEvent.elementId || '无' }}</el-descriptions-item>
        <el-descriptions-item label="元素文本">{{ selectedEvent.elementText || '无' }}</el-descriptions-item>
        <el-descriptions-item label="时间戳">{{ selectedEvent.timestamp }}</el-descriptions-item>
        <el-descriptions-item label="网络类型">{{ selectedEvent.networkType || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ selectedEvent.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="设备信息" :span="2">
          <pre style="margin: 0; white-space: pre-wrap;">{{ formatJson(selectedEvent.deviceInfo) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="自定义属性" :span="2">
          <pre style="margin: 0; white-space: pre-wrap;">{{ formatJson(selectedEvent.properties) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import { trackingAPI } from '../../api/tracking';

export default {
  name: 'UserBehavior',
  setup() {
    const loading = ref(false);
    const userEvents = ref([]);
    const userStats = ref(null);
    const pageStats = ref([]);
    const showEventDialog = ref(false);
    const selectedEvent = ref(null);

    const searchForm = reactive({
      userId: '',
      startDate: '',
      endDate: '',
      limit: 100
    });

    const dateRange = ref(null);

    // 图表引用
    const timelineChart = ref(null);
    const pageChart = ref(null);
    const eventChart = ref(null);
    let timelineChartInstance = null;
    let pageChartInstance = null;
    let eventChartInstance = null;

    // 搜索用户行为数据
    const searchUser = async () => {
      if (!searchForm.userId.trim()) {
        ElMessage.warning('请输入用户ID');
        return;
      }
      
      try {
        loading.value = true;
        
        const response = await trackingAPI.getUserPath(
          searchForm.userId,
          searchForm.startDate,
          searchForm.endDate,
          searchForm.limit
        );
        
        if (response.data.code === 200) {
          userEvents.value = response.data.data;
          calculateUserStats();
          renderCharts();
        } else {
          ElMessage.error(response.data.message || '获取用户行为数据失败');
        }
      } catch (error) {
        console.error('获取用户行为数据失败:', error);
        ElMessage.error('获取用户行为数据失败');
      } finally {
        loading.value = false;
      }
    };

    // 计算用户统计信息
    const calculateUserStats = () => {
      if (userEvents.value.length === 0) {
        userStats.value = null;
        pageStats.value = [];
        return;
      }
      
      // 计算统计数据
      const totalEvents = userEvents.value.length;
      const uniquePages = new Set(userEvents.value.map(e => e.pagePath).filter(Boolean)).size;
      
      // 计算时间范围
      const timestamps = userEvents.value.map(e => e.timestamp).filter(Boolean).sort((a, b) => a - b);
      const totalDuration = timestamps.length > 1 ? timestamps[timestamps.length - 1] - timestamps[0] : 0;
      
      // 计算每小时平均事件数
      const hours = totalDuration / (1000 * 60 * 60);
      const avgEventsPerHour = hours > 0 ? Math.round(totalEvents / hours) : totalEvents;
      
      userStats.value = {
        totalEvents,
        uniquePages,
        totalDuration,
        avgEventsPerHour
      };
      
      // 计算页面统计
      const pageCountMap = new Map();
      const eventTypeCountMap = new Map();
      
      userEvents.value.forEach(event => {
        const page = event.pagePath || '未知页面';
        const eventType = event.eventType || '未知事件';
        
        pageCountMap.set(page, (pageCountMap.get(page) || 0) + 1);
        eventTypeCountMap.set(eventType, (eventTypeCountMap.get(eventType) || 0) + 1);
      });
      
      pageStats.value = [
        ...Array.from(pageCountMap.entries()).map(([name, value]) => ({ name, value })),
        ...Array.from(eventTypeCountMap.entries()).map(([name, value]) => ({ name, value }))
      ];
    };

    // 渲染图表
    const renderCharts = () => {
      nextTick(() => {
        renderTimelineChart();
        renderPageChart();
        renderEventChart();
      });
    };

    // 渲染时间线图表
    const renderTimelineChart = () => {
      if (!timelineChart.value || userEvents.value.length === 0) return;
      
      if (!timelineChartInstance) {
        timelineChartInstance = echarts.init(timelineChart.value);
      }
      
      // 按时间排序事件
      const sortedEvents = [...userEvents.value].sort((a, b) => a.timestamp - b.timestamp);
      
      const timelineData = sortedEvents.map(event => ({
        value: [event.timestamp, event.eventType],
        name: event.eventType,
        itemStyle: {
          color: getEventTypeColor(event.eventType, true)
        }
      }));
      
      const option = {
        title: {
          text: '用户行为时间线',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            const event = params.data;
            return `
              时间: ${formatTimestamp(event.value[0])}<br/>
              事件: ${event.name}<br/>
              页面: ${event.pagePath || '未知'}<br/>
              元素: ${event.elementText || '无'}
            `;
          }
        },
        xAxis: {
          type: 'time',
          splitLine: {
            show: true
          }
        },
        yAxis: {
          type: 'category',
          data: [...new Set(sortedEvents.map(e => e.eventType))].filter(Boolean)
        },
        series: [{
          type: 'scatter',
          data: timelineData,
          symbolSize: 8
        }]
      };
      
      timelineChartInstance.setOption(option);
    };

    // 渲染页面统计图表
    const renderPageChart = () => {
      if (!pageChart.value) return;
      
      const pageData = userEvents.value.reduce((acc, event) => {
        const page = event.pagePath || '未知页面';
        acc[page] = (acc[page] || 0) + 1;
        return acc;
      }, {});
      
      const data = Object.entries(pageData).map(([name, value]) => ({ name, value }));
      
      if (!pageChartInstance) {
        pageChartInstance = echarts.init(pageChart.value);
      }
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        series: [{
          name: '页面访问',
          type: 'pie',
          radius: '50%',
          data: data,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      };
      
      pageChartInstance.setOption(option);
    };

    // 渲染事件类型图表
    const renderEventChart = () => {
      if (!eventChart.value) return;
      
      const eventData = userEvents.value.reduce((acc, event) => {
        const eventType = event.eventType || '未知事件';
        acc[eventType] = (acc[eventType] || 0) + 1;
        return acc;
      }, {});
      
      const data = Object.entries(eventData).map(([name, value]) => ({ name, value }));
      
      if (!eventChartInstance) {
        eventChartInstance = echarts.init(eventChart.value);
      }
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
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
          type: 'bar',
          itemStyle: {
            color: '#409EFF'
          }
        }]
      };
      
      eventChartInstance.setOption(option);
    };

    // 工具方法
    const formatTimestamp = (timestamp) => {
      if (!timestamp) return '未知时间';
      return new Date(timestamp).toLocaleString('zh-CN');
    };

    const formatDuration = (milliseconds) => {
      if (!milliseconds) return '0分钟';
      const hours = Math.floor(milliseconds / (1000 * 60 * 60));
      const minutes = Math.floor((milliseconds % (1000 * 60 * 60)) / (1000 * 60));
      if (hours > 0) {
        return `${hours}小时${minutes}分钟`;
      }
      return `${minutes}分钟`;
    };

    const formatJson = (jsonStr) => {
      if (!jsonStr) return '无';
      try {
        const parsed = JSON.parse(jsonStr);
        return JSON.stringify(parsed, null, 2);
      } catch {
        return jsonStr;
      }
    };

    const getDeviceInfo = (deviceInfoStr) => {
      if (!deviceInfoStr) return '未知设备';
      try {
        const deviceInfo = JSON.parse(deviceInfoStr);
        return `${deviceInfo.model || '未知设备'} ${deviceInfo.osVersion || ''}`.trim();
      } catch {
        return deviceInfoStr.length > 20 ? deviceInfoStr.substring(0, 20) + '...' : deviceInfoStr;
      }
    };

    const getEventTypeColor = (eventType, forChart = false) => {
      const colorMap = {
        'page_view': '#409EFF',
        'button_click': '#67C23A',
        'form_submit': '#E6A23C',
        'scroll': '#F56C6C',
        'input_change': '#909399'
      };
      
      const color = colorMap[eventType] || '#909399';
      return forChart ? color : undefined;
    };

    const onDateRangeChange = (dates) => {
      if (dates) {
        searchForm.startDate = dates[0];
        searchForm.endDate = dates[1];
      } else {
        searchForm.startDate = '';
        searchForm.endDate = '';
      }
    };

    const resetSearch = () => {
      searchForm.userId = '';
      searchForm.startDate = '';
      searchForm.endDate = '';
      searchForm.limit = 100;
      dateRange.value = null;
      userEvents.value = [];
      userStats.value = null;
      pageStats.value = [];
    };

    const viewEventDetails = (event) => {
      selectedEvent.value = event;
      showEventDialog.value = true;
    };

    const refreshData = () => {
      if (searchForm.userId) {
        searchUser();
      } else {
        ElMessage.info('请先输入用户ID进行搜索');
      }
    };

    // 处理窗口大小变化
    const handleResize = () => {
      if (timelineChartInstance) timelineChartInstance.resize();
      if (pageChartInstance) pageChartInstance.resize();
      if (eventChartInstance) eventChartInstance.resize();
    };

    onMounted(() => {
      window.addEventListener('resize', handleResize);
    });

    return {
      loading,
      userEvents,
      userStats,
      pageStats,
      showEventDialog,
      selectedEvent,
      searchForm,
      dateRange,
      timelineChart,
      pageChart,
      eventChart,
      searchUser,
      resetSearch,
      viewEventDetails,
      refreshData,
      onDateRangeChange,
      formatTimestamp,
      formatDuration,
      formatJson,
      getDeviceInfo,
      getEventTypeColor
    };
  }
};
</script>

<style scoped>
.user-behavior {
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

.search-card {
  margin-bottom: 20px;
}

.stats-section {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  height: 100%;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.events-table {
  margin-top: 20px;
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
