<template>
  <div>
    <h2 class="page-title">购物车</h2>

    <el-empty v-if="!cartStore.items.length" description="购物车空空如也" />

    <template v-else>
      <el-table :data="cartStore.items" style="width: 100%">
        <el-table-column type="selection" width="50" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.mainImage || defaultImg" class="cart-img" />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              size="small"
              @change="(val) => updateQuantity(row.productId, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="removeItem(row.productId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 结算栏 -->
      <div class="checkout-bar">
        <el-button type="danger" text @click="clearCart">清空购物车</el-button>
        <div class="total-area">
          <span>合计：<strong class="total-price">¥{{ cartStore.checkedAmount }}</strong></span>
          <el-button type="primary" size="large" @click="checkout">去结算</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartApi } from '@/api'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const defaultImg = 'https://via.placeholder.com/60x60?text=IMG'

onMounted(loadCart)

async function loadCart() {
  const res = await cartApi.list()
  cartStore.setItems(res.data || [])
}

async function updateQuantity(productId, quantity) {
  await cartApi.updateQuantity(productId, quantity)
}

async function removeItem(productId) {
  await cartApi.remove(productId)
  await loadCart()
  ElMessage.success('已删除')
}

async function clearCart() {
  await ElMessageBox.confirm('确定清空购物车？', '提示', { type: 'warning' })
  await cartApi.clear()
  cartStore.setItems([])
}

function checkout() {
  const checked = cartStore.items.filter(i => i.checked)
  if (!checked.length) { ElMessage.warning('请选择商品'); return }
  router.push('/order/confirm')
}
</script>

<style scoped>
.page-title { margin-bottom: 20px; }
.product-cell { display: flex; align-items: center; gap: 10px; }
.cart-img { width: 60px; height: 60px; object-fit: cover; border-radius: 4px; }
.price { color: #f56c6c; font-weight: bold; }
.checkout-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}
.total-area { display: flex; align-items: center; gap: 20px; }
.total-price { font-size: 22px; color: #f56c6c; }
</style>
