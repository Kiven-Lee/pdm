<template>
  <div>
    <h2 class="page-title">确认订单</h2>

    <!-- 收货地址 -->
    <el-card class="section-card">
      <template #header><span>收货地址</span></template>
      <el-form :model="addressForm" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收件人">
              <el-input v-model="addressForm.name" placeholder="收件人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="手机号">
              <el-input v-model="addressForm.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址">
          <el-input v-model="addressForm.address" placeholder="省市区 + 详细地址" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 商品列表 -->
    <el-card class="section-card">
      <template #header><span>商品清单</span></template>
      <el-table :data="checkedItems" style="width: 100%">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">{{ row.productName }}</template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="80" prop="quantity" />
        <el-table-column label="小计" width="120">
          <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 订单备注 -->
    <el-card class="section-card">
      <template #header><span>备注</span></template>
      <el-input v-model="remark" type="textarea" placeholder="选填，对本次交易的说明" :rows="2" />
    </el-card>

    <!-- 提交 -->
    <div class="submit-bar">
      <span>实付金额：<strong class="total-price">¥{{ totalAmount }}</strong></span>
      <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi, cartApi } from '@/api'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()

// 只结算选中的商品
const checkedItems = computed(() => cartStore.items.filter(i => i.checked))
const totalAmount = computed(() =>
  checkedItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2)
)

const addressForm = ref({ name: '', phone: '', address: '' })
const remark = ref('')
const submitting = ref(false)

async function submitOrder() {
  if (!addressForm.value.name || !addressForm.value.phone || !addressForm.value.address) {
    ElMessage.warning('请填写完整收货地址')
    return
  }
  submitting.value = true
  try {
    const res = await orderApi.create({
      items: checkedItems.value.map(i => ({
        productId: i.productId,
        productName: i.productName,
        productImage: i.mainImage,
        price: i.price,
        quantity: i.quantity
      })),
      address: JSON.stringify(addressForm.value),
      remark: remark.value
    })
    // 清空已下单的商品
    await cartApi.clear()
    cartStore.setItems([])
    ElMessage.success('订单创建成功')
    router.push('/order')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page-title { margin-bottom: 20px; }
.section-card { margin-bottom: 16px; }
.submit-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}
.total-price { font-size: 22px; color: #f56c6c; }
</style>
