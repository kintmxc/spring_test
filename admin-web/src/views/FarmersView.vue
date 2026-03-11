<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>农户管理</h2>
        <p>查看农户资料、审核状态并维护账号启停。</p>
      </div>
      <el-button type="success" :loading="listLoading" @click="openDialog()">新增农户</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.farmerName" placeholder="农户名称" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.authStatus" placeholder="审核状态" clearable>
        <el-option :value="0" label="待审核" />
        <el-option :value="1" label="已通过" />
        <el-option :value="2" label="已驳回" />
      </el-select>
      <el-select v-model="query.accountStatus" placeholder="账号状态" clearable>
        <el-option :value="1" label="启用" />
        <el-option :value="0" label="禁用" />
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
      <span class="tag-soft">共 {{ total }} 位农户</span>
      <span class="tag-soft">当前页 {{ query.pageNum }}</span>
    </div>

    <el-table v-loading="listLoading" :data="records" stripe>
      <el-table-column prop="farmerName" label="农户名称" min-width="140" />
      <el-table-column prop="loginName" label="登录名" min-width="140" />
      <el-table-column prop="contactPhone" label="联系方式" min-width="140" />
      <el-table-column prop="originPlace" label="产地说明" min-width="180" />
      <el-table-column label="经营概况" min-width="200">
        <template #default="scope">商品 {{ scope.row.productCount }} / 在售 {{ scope.row.onSaleProductCount }} / 订单 {{ scope.row.orderCount }}</template>
      </el-table-column>
      <el-table-column prop="authStatus" label="审核状态" width="120">
        <template #default="scope"><el-tag :type="authTagType(scope.row.authStatus)">{{ scope.row.authStatusText }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="accountStatus" label="账号状态" width="120">
        <template #default="scope"><el-tag :type="scope.row.accountStatus === 1 ? 'success' : 'danger'">{{ scope.row.accountStatusText }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button link type="primary" :loading="rowActionId === scope.row.id && rowActionType === 'edit'" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="success" :loading="rowActionId === scope.row.id && rowActionType === 'audit-pass'" @click="audit(scope.row, 1)">通过</el-button>
          <el-button link type="warning" :loading="rowActionId === scope.row.id && rowActionType === 'audit-reject'" @click="audit(scope.row, 2)">驳回</el-button>
          <el-button link type="danger" :loading="rowActionId === scope.row.id && rowActionType === 'toggle'" @click="toggleStatus(scope.row)">{{ scope.row.accountStatus === 1 ? '禁用' : '启用' }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="table-empty">
          <el-empty description="暂无农户数据" />
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
      @current-change="(page) => { query.pageNum = page; loadFarmers() }"
      @size-change="(size) => { query.pageSize = size; handleSearch() }"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑农户' : '新增农户'" width="560px">
      <el-form ref="formRef" v-loading="detailLoading" class="dialog-loading" :model="form" :rules="rules" label-position="top">
        <el-form-item label="登录名" prop="loginName"><el-input v-model="form.loginName" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="农户名称" prop="farmerName"><el-input v-model="form.farmerName" /></el-form-item>
        <el-form-item label="联系方式" prop="contactPhone"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="产地说明" prop="originPlace"><el-input v-model="form.originPlace" /></el-form-item>
        <el-form-item label="身份证号" prop="idCardNo"><el-input v-model="form.idCardNo" /></el-form-item>
        <el-form-item label="营业执照号" prop="licenseNo"><el-input v-model="form.licenseNo" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
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
import { farmerPageApi, farmerDetailApi, createFarmerApi, updateFarmerApi, auditFarmerApi, updateFarmerStatusApi } from '../api/farmers'

const query = reactive({ pageNum: 1, pageSize: 10, farmerName: '', authStatus: '', accountStatus: '' })
const records = ref([])
const total = ref(0)
const listLoading = ref(false)
const detailLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const rowActionId = ref(null)
const rowActionType = ref('')
const formRef = ref(null)
const form = reactive({ loginName: '', password: '', farmerName: '', contactPhone: '', originPlace: '', idCardNo: '', licenseNo: '', remark: '' })
const rules = {
  loginName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ validator: (_rule, value, callback) => {
    if (!editingId.value && !value) {
      callback(new Error('请输入密码'))
      return
    }
    callback()
  }, trigger: 'blur' }],
  farmerName: [{ required: true, message: '请输入农户名称', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  originPlace: [{ required: true, message: '请输入产地说明', trigger: 'blur' }],
  idCardNo: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  licenseNo: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }],
}

