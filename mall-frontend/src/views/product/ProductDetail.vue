<template>
  <div v-loading="loading" class="detail-container">
    <template v-if="product">
      <el-row :gutter="40">
        <!-- 商品图片 -->
        <el-col :span="10">
          <img :src="product.mainImage || defaultImg" class="main-img" />
        </el-col>

        <!-- 商品信息 -->
        <el-col :span="14">
          <h1 class="product-name">{{ product.name }}</h1>
          <div class="price-row">
            <span class="price">¥{{ product.price }}</span>
            <span class="sales">已售 {{ product.sales }} 件</span>
          </div>
          <el-divider />
          <p class="description">{{ product.description }}</p>
          <el-divider />

          <!-- 数量选择 -->
          <div class="quantity-row">
            <span>数量：</span>
            <el-input-number v-model="quantity" :min="1" :max="99" />
          </div>

          <!-- 操作按钮 -->
          <div class="action-row">
            <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
            <el-button type="danger" size="large" @click="buyNow">立即购买</el-button>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const product = ref(null)
const loading = ref(false)
const quantity = ref(1)
const defaultImg = 'https://via.placeholder.com/400x400?text=No+Image'

onMounted(async () => {
  loading.value = true
  try {
    const res = await productApi.detail(route.params.id)
    product.value = res.data
  } finally {
    loading.value = false
  }
})

async function addToCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  await cartApi.add({
    productId: product.value.id,
    productName: product.value.name,
    mainImage: product.value.mainImage,
    price: product.value.price,
    quantity: quantity.value
  })
  ElMessage.success('已加入购物车')
  const res = await cartApi.list()
  cartStore.setItems(res.data || [])
}

async function buyNow() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  await addToCart()
  router.push('/cart')
}
</script>

<style scoped>
.detail-container { max-width: 900px; margin: 0 auto; }
.main-img { width: 100%; border-radius: 8px; }
.product-name { font-size: 22px; font-weight: bold; margin-bottom: 12px; }
.price-row { display: flex; align-items: baseline; gap: 16px; }
.price { font-size: 28px; color: #f56c6c; font-weight: bold; }
.sales { color: #999; font-size: 14px; }
.description { color: #666; line-height: 1.8; }
.quantity-row { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
.action-row { display: flex; gap: 16px; }
</style>
