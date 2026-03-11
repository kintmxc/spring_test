<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>农产品管理</h2>
        <p>管理商品信息、上下架状态、库存调整和详情查看。</p>
      </div>
      <el-button type="success" :loading="listLoading" @click="openForm()">新增商品</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="query.productName" placeholder="商品名称" clearable @keyup.enter="handleSearch" />
      <el-input v-model="query.originPlace" placeholder="产地关键词" clearable @keyup.enter="handleSearch" />
      <el-select v-model="query.categoryId" placeholder="商品分类" clearable>
        <el-option v-for="item in categoryOptions" :key="item.value" :value="item.value" :label="item.label" />
      </el-select>
      <el-select v-if="authState.user?.roleCode === 'ADMIN'" v-model="query.farmerId" placeholder="所属农户" clearable>
        <el-option v-for="item in farmerOptions" :key="item.value" :value="item.value" :label="item.label" />
      </el-select>
      <el-select v-model="query.saleStatus" placeholder="上架状态" clearable>
        <el-option :value="1" label="上架" />
        <el-option :value="0" label="下架" />
      </el-select>
      <el-select v-model="query.stockStatus" placeholder="库存状态" clearable>
        <el-option :value="2" label="库存充足" />
        <el-option :value="1" label="库存偏低" />
        <el-option :value="0" label="已售罄" />
      </el-select>
      <el-select v-model="query.hasCoverImage" placeholder="封面情况" clearable>
        <el-option :value="1" label="有封面" />
        <el-option :value="0" label="无封面" />
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
      <span class="tag-soft">共 {{ total }} 个商品</span>
      <span class="tag-soft">当前页 {{ query.pageNum }}</span>
      <span class="tag-soft">{{ authState.user?.roleCode === 'ADMIN' ? '管理员视角' : '农户视角' }}</span>
    </div>

    <el-table v-loading="listLoading" :data="records" stripe>
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column label="封面" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.coverImage"
            :src="scope.row.coverImage"
            :preview-src-list="[scope.row.coverImage]"
            fit="cover"
            preview-teleported
            style="width: 52px; height: 52px; border-radius: 12px"
          />
          <span v-else class="muted">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" min-width="120" />
      <el-table-column v-if="authState.user?.roleCode === 'ADMIN'" prop="farmerName" label="农户" min-width="140" />
      <el-table-column prop="originPlace" label="产地" min-width="160" show-overflow-tooltip />
      <el-table-column prop="price" label="价格" min-width="100" />
      <el-table-column prop="stock" label="库存" min-width="120">
        <template #default="scope">
          <el-tag :type="stockTagType(scope.row.stock)">{{ stockLabel(scope.row.stock) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="saleStatus" label="状态" min-width="100">
        <template #default="scope">
          <el-tag :type="scope.row.saleStatus === 1 ? 'success' : 'info'">{{ scope.row.saleStatus === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="scope">
          <el-button link type="primary" :loading="rowActionId === scope.row.id && rowActionType === 'detail'" @click="showDetail(scope.row.id)">详情</el-button>
          <el-button link type="success" @click="openForm(scope.row)">编辑</el-button>
          <el-button link type="info" @click="openStockDialog(scope.row)">调库存</el-button>
          <el-button link type="warning" :loading="rowActionId === scope.row.id && rowActionType === 'status'" @click="toggleStatus(scope.row)">{{ scope.row.saleStatus === 1 ? '下架' : '上架' }}</el-button>
          <el-button link type="danger" :loading="rowActionId === scope.row.id && rowActionType === 'delete'" @click="removeProduct(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="table-empty">
          <el-empty description="暂无商品数据" />
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
      @current-change="(page) => { query.pageNum = page; loadProducts() }"
      @size-change="(size) => { query.pageSize = size; handleSearch() }"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="720px">
      <el-form ref="productFormRef" :model="form" :rules="formRules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="商品名称" prop="productName"><el-input v-model="form.productName" maxlength="30" show-word-limit /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分类" prop="categoryId"><el-select v-model="form.categoryId" style="width: 100%"><el-option v-for="item in categoryOptions" :key="item.value" :value="item.value" :label="item.label" /></el-select></el-form-item></el-col>
          <el-col :span="12" v-if="authState.user?.roleCode === 'ADMIN'"><el-form-item label="所属农户" prop="farmerId"><el-select v-model="form.farmerId" style="width: 100%"><el-option v-for="item in farmerOptions" :key="item.value" :value="item.value" :label="item.label" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位" prop="unitName"><el-input v-model="form.unitName" maxlength="10" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="上架状态" prop="saleStatus"><el-select v-model="form.saleStatus" style="width: 100%"><el-option :value="1" label="上架" /><el-option :value="0" label="下架" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="产地" prop="originPlace"><el-input v-model="form.originPlace" maxlength="60" show-word-limit /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="封面图片">
              <el-upload
                accept="image/png,image/jpeg,image/webp"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleCoverChange"
              >
                <el-button plain>上传封面</el-button>
              </el-upload>
              <div class="muted" style="margin-top: 8px">支持 JPG、PNG、WebP，建议 1MB 以内。</div>
              <div v-if="form.coverImage" style="display: flex; align-items: center; gap: 14px; margin-top: 12px">
                <el-image
                  :src="form.coverImage"
                  :preview-src-list="[form.coverImage]"
                  fit="cover"
                  preview-teleported
                  style="width: 96px; height: 96px; border-radius: 16px"
                />
                <el-button text type="danger" @click="form.coverImage = ''">移除封面</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="商品描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="300" show-word-limit /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitLoading" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="商品详情" width="720px">
      <div v-loading="detailLoading" class="drawer-grid dialog-loading" v-if="detail">
        <div v-if="detail.coverImage"><el-image :src="detail.coverImage" :preview-src-list="[detail.coverImage]" fit="cover" preview-teleported style="width: 120px; height: 120px; border-radius: 18px" /></div>
        <div><strong>商品：</strong>{{ detail.productName }}</div>
        <div><strong>分类：</strong>{{ detail.categoryName || '-' }}</div>
        <div><strong>农户：</strong>{{ detail.farmerName || '-' }}</div>
        <div><strong>产地：</strong>{{ detail.originPlace || '-' }}</div>
        <div><strong>价格：</strong>{{ detail.price }}</div>
        <div><strong>库存：</strong>{{ detail.stock }}</div>
        <div><strong>追溯状态：</strong>{{ detail.traceStatusText }}</div>
        <div><strong>产地说明：</strong>{{ detail.originDesc || '-' }}</div>
        <div><strong>检测说明：</strong>{{ detail.inspectDesc || '-' }}</div>
      </div>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="库存调整" width="480px">
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-position="top">
        <el-form-item label="最新库存" prop="stock"><el-input-number v-model="stockForm.stock" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="调整说明" prop="adjustReason"><el-input v-model="stockForm.adjustReason" type="textarea" :rows="3" maxlength="100" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="success" :loading="stockSubmitting" @click="submitStock">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authState } from '../stores/auth'
import { categoryOptionsApi } from '../api/categories'
import { farmerOptionsApi } from '../api/farmers'
import { productPageApi, productDetailApi, createProductApi, updateProductApi, updateProductStatusApi, updateProductStockApi, deleteProductApi } from '../api/products'
import { uploadImageApi } from '../api/files'

const query = reactive({ pageNum: 1, pageSize: 10, productName: '', originPlace: '', categoryId: '', farmerId: '', saleStatus: '', stockStatus: '', hasCoverImage: '' })
const records = ref([])
const total = ref(0)
const listLoading = ref(false)
const submitLoading = ref(false)
const detailLoading = ref(false)
const stockSubmitting = ref(false)
const categoryOptions = ref([])
const farmerOptions = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const stockVisible = ref(false)
const productFormRef = ref(null)
const stockFormRef = ref(null)
const editingId = ref(null)
const stockProductId = ref(null)
const rowActionId = ref(null)
const rowActionType = ref('')
const detail = ref(null)
const form = reactive({ farmerId: null, categoryId: null, productName: '', price: 0, stock: 0, unitName: '斤', originPlace: '', coverImage: '', description: '', saleStatus: 1 })
const stockForm = reactive({ stock: 0, adjustReason: '' })

const formRules = {
  farmerId: [{ required: true, message: '请选择所属农户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'change' }],
  unitName: [{ required: true, message: '请输入计量单位', trigger: 'blur' }],
  originPlace: [{ required: true, message: '请输入产地信息', trigger: 'blur' }],
  saleStatus: [{ required: true, message: '请选择上架状态', trigger: 'change' }],
}
const stockRules = {
  stock: [{ required: true, message: '请输入库存数量', trigger: 'change' }],
  adjustReason: [{ required: true, message: '请输入调整说明', trigger: 'blur' }],
}

function stockLabel(stock) {
  if (stock === 0) {
    return '已售罄'
  }
  if (stock <= 20) {
    return `偏低 ${stock}`
  }
  return `充足 ${stock}`
}

function stockTagType(stock) {
  if (stock === 0) {
    return 'danger'
  }
  if (stock <= 20) {
    return 'warning'
  }
  return 'success'
}

async function loadOptions() {
  categoryOptions.value = await categoryOptionsApi()
  farmerOptions.value = await farmerOptionsApi()
  if (authState.user?.roleCode === 'FARMER' && farmerOptions.value.length) {
    form.farmerId = farmerOptions.value[0].value
  }
}

async function loadProducts() {
  try {
    listLoading.value = true
    const data = await productPageApi(query)
    records.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(error.message || '加载商品失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadProducts()
}

function resetQuery() {
  query.pageNum = 1
  query.pageSize = 10
  query.productName = ''
  query.originPlace = ''
  query.categoryId = ''
  query.farmerId = ''
  query.saleStatus = ''
  query.stockStatus = ''
  query.hasCoverImage = ''
  loadProducts()
}

function openForm(row) {
  editingId.value = row?.id ?? null
  form.farmerId = row?.farmerId ?? (farmerOptions.value[0]?.value ?? null)
  form.categoryId = row?.categoryId ?? null
  form.productName = row?.productName ?? ''
  form.price = row?.price ?? 0
  form.stock = row?.stock ?? 0
  form.unitName = row?.unitName ?? '斤'
  form.originPlace = row?.originPlace ?? ''
  form.coverImage = row?.coverImage ?? ''
  form.description = row?.description ?? ''
  form.saleStatus = row?.saleStatus ?? 1
  dialogVisible.value = true
  productFormRef.value?.clearValidate()
}

async function handleCoverChange(uploadFile) {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    return
  }
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('仅支持 JPG、PNG、WebP 图片')
    return
  }
  if (rawFile.size > 1024 * 1024) {
    ElMessage.error('图片大小不能超过 1MB')
    return
  }
  try {
    const result = await uploadImageApi(rawFile)
    form.coverImage = result.path
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.message || '封面上传失败')
  }
}

async function submit() {
  try {
    await productFormRef.value.validate()
    submitLoading.value = true
    if (editingId.value) {
      const product = await updateProductApi(editingId.value, form)
      applyProductMutation(toProductRow(product), { isCreate: false })
      ElMessage.success('商品已更新')
    } else {
      const product = await createProductApi(form)
      applyProductMutation(toProductRow(product), { isCreate: true })
      ElMessage.success('商品已新增')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存商品失败')
    }
  } finally {
    submitLoading.value = false
  }
}

async function toggleStatus(row) {
  try {
    rowActionId.value = row.id
    rowActionType.value = 'status'
    const product = await updateProductStatusApi(row.id, row.saleStatus === 1 ? 0 : 1)
    applyProductMutation(toProductRow(product), { isCreate: false })
    if (detail.value?.id === product.id) {
      detail.value = product
    }
    ElMessage.success('商品状态已更新')
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败')
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

async function removeProduct(id) {
  try {
    await ElMessageBox.confirm('确认删除该商品吗？', '删除提示', { type: 'warning' })
    rowActionId.value = id
    rowActionType.value = 'delete'
    await deleteProductApi(id)
    removeProductLocal(id)
    if (detail.value?.id === id) {
      detailVisible.value = false
      detail.value = null
    }
    ElMessage.success('商品已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除商品失败')
    }
  } finally {
    rowActionId.value = null
    rowActionType.value = ''
  }
}

function removeProductLocal(id) {
  const next = records.value.filter((item) => item.id !== id)
  if (next.length !== records.value.length) {
    total.value = Math.max(total.value - 1, 0)
  }
  records.value = next
}

async function showDetail(id) {
  try {
    rowActionId.value = id
    rowActionType.value = 'detail'
    detailLoading.value = true
    detail.value = await productDetailApi(id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载详情失败')
  } finally {
    detailLoading.value = false
    rowActionId.value = null
    rowActionType.value = ''
  }
}

function openStockDialog(row) {
  stockProductId.value = row.id
  stockForm.stock = row.stock
  stockForm.adjustReason = ''
  stockVisible.value = true
  stockFormRef.value?.clearValidate()
}

async function submitStock() {
  try {
    await stockFormRef.value.validate()
    stockSubmitting.value = true
    const product = await updateProductStockApi(stockProductId.value, stockForm)
    applyProductMutation(toProductRow(product), { isCreate: false })
    if (detail.value?.id === product.id) {
      detail.value = product
    }
    ElMessage.success('库存已更新')
    stockVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '更新库存失败')
    }
  } finally {
    stockSubmitting.value = false
  }
}

function toProductRow(detail) {
  return {
    id: detail.id,
    farmerId: detail.farmerId,
    categoryId: detail.categoryId,
    productName: detail.productName,
    price: detail.price,
    stock: detail.stock,
    unitName: detail.unitName,
    originPlace: detail.originPlace,
    coverImage: detail.coverImage,
    description: detail.description,
    saleStatus: detail.saleStatus,
    salesCount: detail.salesCount,
    createTime: detail.createTime,
    updateTime: detail.updateTime,
    categoryName: detail.categoryName,
    farmerName: detail.farmerName,
  }
}

function matchesProductFilter(row) {
  const productKeyword = query.productName.trim().toLowerCase()
  if (productKeyword && !`${row.productName || ''}`.toLowerCase().includes(productKeyword)) {
    return false
  }
  const originKeyword = query.originPlace.trim().toLowerCase()
  if (originKeyword && !`${row.originPlace || ''}`.toLowerCase().includes(originKeyword)) {
    return false
  }
  if (query.categoryId !== '' && row.categoryId !== query.categoryId) {
    return false
  }
  if (query.farmerId !== '' && row.farmerId !== query.farmerId) {
    return false
  }
  if (query.saleStatus !== '' && row.saleStatus !== query.saleStatus) {
    return false
  }
  if (query.stockStatus !== '') {
    const stockStatus = row.stock === 0 ? 0 : row.stock <= 20 ? 1 : 2
    if (stockStatus !== query.stockStatus) {
      return false
    }
  }
  if (query.hasCoverImage !== '') {
    const hasCoverImage = row.coverImage ? 1 : 0
    if (hasCoverImage !== query.hasCoverImage) {
      return false
    }
  }
  return true
}

function applyProductMutation(row, { isCreate }) {
  const next = [...records.value]
  const index = next.findIndex((item) => item.id === row.id)
  const matched = matchesProductFilter(row)

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

onMounted(async () => {
  await loadOptions()
  await loadProducts()
})
</script>