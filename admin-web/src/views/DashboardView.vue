<template>
  <div>
    <div class="page-header">
      <div>
        <h2>经营看板</h2>
        <p>快速掌握今日订单、待处理任务、商品在售情况和本月销售额。</p>
      </div>
      <el-button type="success" plain :loading="loading" @click="loadData">刷新数据</el-button>
    </div>

    <div class="metrics-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
      </div>
    </div>

    <!-- 销售统计图表 -->
    <div class="chart-grid">
      <div class="page-card chart-card">
        <div class="page-header">
          <h2 style="font-size: 20px">销售趋势</h2>
          <p>最近30天销售额和订单数变化趋势</p>
        </div>
        <div ref="salesTrendChart" class="chart-container"></div>
      </div>

      <div class="page-card chart-card">
        <div class="page-header">
          <h2 style="font-size: 20px">商品销售排行</h2>
          <p>销量最高的10个商品</p>
        </div>
        <div ref="productRankChart" class="chart-container"></div>
      </div>
    </div>

    <!-- 销售统计卡片 -->
    <div class="stats-grid">
      <div class="page-card">
        <div class="page-header">
          <h2 style="font-size: 20px">销售概览</h2>
          <p>本月销售数据统计</p>
        </div>
        <div class="stats-content">
          <div class="stat-item">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥ {{ salesStats.totalSales || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总订单数</div>
            <div class="stat-value">{{ salesStats.totalOrders || 0 }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总商品数</div>
            <div class="stat-value">{{ salesStats.totalQuantity || 0 }}</div>
          </div>
        </div>
      </div>

      <div class="page-card">
        <div class="page-header">
          <h2 style="font-size: 20px">最新订单</h2>
          <p>用于首页快速浏览最近履约数据。</p>
        </div>
        <el-table v-loading="loading" :data="latestOrders" stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="receiverName" label="收货人" min-width="120" />
          <el-table-column prop="payAmount" label="实付金额" min-width="120" />
          <el-table-column prop="orderStatus" label="状态" min-width="100">
            <template #default="scope">{{ orderStatusText(scope.row.orderStatus) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="180" />
          <template #empty>
            <div class="table-empty">
              <el-empty description="暂无最新订单" />
            </div>
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { overviewApi, latestOrdersApi, salesStatsApi } from '../api/dashboard'

const overview = ref({ todayOrderCount: 0, pendingOrderCount: 0, onSaleProductCount: 0, monthSalesAmount: 0 })
const latestOrders = ref([])
const salesStats = ref({})
const loading = ref(false)

// 图表引用
const salesTrendChart = ref(null)
const productRankChart = ref(null)
let salesTrendChartInstance = null
let productRankChartInstance = null

const metrics = computed(() => [
  { label: '今日订单数', value: overview.value.todayOrderCount },
  { label: '待处理订单数', value: overview.value.pendingOrderCount },
  { label: '在售商品数', value: overview.value.onSaleProductCount },
  { label: '本月销售额', value: `¥ ${overview.value.monthSalesAmount}` },
])

function orderStatusText(status) {
  return ['已创建', '已支付', '已发货', '已完成', '已取消'][status] || '未知状态'
}

async function loadData() {
  try {
    loading.value = true
    overview.value = await overviewApi()
    latestOrders.value = await latestOrdersApi(6)
    salesStats.value = await salesStatsApi('month')
    
    // 延迟渲染图表，确保DOM已更新
    setTimeout(() => {
      initCharts()
    }, 100)
  } catch (error) {
    ElMessage.error(error.message || '加载看板失败')
  } finally {
    loading.value = false
  }
}

function initCharts() {
  initSalesTrendChart()
  initProductRankChart()
}

function initSalesTrendChart() {
  if (!salesTrendChart.value) return
  
  if (salesTrendChartInstance) {
    salesTrendChartInstance.dispose()
  }
  
  salesTrendChartInstance = echarts.init(salesTrendChart.value)
  
  // 模拟最近30天数据
  const dates = []
  const salesData = []
  const orderData = []
  
  for (let i = 29; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    dates.push(`${date.getMonth() + 1}/${date.getDate()}`)
    salesData.push(Math.floor(Math.random() * 5000) + 1000)
    orderData.push(Math.floor(Math.random() * 50) + 10)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['销售额', '订单数']
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 45
      }
    },
    yAxis: [
      {
        type: 'value',
        name: '销售额',
        position: 'left'
      },
      {
        type: 'value',
        name: '订单数',
        position: 'right'
      }
    ],
    series: [
      {
        name: '销售额',
        type: 'line',
        data: salesData,
        smooth: true,
        itemStyle: {
          color: '#67C23A'
        }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: orderData,
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
  
  salesTrendChartInstance.setOption(option)
}

function initProductRankChart() {
  if (!productRankChart.value) return
  
  if (productRankChartInstance) {
    productRankChartInstance.dispose()
  }
  
  productRankChartInstance = echarts.init(productRankChart.value)
  
  // 模拟商品销售排行数据
  const productNames = [
    '新鲜苹果', '有机蔬菜', '土鸡蛋', '农家猪肉', '生态大米',
    '新鲜牛奶', '野生蜂蜜', '山茶油', '有机小米', '绿色蔬菜'
  ]
  const sales = []
  
  for (let i = 0; i < 10; i++) {
    sales.push(Math.floor(Math.random() * 1000) + 100)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'value',
      name: '销量'
    },
    yAxis: {
      type: 'category',
      data: productNames,
      axisLabel: {
        interval: 0,
        rotate: 0
      }
    },
    series: [{
      name: '销量',
      type: 'bar',
      data: sales,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#67C23A' },
          { offset: 1, color: '#409EFF' }
        ])
      }
    }]
  }
  
  productRankChartInstance.setOption(option)
}

function handleResize() {
  salesTrendChartInstance?.resize()
  productRankChartInstance?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesTrendChartInstance?.dispose()
  productRankChartInstance?.dispose()
})
</script>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin: 20px 0;
}

.metric-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.metric-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.chart-card {
  height: 400px;
}

.chart-container {
  width: 100%;
  height: 320px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 20px;
  margin: 20px 0;
}

.stats-content {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 20px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.page-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.page-header p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.table-empty {
  padding: 40px 0;
  text-align: center;
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-content {
    grid-template-columns: 1fr;
  }
}
</style>
