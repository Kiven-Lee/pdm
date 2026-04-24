<template>
  <el-container class="app-container">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-inner">
        <!-- Logo -->
        <router-link to="/" class="logo">🛒 Mall 商城</router-link>

        <!-- 导航菜单 -->
        <el-menu mode="horizontal" :ellipsis="false" class="nav-menu">
          <el-menu-item @click="$router.push('/')">首页</el-menu-item>
        </el-menu>

        <!-- 右侧操作区 -->
        <div class="header-right">
          <!-- 购物车图标 -->
          <el-badge :value="cartStore.totalCount || ''" :hidden="!cartStore.totalCount">
            <el-button :icon="ShoppingCart" circle @click="$router.push('/cart')" />
          </el-badge>

          <!-- 未登录显示登录/注册 -->
          <template v-if="!userStore.isLoggedIn">
            <el-button @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>

          <!-- 已登录显示用户名和退出 -->
          <template v-else>
            <el-dropdown @command="handleCommand">
              <span class="user-name">{{ userStore.username }} <el-icon><ArrowDown /></el-icon></span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="order">我的订单</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="app-main">
      <router-view />
    </el-main>

    <!-- 底部 -->
    <el-footer class="app-footer">
      <p>© 2024 Mall 商城 - 基于 Spring Cloud Alibaba + Vue3 构建</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { ShoppingCart, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useRouter } from 'vue-router'
import { authApi, cartApi } from '@/api'
import { onMounted } from 'vue'

const userStore = useUserStore()
const cartStore = useCartStore()
const router = useRouter()

// 登录后加载购物车数量
onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const res = await cartApi.list()
      cartStore.setItems(res.data || [])
    } catch (e) { /* 忽略 */ }
  }
})

async function handleCommand(command) {
  if (command === 'order') {
    router.push('/order')
  } else if (command === 'logout') {
    await authApi.logout().catch(() => {})
    userStore.logout()
    cartStore.setItems([])
    router.push('/')
  }
}
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; }

.app-container { min-height: 100vh; }

.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 20px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
  white-space: nowrap;
}

.nav-menu { border-bottom: none; flex: 1; }

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #333;
}

.app-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.app-footer {
  background: #333;
  color: #aaa;
  text-align: center;
  line-height: 60px;
  font-size: 13px;
}
</style>
