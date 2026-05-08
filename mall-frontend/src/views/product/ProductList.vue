<template>
  <div>
    <!-- 分类筛选 -->
    <div class="category-bar">
      <el-tag
        v-for="cat in categories"
        :key="cat.id"
        :type="selectedCategory === cat.id ? '' : 'info'"
        class="cat-tag"
        @click="selectCategory(cat.id)"
      >{{ cat.name }}</el-tag>
      <el-tag type="info" class="cat-tag" @click="selectCategory(null)">全部</el-tag>
    </div>

    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品..."
        clearable
        @keyup.enter="loadProducts"
        style="width: 300px"
      >
        <template #append>
          <el-button :icon="Search" @click="loadProducts" />
        </template>
      </el-input>
    </div>

    <!-- 商品列表 -->
    <el-row :gutter="20" v-loading="loading">
      <el-col :span="6" v-for="product in products" :key="product.id" class="product-col">
        <el-card class="product-card" shadow="hover" @click="goDetail(product.id)">
          <img :src="product.mainImage || defaultImg" class="product-img" />
          <div class="product-info">
            <p class="product-name">{{ product.name }}</p>
            <p class="product-price">¥{{ product.price }}</p>
            <p class="product-sales">已售 {{ product.sales }}</p>
          </div>
          <el-button
            type="primary"
            size="small"
            class="add-cart-btn"
            @click.stop="addToCart(product)"
          >加入购物车</el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="pagination"
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productApi, cartApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const products = ref([])
const categories = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const keyword = ref('')
const selectedCategory = ref(null)
const defaultImg = 'https://via.placeholder.com/200x200?text=No+Image'

onMounted(() => {
  loadCategories()
  loadProducts()
})

async function loadCategories() {
  const res = await productApi.categoryList()
  // 只显示一级分类
  categories.value = (res.data || []).filter(c => c.level === 1)
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.list({
      page: page.value,
      size: pageSize.value,
      categoryId: selectedCategory.value,
      keyword: keyword.value || undefined
    })
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function selectCategory(id) {
  selectedCategory.value = id
  page.value = 1
  loadProducts()
}

function onPageChange(p) {
  page.value = p
  loadProducts()
}

function goDetail(id) {
  router.push(`/product/${id}`)
}

async function addToCart(product) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await cartApi.add({
    productId: product.id,
    productName: product.name,
    mainImage: product.mainImage,
    price: product.price,
    quantity: 1
  })
  ElMessage.success('已加入购物车')
  // 刷新购物车数量
  const cartRes = await cartApi.list()
  cartStore.setItems(cartRes.data || [])
}
</script>

<style scoped>
.category-bar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.cat-tag { cursor: pointer; }
.search-bar { margin-bottom: 20px; }
.product-col { margin-bottom: 20px; }
.product-card { cursor: pointer; transition: transform .2s; }
.product-card:hover { transform: translateY(-4px); }
.product-img { width: 100%; height: 180px; object-fit: cover; border-radius: 4px; }
.product-info { padding: 8px 0; }
.product-name { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { font-size: 18px; color: #f56c6c; font-weight: bold; margin: 4px 0; }
.product-sales { font-size: 12px; color: #999; }
.add-cart-btn { width: 100%; margin-top: 8px; }
.pagination { margin-top: 20px; text-align: center; }
</style>
