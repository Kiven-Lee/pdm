import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/views/product/ProductList.vue'),
    meta: { title: '商城首页' }
  },
  {
    path: '/product/:id',
    component: () => import('@/views/product/ProductDetail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    component: () => import('@/views/cart/CartPage.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/order',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/order/confirm',
    component: () => import('@/views/order/OrderConfirm.vue'),
    meta: { title: '确认订单', requiresAuth: true }
  },
  {
    path: '/logistics/:orderId',
    component: () => import('@/views/logistics/LogisticsPage.vue'),
    meta: { title: '物流查询', requiresAuth: true }
  },
  {
    path: '/login',
    component: () => import('@/views/auth/LoginPage.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    component: () => import('@/views/auth/RegisterPage.vue'),
    meta: { title: '注册' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面，未登录时跳转到登录页
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - Mall商城` : 'Mall商城'
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