function authText(value) {
  return ['待审核', '已通过', '已驳回'][value] || '未知'
}

function authTagType(value) {
  return ['warning', 'success', 'danger'][value] || 'info'
}

async function loadFarmers() {
  try {
    listLoading.value = true
    const data = await farmerPageApi(query)
    records.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '加载农户失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadFarmers()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = 10
  query.farmerName = ''
  query.authStatus = ''
  query.accountStatus = ''
  loadFarmers()
}

async function openDialog(row) {
  editingId.value = row?.id ?? null
  rowActionId.value = editingId.value
  rowActionType.value = editingId.value ? 'edit' : ''
  form.password = ''
  form.loginName = ''
  form.farmerName = ''
  form.contactPhone = ''
  form.originPlace = ''
  form.idCardNo = ''
  form.licenseNo = ''
  form.remark = ''

  if (editingId.value) {
    try {
      detailLoading.value = true
      const detail = await farmerDetailApi(editingId.value)
      form.loginName = detail.loginName ?? ''
      form.farmerName = detail.farmerName ?? ''
      form.contactPhone = detail.contactPhone ?? ''
      form.originPlace = detail.originPlace ?? ''
      form.idCardNo = detail.idCardNo ?? ''
      form.licenseNo = detail.licenseNo ?? ''
      form.remark = detail.remark ?? ''
    } catch (error) {
      ElMessage.error(error.message || '加载农户详情失败')
      return
    } finally {
      detailLoading.value = false
      rowActionId.value = null
      rowActionType.value = ''
    }
  }

  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    if (editingId.value) {
      const farmer = await updateFarmerApi(editingId.value, form)
      applyFarmerMutation(toFarmerRow(farmer), { isCreate: false })
      ElMessage.success('农户已更新')
    } else {
      const farmer = await createFarmerApi(form)
      applyFarmerMutation(toFarmerRow(farmer), { isCreate: true })
      ElMessage.success('农户已新增')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存农户失败')
    }
  } finally {
    submitLoading.value = false
  }
}

function toFarmerRow(detail) {
  return {
    id: detail.id,
    loginName: detail.loginName,
    farmerName: detail.farmerName,
    contactPhone: detail.contactPhone,
    originPlace: detail.originPlace,
    idCardNo: detail.idCardNo,
    licenseNo: detail.licenseNo,
    authStatus: detail.authStatus,
    accountStatus: detail.accountStatus,
    remark: detail.remark,
    createTime: detail.createTime,
    updateTime: detail.updateTime,
    authStatusText: detail.authStatusText,
    accountStatusText: detail.accountStatusText,
    productCount: detail.productCount ?? 0,
    onSaleProductCount: detail.onSaleProductCount ?? 0,
    orderCount: detail.orderCount ?? 0,
  }
}

function matchesFarmerFilter(row) {
  const keyword = query.farmerName.trim().toLowerCase()
  if (keyword && !`${row.farmerName || ''}`.toLowerCase().includes(keyword)) {
    return false
  }
  if (query.authStatus !== '' && row.authStatus !== query.authStatus) {
    return false
  }
  if (query.accountStatus !== '' && row.accountStatus !== query.accountStatus) {
    return false
  }
  return true
}

function applyFarmerMutation(row, { isCreate }) {
  const next = [...records.value]
  const index = next.findIndex((item) => item.id === row.id)
  const matched = matchesFarmerFilter(row)

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

async function audit(row, authStatus) {
  try {
    rowActionId.value = row.id
    rowActionType.value = authStatus === 1 ? 'audit-pass' : 'audit-reject'
    const farmer = await auditFarmerApi(row.id, { authStatus, remark: authStatus === 1 ? '审核通过' : '审核驳回' })
    applyFarmerMutation(toFarmerRow(farmer), { isCreate: false })
    ElMessage.success('审核状态已更新')
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

async function toggleStatus(row) {
  try {
    rowActionId.value = row.id
    rowActionType.value = 'toggle'
    const farmer = await updateFarmerStatusApi(row.id, row.accountStatus === 1 ? 0 : 1)
    applyFarmerMutation(toFarmerRow(farmer), { isCreate: false })
    ElMessage.success('账号状态已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败')
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

onMounted(loadFarmers)
</script>