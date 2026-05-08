import request from '@/utils/request'

/** 认证相关 API */
export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
  logout: () => request.post('/auth/logout')
}

/** 商品相关 API */
export const productApi = {
  list: (params) => request.get('/product/list', { params }),
  detail: (id) => request.get(`/product/detail/${id}`),
  categoryList: () => request.get('/product/category/list')
}

/** 购物车相关 API */
export const cartApi = {
  list: () => request.get('/cart/list'),
  add: (data) => request.post('/cart/add', data),
  updateQuantity: (productId, quantity) =>
    request.put('/cart/quantity', null, { params: { productId, quantity } }),
  remove: (productId) => request.delete(`/cart/remove/${productId}`),
  clear: () => request.delete('/cart/clear'),
  updateChecked: (productId, checked) =>
    request.put('/cart/checked', null, { params: { productId, checked } })
}

/** 订单相关 API */
export const orderApi = {
  create: (data) => request.post('/order/create', data),
  list: () => request.get('/order/list'),
  items: (orderId) => request.get(`/order/items/${orderId}`),
  pay: (orderNo) => request.post(`/order/pay/${orderNo}`),
  cancel: (orderNo) => request.post(`/order/cancel/${orderNo}`)
}

/** 物流相关 API */
export const logisticsApi = {
  getByOrderId: (orderId) => request.get(`/logistics/order/${orderId}`),
  getTrack: (trackingNo) => request.get(`/logistics/track/${trackingNo}`)
}
