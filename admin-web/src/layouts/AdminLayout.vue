<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand-card">
        <h1>本地农产品<br />管理端</h1>
        <p>围绕农户、商品、订单、追溯和经营看板构建的一体化后台。</p>
      </div>
      <nav class="side-nav">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          active-class="active"
        >
          <component :is="item.icon" style="width: 18px; height: 18px" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
    <main class="content">
      <header class="topbar">
        <div>
          <div class="tag-soft">{{ roleText }}</div>
          <h3 style="margin: 10px 0 4px">{{ authState.user?.username || '未登录' }}</h3>
          <div class="muted">{{ authState.user?.roleCode === 'ADMIN' ? '管理员拥有全局管理权限' : '农户仅能操作自己的业务数据' }}</div>
        </div>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </header>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Grid, UserFilled, Goods, Tickets, DataBoard } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authState, logout } from '../stores/auth'

const router = useRouter()

const menus = computed(() => {
  const base = [
    { path: '/dashboard', label: '经营看板', icon: HomeFilled },
    { path: '/products', label: '农产品管理', icon: Goods },
    { path: '/orders', label: '订单管理', icon: Tickets },
    { path: '/traces', label: '质量追溯', icon: DataBoard },
  ]
  if (authState.user?.roleCode === 'ADMIN') {
    base.splice(1, 0, { path: '/categories', label: '分类管理', icon: Grid })
    base.splice(2, 0, { path: '/farmers', label: '农户管理', icon: UserFilled })
  }
  return base
})

const roleText = computed(() => (authState.user?.roleCode === 'ADMIN' ? '管理员' : '农户'))

async function handleLogout() {
  await logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>