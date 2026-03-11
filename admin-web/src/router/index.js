import { createRouter, createWebHistory } from 'vue-router'
import { clearAuth, fetchCurrentUser, authState } from '../stores/auth'

const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const CategoriesView = () => import('../views/CategoriesView.vue')
const FarmersView = () => import('../views/FarmersView.vue')
const ProductsView = () => import('../views/ProductsView.vue')
const OrdersView = () => import('../views/OrdersView.vue')
const TracesView = () => import('../views/TracesView.vue')
const AdminLayout = () => import('../layouts/AdminLayout.vue')

const routes = [
  { path: '/login', name: 'login', component: LoginView },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true } },
      { path: '/categories', name: 'categories', component: CategoriesView, meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: '/farmers', name: 'farmers', component: FarmersView, meta: { requiresAuth: true, roles: ['ADMIN'] } },
      { path: '/products', name: 'products', component: ProductsView, meta: { requiresAuth: true } },
      { path: '/orders', name: 'orders', component: OrdersView, meta: { requiresAuth: true } },
      { path: '/traces', name: 'traces', component: TracesView, meta: { requiresAuth: true } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  if (to.path === '/login') {
    if (authState.user) {
      return '/dashboard'
    }
    return true
  }

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!authState.user) {
    try {
      await fetchCurrentUser()
    } catch {
      clearAuth()
      return '/login'
    }
  }

  const roles = to.meta.roles || []
  if (roles.length && !roles.includes(authState.user?.roleCode)) {
    return '/dashboard'
  }

  return true
})

export default router