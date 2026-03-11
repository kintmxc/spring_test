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

    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 style="font-size: 22px">最新订单</h2>
          <p>用于首页快速浏览最近履约数据。</p>
        </div>
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
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { overviewApi, latestOrdersApi } from '../api/dashboard'

const overview = ref({ todayOrderCount: 0, pendingOrderCount: 0, onSaleProductCount: 0, monthSalesAmount: 0 })
const latestOrders = ref([])
const loading = ref(false)

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
  } catch (error) {
    ElMessage.error(error.message || '加载看板失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>