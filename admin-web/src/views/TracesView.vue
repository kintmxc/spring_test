<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>质量追溯</h2>
        <p>围绕商品维护生产日期、产地说明和检测说明。</p>
      </div>
      <el-button type="success" plain :loading="listLoading" @click="loadTraces">刷新列表</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.productName" placeholder="商品名称" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.traceStatus" placeholder="追溯状态" clearable>
        <el-option :value="1" label="有效" />
        <el-option :value="0" label="停用" />
        <el-option :value="-1" label="未维护" />
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
      <span class="tag-soft">共 {{ total }} 条记录</span>
      <span class="tag-soft">当前页 {{ query.pageNum }}</span>
    </div>

    <el-table v-loading="listLoading" :data="records" stripe>
      <el-table-column label="封面" width="100">
        <template #default="scope">
          <el-image v-if="scope.row.coverImage" :src="scope.row.coverImage" :preview-src-list="[scope.row.coverImage]" fit="cover" preview-teleported style="width: 52px; height: 52px; border-radius: 12px" />
          <span v-else class="muted">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column v-if="$route.path === '/traces'" prop="categoryName" label="分类" min-width="120" />
      <el-table-column prop="farmerName" label="农户" min-width="140" />
      <el-table-column prop="originPlace" label="产地" min-width="160" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column label="追溯状态" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.traceMaintained" :type="scope.row.traceStatus === 1 ? 'success' : 'danger'">{{ scope.row.traceStatus === 1 ? '有效' : '停用' }}</el-tag>
          <el-tag v-else type="info">未维护</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="productionDate" label="生产日期" min-width="140" />
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <el-button link type="primary" :loading="rowActionId === scope.row.productId && rowActionType === 'detail'" @click="openTrace(scope.row)">维护追溯</el-button>
          <el-button v-if="scope.row.traceId" link type="danger" :loading="rowActionId === scope.row.productId && rowActionType === 'disable'" @click="disableTrace(scope.row)">停用</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="table-empty">
          <el-empty description="暂无追溯数据" />
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
      @current-change="(page) => { query.pageNum = page; loadTraces() }"
      @size-change="(size) => { query.pageSize = size; handleSearch() }"
    />

    <el-dialog v-model="dialogVisible" title="维护追溯信息" width="620px">
      <el-form ref="formRef" v-loading="detailLoading" class="dialog-loading" :model="form" :rules="rules" label-position="top">
        <el-form-item label="商品名称"><el-input :model-value="activeProduct?.productName || ''" disabled /></el-form-item>
        <el-form-item label="生产日期" prop="productionDate"><el-date-picker v-model="form.productionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="产地说明" prop="originDesc"><el-input v-model="form.originDesc" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="检测说明" prop="inspectDesc"><el-input v-model="form.inspectDesc" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="状态" prop="traceStatus"><el-select v-model="form.traceStatus" style="width: 100%"><el-option :value="1" label="有效" /><el-option :value="0" label="停用" /></el-select></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitLoading" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { tracePageApi, traceByProductApi, saveTraceApi, disableTraceApi } from '../api/traces'

const records = ref([])
const total = ref(0)
const listLoading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const activeProduct = ref(null)
const formRef = ref(null)
const rowActionId = ref(null)
const rowActionType = ref('')
const query = reactive({ pageNum: 1, pageSize: 10, productName: '', traceStatus: '' })
const form = reactive({ productId: null, productionDate: '', originDesc: '', inspectDesc: '', traceStatus: 1 })
const rules = {
  productionDate: [{ required: true, message: '请选择生产日期', trigger: 'change' }],
  originDesc: [{ required: true, message: '请输入产地说明', trigger: 'blur' }],
  inspectDesc: [{ required: true, message: '请输入检测说明', trigger: 'blur' }],
  traceStatus: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadTraces() {
  try {
    listLoading.value = true
    const data = await tracePageApi(query)
    records.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '加载追溯列表失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadTraces()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = 10
  query.productName = ''
  query.traceStatus = ''
  loadTraces()
}

async function openTrace(product) {
  activeProduct.value = product
  rowActionId.value = product.productId
  rowActionType.value = 'detail'
  form.productId = product.productId
  form.productionDate = ''
  form.originDesc = ''
  form.inspectDesc = ''
  form.traceStatus = 1
  try {
    detailLoading.value = true
    const trace = await traceByProductApi(product.productId)
    if (trace) {
      form.productionDate = trace.productionDate
      form.originDesc = trace.originDesc
      form.inspectDesc = trace.inspectDesc
      form.traceStatus = trace.traceStatus ?? 1
    }
  } catch (error) {
    ElMessage.error(error.message || '加载追溯详情失败')
  } finally {
    detailLoading.value = false
    rowActionId.value = null
    rowActionType.value = ''
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    const trace = await saveTraceApi(form)
    applyTraceMutation(trace, { isCreate: !activeProduct.value?.traceId })
    if (activeProduct.value) {
      activeProduct.value = { ...activeProduct.value, ...trace }
    }
    ElMessage.success('追溯信息已保存')
    dialogVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存追溯失败')
    }
  } finally {
    submitLoading.value = false
  }
}

async function disableTrace(row) {
  try {
    rowActionId.value = row.productId
    rowActionType.value = 'disable'
    const trace = await disableTraceApi(row.traceId)
    applyTraceMutation(trace, { isCreate: false })
    if (activeProduct.value?.productId === trace.productId) {
      activeProduct.value = { ...activeProduct.value, ...trace }
    }
    ElMessage.success('追溯已停用')
  } catch (error) {
    ElMessage.error(error.message || '停用追溯失败')
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

function matchesTraceFilter(row) {
  const keyword = query.productName.trim().toLowerCase()
  if (keyword && !`${row.productName || ''}`.toLowerCase().includes(keyword)) {
    return false
  }
  if (query.traceStatus === '') {
    return true
  }
  if (query.traceStatus === -1) {
    return !row.traceMaintained
  }
  return row.traceMaintained && row.traceStatus === query.traceStatus
}

function applyTraceMutation(row, { isCreate }) {
  const next = [...records.value]
  const index = next.findIndex((item) => item.productId === row.productId)
  const matched = matchesTraceFilter(row)

  if (index >= 0) {
    if (matched) {
      next[index] = row
    } else {
      next.splice(index, 1)
      total.value = Math.max(total.value - 1, 0)
    }
    records.value = next
    return
  }

  if (!isCreate || !matched) {
    return
  }

  total.value += 1
  if (query.pageNum === 1) {
    next.unshift(row)
    if (next.length > query.pageSize) {
      next.pop()
    }
    records.value = next
  }
}

onMounted(loadTraces)
</script>