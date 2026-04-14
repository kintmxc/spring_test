<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>订单管理</h2>
        <p>查看订单详情、执行发货、完成或取消等履约操作。</p>
      </div>
      <el-button type="success" plain :loading="listLoading" @click="loadOrders">刷新列表</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.orderNo" placeholder="订单号" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable>
        <el-option :value="0" label="已创建" />
        <el-option :value="1" label="已支付" />
        <el-option :value="2" label="已发货" />
        <el-option :value="3" label="已完成" />
        <el-option :value="4" label="已取消" />
      </el-select>
      <el-select v-model="query.pageSize" placeholder="每页条数" style="width: 140px" @change="handleSearch">
        <el-option :value="10" label="10 条/页" />
        <el-option :value="20" label="20 条/页" />
        <el-option :value="50" label="50 条/页" />
      </el-select>
      <el-button type="success" plain @click="handleSearch">查询</el-button>
      <el-button plain @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar" style="margin-top: -6px">
      <span class="tag-soft">共 {{ total }} 个订单</span>
      <span class="tag-soft">当前页 {{ query.pageNum }}</span>
      <span class="tag-soft">{{ authState.user?.roleCode === 'ADMIN' ? '管理员视角' : '农户视角' }}</span>
    </div>

    <el-table v-loading="listLoading" :data="records" stripe>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column v-if="authState.user?.roleCode === 'ADMIN'" prop="farmerName" label="农户" min-width="140" />
      <el-table-column prop="itemSummary" label="商品摘要" min-width="220" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="件数" width="90" />
      <el-table-column prop="receiverName" label="收货人" min-width="120" />
      <el-table-column prop="receiverPhone" label="联系方式" min-width="130" />
      <el-table-column prop="payAmount" label="实付金额" min-width="120" />
      <el-table-column prop="orderStatus" label="状态" min-width="100">
        <template #default="scope">
          <el-tag :type="orderTagType(scope.row.orderStatus)">{{ orderStatusText(scope.row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="物流" min-width="220" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.logisticsCompany ? `${scope.row.logisticsCompany} / ${scope.row.trackingNo}` : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="scope">
          <el-button link type="primary" :loading="rowActionId === scope.row.id && rowActionType === 'detail'" @click="showDetail(scope.row.id)">详情</el-button>
          <el-button v-if="scope.row.orderStatus === 1" link type="success" @click="openShip(scope.row)">发货</el-button>
          <el-button v-if="scope.row.orderStatus === 2" link type="warning" :loading="rowActionId === scope.row.id && rowActionType === 'complete'" @click="complete(scope.row)">完成</el-button>
          <el-button v-if="scope.row.orderStatus < 2" link type="danger" :loading="rowActionId === scope.row.id && rowActionType === 'cancel'" @click="cancel(scope.row)">取消</el-button>
          <el-button v-if="scope.row.orderStatus === 4" link type="danger" :loading="rowActionId === scope.row.id && rowActionType === 'delete'" @click="deleteOrder(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="table-empty">
          <el-empty description="暂无订单数据" />
        </div>
      </template>
    </el-table>

    <el-pagination
      style="margin-top: 18px; justify-content: flex-end"
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.pageNum"
      :page-sizes="[10, 20, 50]"
      @current-change="(page) => { query.pageNum = page; loadOrders() }"
      @size-change="(size) => { query.pageSize = size; handleSearch() }"
    />

    <el-dialog v-model="detailVisible" title="订单详情" width="820px">
      <div v-loading="detailLoading" class="drawer-grid dialog-loading" v-if="detail">
        <div><strong>订单号：</strong>{{ detail.orderNo }}</div>
        <div v-if="authState.user?.roleCode === 'ADMIN'"><strong>所属农户：</strong>{{ detail.farmerName || '-' }}</div>
        <div><strong>收货人：</strong>{{ detail.receiverName }}</div>
        <div><strong>联系电话：</strong>{{ detail.receiverPhone }}</div>
        <div><strong>地址：</strong>{{ detail.receiverAddress }}</div>
        <div><strong>状态：</strong>{{ detail.orderStatusText }}</div>
        <div><strong>实付金额：</strong>¥ {{ detail.payAmount }}</div>
        <div><strong>订单备注：</strong>{{ detail.remark || '-' }}</div>
        <div><strong>物流信息：</strong>{{ detail.logisticsCompany ? `${detail.logisticsCompany} / ${detail.trackingNo}` : '-' }}</div>
        <div><strong>商品明细：</strong></div>
        <el-table :data="detail.items" stripe>
          <el-table-column prop="productName" label="商品" min-width="180" />
          <el-table-column prop="productPrice" label="单价" width="120" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="subtotalAmount" label="小计" width="120" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="480px">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-position="top">
        <el-form-item label="物流公司" prop="companyName"><el-input v-model="shipForm.companyName" /></el-form-item>
        <el-form-item label="物流单号" prop="trackingNo"><el-input v-model="shipForm.trackingNo" /></el-form-item>
        <el-form-item label="发货备注"><el-input v-model="shipForm.shipRemark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="success" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderPageApi, orderDetailApi, updateOrderStatusApi, shipOrderApi, cancelOrderApi, deleteOrderApi } from '../api/orders'
import { authState } from '../stores/auth'

const query = reactive({ pageNum: 1, pageSize: 10, orderNo: '', orderStatus: '' })
const records = ref([])
const total = ref(0)
const listLoading = ref(false)
const detailLoading = ref(false)
const shipSubmitting = ref(false)
const detail = ref(null)
const detailVisible = ref(false)
const shipVisible = ref(false)
const activeOrderId = ref(null)
const rowActionId = ref(null)
const rowActionType = ref('')
const shipFormRef = ref(null)
const shipForm = reactive({ companyName: '', trackingNo: '', shipRemark: '' })
const shipRules = {
  companyName: [{ required: true, message: '请输入物流公司', trigger: 'blur' }],
  trackingNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }],
}

function orderStatusText(status) {
  return ['已创建', '已支付', '已发货', '已完成', '已取消'][status] || '未知状态'
}

function orderTagType(status) {
  return ['info', 'warning', 'primary', 'success', 'danger'][status] || 'info'
}

async function loadOrders() {
  try {
    listLoading.value = true
    const data = await orderPageApi(query)
    records.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '加载订单失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadOrders()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = 10
  query.orderNo = ''
  query.orderStatus = ''
  loadOrders()
}

async function showDetail(id) {
  try {
    rowActionId.value = id
    rowActionType.value = 'detail'
    detailLoading.value = true
    detail.value = await orderDetailApi(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载订单失败')
  } finally {
    detailLoading.value = false
    rowActionId.value = null
    rowActionType.value = ''
  }
}

function openShip(row) {
  activeOrderId.value = row.id
  shipForm.companyName = ''
  shipForm.trackingNo = ''
  shipForm.shipRemark = ''
  shipVisible.value = true
  shipFormRef.value?.clearValidate()
}

async function submitShip() {
  try {
    await shipFormRef.value.validate()
    shipSubmitting.value = true
    const order = await shipOrderApi(activeOrderId.value, shipForm)
    applyOrderMutation(toOrderRow(order))
    if (detail.value?.id === order.id) {
      detail.value = order
    }
    ElMessage.success('订单已发货')
    shipVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '发货失败')
    }
  } finally {
    shipSubmitting.value = false
  }
}

async function complete(row) {
  try {
    rowActionId.value = row.id
    rowActionType.value = 'complete'
    const order = await updateOrderStatusApi(row.id, { targetStatus: 3, remark: '前端手动完成订单' })
    applyOrderMutation(toOrderRow(order))
    if (detail.value?.id === order.id) {
      detail.value = order
    }
    ElMessage.success('订单已完成')
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败')
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm('确认取消该订单吗？', '取消提示', { type: 'warning' })
    rowActionId.value = row.id
    rowActionType.value = 'cancel'
    const order = await cancelOrderApi(row.id, '前端手动取消订单')
    applyOrderMutation(toOrderRow(order))
    if (detail.value?.id === order.id) {
      detail.value = order
    }
    ElMessage.success('订单已取消')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消订单失败')
    }
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

async function deleteOrder(row) {
  try {
    await ElMessageBox.confirm('确认删除该订单吗？删除后无法恢复！', '删除提示', { type: 'warning' })
    rowActionId.value = row.id
    rowActionType.value = 'delete'
    await deleteOrderApi(row.id)
    const index = records.value.findIndex((item) => item.id === row.id)
    if (index >= 0) {
      records.value.splice(index, 1)
      total.value = Math.max(total.value - 1, 0)
    }
    if (detail.value?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    ElMessage.success('订单已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除订单失败')
    }
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

function toOrderRow(detail) {
  const items = Array.isArray(detail.items) ? detail.items : []
  const itemCount = items.reduce((totalCount, item) => totalCount + (item.quantity ?? 0), 0)
  const itemSummary = items.length
    ? items.map((item) => `${item.productName} x${item.quantity}`).join('，')
    : '-'

  return {
    id: detail.id,
    orderNo: detail.orderNo,
    userId: detail.userId,
    farmerId: detail.farmerId,
    totalAmount: detail.totalAmount,
    payAmount: detail.payAmount,
    orderStatus: detail.orderStatus,
    payStatus: detail.payStatus,
    receiverName: detail.receiverName,
    receiverPhone: detail.receiverPhone,
    receiverAddress: detail.receiverAddress,
    remark: detail.remark,
    payTime: detail.payTime,
    shipTime: detail.shipTime,
    finishTime: detail.finishTime,
    cancelTime: detail.cancelTime,
    createTime: detail.createTime,
    updateTime: detail.updateTime,
    farmerName: detail.farmerName,
    itemCount,
    itemSummary,
    logisticsCompany: detail.logisticsCompany,
    trackingNo: detail.trackingNo,
  }
}

function matchesOrderFilter(row) {
  const keyword = query.orderNo.trim().toLowerCase()
  if (keyword && !`${row.orderNo || ''}`.toLowerCase().includes(keyword)) {
    return false
  }
  if (query.orderStatus !== '' && row.orderStatus !== query.orderStatus) {
    return false
  }
  return true
}

function applyOrderMutation(row) {
  const next = [...records.value]
  const index = next.findIndex((item) => item.id === row.id)
  if (index < 0) {
    return
  }

  if (matchesOrderFilter(row)) {
    next[index] = row
  } else {
    next.splice(index, 1)
    total.value = Math.max(total.value - 1, 0)
  }
  records.value = next
}

onMounted(loadOrders)
</script>