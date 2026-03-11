<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>分类管理</h2>
        <p>维护农产品分类，为商品录入和筛选提供基础数据。</p>
      </div>
      <el-button type="success" :loading="listLoading" @click="openDialog()">新增分类</el-button>
    </div>

    <el-table v-loading="listLoading" :data="categories" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="categoryName" label="分类名称" min-width="160" />
      <el-table-column prop="sortNo" label="排序" width="100" />
      <el-table-column prop="productCount" label="商品数" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="removeCategory(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="table-empty">
          <el-empty description="暂无分类数据" />
        </div>
      </template>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryListApi, createCategoryApi, deleteCategoryApi, updateCategoryApi } from '../api/categories'

const categories = ref([])
const listLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const form = reactive({ categoryName: '', sortNo: 0, status: 1 })
const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sortNo: [{ required: true, message: '请输入排序号', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

async function loadCategories() {
  try {
    listLoading.value = true
    categories.value = await categoryListApi()
  } catch (error) {
    ElMessage.error(error.message || '加载分类失败')
  } finally {
    listLoading.value = false
  }
}

function openDialog(row) {
  editingId.value = row?.id ?? null
  form.categoryName = row?.categoryName ?? ''
  form.sortNo = row?.sortNo ?? 0
  form.status = row?.status ?? 1
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    if (editingId.value) {
      const category = await updateCategoryApi(editingId.value, form)
      upsertCategory(category)
      ElMessage.success('分类已更新')
    } else {
      const category = await createCategoryApi(form)
      upsertCategory(category)
      ElMessage.success('分类已新增')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '保存分类失败')
    }
  } finally {
    submitLoading.value = false
  }
}

function upsertCategory(category) {
  const next = [...categories.value]
  const index = next.findIndex((item) => item.id === category.id)
  if (index >= 0) {
    next[index] = category
  } else {
    next.unshift(category)
  }
  next.sort((left, right) => {
    if (left.sortNo !== right.sortNo) {
      return left.sortNo - right.sortNo
    }
    return right.id - left.id
  })
  categories.value = next
}

async function removeCategory(id) {
  try {
    await ElMessageBox.confirm('确认删除该分类吗？', '删除提示', { type: 'warning' })
    await deleteCategoryApi(id)
    removeCategoryLocal(id)
    ElMessage.success('分类已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除分类失败')
    }
  }
}

function removeCategoryLocal(id) {
  categories.value = categories.value.filter((item) => item.id !== id)
}

onMounted(loadCategories)
</script>