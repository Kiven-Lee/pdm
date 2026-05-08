import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 购物车状态 Store
 * 购物车数据存储在后端 Redis，此处只做前端展示用的本地缓存
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  // 购物车商品总数（所有商品数量之和）
  const totalCount = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  // 选中商品的总金额
  const checkedAmount = computed(() =>
    items.value
      .filter(item => item.checked)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
      .toFixed(2)
  )

  function setItems(cartItems) {
    items.value = cartItems
  }

  return { items, totalCount, checkedAmount, setItems }
})
