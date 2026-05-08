<template>
  <div>
    <h2 class="page-title">我的订单</h2>

    <el-empty v-if="!orders.length" description="暂无订单" />

    <el-card v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span class="order-no">订单号：{{ order.orderNo }}</span>
        <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
      </div>
      <div class="order-amount">实付：<strong class="price">¥{{ order.totalAmount }}</strong></div>
      <div class="order-time">下单时间：{{ order.createTime }}</div>
      <div class="order-actions">
        <!-- 待支付：显示支付和取消按钮 -->
        <template v-if="order.status === 0">
          <el-button type="primary" size="small" @click="payOrder(order.orderNo)">立即支付</el-button>
          <el-button size="small" @click="cancelOrder(order.orderNo)">取消订单</el-button>
        </template>
        <!-- 已发货：显示查看物流 -->
        <el-button
          v-if="order.status >= 1"
          size="small"
          @click="$router.push(`/logistics/${order.id}`)"
        >查看物流</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api'

const router = useRouter()
const orders = ref([])

onMounted(loadOrders)

async function loadOrders() {
  const res = await orderApi.list()
  orders.value = res.data || []
}

async function payOrder(orderNo) {
  await orderApi.pay(orderNo)
  ElMessage.success('支付成功')
  loadOrders()
}

async function cancelOrder(orderNo) {
  await orderApi.cancel(orderNo)
  ElMessage.success('订单已取消')
  loadOrders()
}

// 订单状态文字映射
function statusText(status) {
  const map = { 0: '待支付', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

// 订单状态标签类型
function statusTagType(status) {
  const map = { 0: 'warning', 1: '', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || ''
}
</script>

<style scoped>
.page-title { margin-bottom: 20px; }
.order-card { margin-bottom: 16px; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-weight: bold; color: #333; }
.order-amount { margin: 4px 0; }
.price { color: #f56c6c; }
.order-time { color: #999; font-size: 13px; margin-bottom: 12px; }
.order-actions { display: flex; gap: 8px; }
</style>
