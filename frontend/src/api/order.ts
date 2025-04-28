import { axios as request } from '../utils/request'

// 获取用户订单列表
export function getOrders() {
  return request({
    url: '/api/orders',
    method: 'get'
  })
}

// 获取订单详情
export function getOrderDetail(orderId: string) {
  return request({
    url: `/api/orders/${orderId}`,
    method: 'get'
  })
}

// 取消订单
export function cancelOrder(orderId: string) {
  return request({
    url: `/api/orders/${orderId}/cancel`,
    method: 'post'
  })
}

// 确认收货
export function confirmReceipt(orderId: string) {
  return request({
    url: `/api/orders/${orderId}/confirm`,
    method: 'post'
  })
}

// 订单支付
export function payOrder(orderId: string) {
  // 返回完整URL，前端会用于跳转
  return `/api/orders/${orderId}/pay`
}

// 查询订单状态
export function getOrderStatus(orderId: string) {
  return request({
    url: `/api/orders/${orderId}/status`,
    method: 'get'
  })
}

// 检查订单支付状态(主动向支付宝查询)
export function checkPaymentStatus(orderId: string) {
  return request({
    url: `/api/orders/${orderId}/check-payment`,
    method: 'post'
  })
}