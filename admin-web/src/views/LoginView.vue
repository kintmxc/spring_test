<template>
  <div class="login-page">
    <div class="login-panel">
      <section class="login-visual">
        <div class="tag-soft" style="background: rgba(255,255,255,0.18); color: #fff">Admin Console</div>
        <h1 style="font-size: 42px; line-height: 1.15; margin: 22px 0 16px">把农产品管理、订单履约和追溯维护集中到一个工作台。</h1>
        <p style="max-width: 430px; line-height: 1.8; color: rgba(255,255,255,0.84)">
          当前后台已接入登录、分类、农户、商品、订单、追溯和经营看板接口，适合直接联调管理端页面。
        </p>
      </section>
      <section class="login-form-wrap">
        <div class="login-form-card">
          <div style="margin-bottom: 28px">
            <div class="tag-soft">欢迎回来</div>
            <h2 style="margin: 16px 0 6px; font-size: 32px">登录管理端</h2>
            <p class="muted">仅管理员账号可登录后台，农户和消费者请使用小程序端。</p>
          </div>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" size="large" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="success" size="large" style="width: 100%; margin-top: 10px" :loading="loading" @click="handleLogin">
              进入管理端
            </el-button>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../stores/auth'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  try {
    await formRef.value.validate()
    loading.value = true
    await login(form)
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>