<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, DocumentChecked, ShoppingBag, Delete, Warning, Loading } from '@element-plus/icons-vue'

// 模拟订单状态类型
enum OrderStatus {
  PENDING_PAYMENT = 'PENDING_PAYMENT',
  PAID = 'PAID',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

// 订单类型定义
interface OrderItem {
  title: string
  cover: string
  price: number
  quantity: number
}

interface Order {
  orderId: string
  orderNumber: string
  status: OrderStatus
  totalAmount: number
  createdAt: string
  items: OrderItem[]
  paymentMethod: string
  shippingAddress: {
    name: string
    phone: string
    address: string
  }
}

const router = useRouter()
const loading = ref(true)
const orders = ref<Order[]>([])

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('token')
  
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// 获取订单列表
const fetchOrders = async () => {
  if (!checkLoginStatus()) return
  
  loading.value = true
  try {
    // 模拟API调用
    // const res = await getOrders()
    setTimeout(() => {
      // 模拟数据
      orders.value = [
        {
          orderId: '1',
          orderNumber: 'TM202504280001',
          status: OrderStatus.PAID,
          totalAmount: 99.8,
          createdAt: '2025-04-28 10:30:00',
          items: [
            { title: '深入理解Java虚拟机', cover: '/books/java-vm.jpg', price: 79.9, quantity: 1 },
            { title: '算法导论', cover: '/books/algorithms.jpg', price: 19.9, quantity: 1 }
          ],
          paymentMethod: 'ALIPAY',
          shippingAddress: {
            name: '张三',
            phone: '13800138000',
            address: '北京市朝阳区某某路123号'
          }
        },
        {
          orderId: '2',
          orderNumber: 'TM202504270015',
          status: OrderStatus.PENDING_PAYMENT,
          totalAmount: 59.8,
          createdAt: '2025-04-27 15:20:00',
          items: [
            { title: 'Python编程：从入门到实践', cover: '/books/python.jpg', price: 59.8, quantity: 1 }
          ],
          paymentMethod: 'ALIPAY',
          shippingAddress: {
            name: '张三',
            phone: '13800138000',
            address: '北京市朝阳区某某路123号'
          }
        }
      ]
      loading.value = false
    }, 1000)
  } catch (error) {
    console.error('获取订单数据出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    loading.value = false
  }
}

// 取消订单
const cancelOrder = (orderId: string) => {
  ElMessage.success('订单取消成功')
  // 实际应调用API进行处理
  orders.value = orders.value.map(order => {
    if (order.orderId === orderId) {
      return { ...order, status: OrderStatus.CANCELLED }
    }
    return order
  })
}

// 去支付
const goToPay = (orderId: string) => {
  ElMessage.info('跳转到支付页面')
  // 实际应跳转到支付页面
}

// 确认收货
const confirmReceipt = (orderId: string) => {
  ElMessage.success('确认收货成功')
  // 实际应调用API进行处理
  orders.value = orders.value.map(order => {
    if (order.orderId === orderId) {
      return { ...order, status: OrderStatus.COMPLETED }
    }
    return order
  })
}

// 格式化订单状态
const formatOrderStatus = (status: OrderStatus) => {
  const statusMap = {
    [OrderStatus.PENDING_PAYMENT]: '待付款',
    [OrderStatus.PAID]: '已付款',
    [OrderStatus.SHIPPED]: '已发货',
    [OrderStatus.DELIVERED]: '待收货',
    [OrderStatus.COMPLETED]: '已完成',
    [OrderStatus.CANCELLED]: '已取消'
  }
  return statusMap[status] || '未知状态'
}

// 获取状态对应的颜色
const getStatusColor = (status: OrderStatus) => {
  const colorMap = {
    [OrderStatus.PENDING_PAYMENT]: '#E6A23C',
    [OrderStatus.PAID]: '#409EFF',
    [OrderStatus.SHIPPED]: '#67C23A',
    [OrderStatus.DELIVERED]: '#409EFF',
    [OrderStatus.COMPLETED]: '#67C23A',
    [OrderStatus.CANCELLED]: '#909399'
  }
  return colorMap[status] || '#909399'
}

// 图片加载错误处理
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = '/placeholder.jpg'
}

// 页面加载时获取订单数据
onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="order-container">
    <div class="order-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><Document /></el-icon>
        我的订单
      </h1>
    </div>
    
    <el-card class="order-content" v-loading="loading">
      <!-- 订单为空 -->
      <div v-if="!loading && orders.length === 0" class="empty-order">
        <el-empty description="您还没有订单">
          <template #image>
            <el-icon class="empty-icon"><Document /></el-icon>
          </template>
          <template #description>
            <p>您还没有下过订单</p>
          </template>
          <el-button type="primary" @click="router.push('/mall')">
            <el-icon><ShoppingBag /></el-icon>
            去选购图书
          </el-button>
        </el-empty>
      </div>
      
      <!-- 有订单 -->
      <template v-else-if="!loading">
        <div class="orders-list">
          <div v-for="order in orders" :key="order.orderId" class="order-item">
            <div class="order-header-info">
              <div class="order-number">
                订单号：{{ order.orderNumber }}
              </div>
              <div class="order-date">
                下单时间：{{ order.createdAt }}
              </div>
              <div class="order-status">
                <el-tag :color="getStatusColor(order.status)" effect="dark" size="small">
                  {{ formatOrderStatus(order.status) }}
                </el-tag>
              </div>
            </div>
            
            <div class="order-products">
              <div v-for="(item, index) in order.items" :key="index" class="order-product-item">
                <div class="product-image">
                  <img :src="item.cover" :alt="item.title" @error="handleImageError">
                </div>
                <div class="product-info">
                  <div class="product-title">{{ item.title }}</div>
                  <div class="product-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
                </div>
                <div class="product-subtotal">
                  ¥{{ (item.price * item.quantity).toFixed(2) }}
                </div>
              </div>
            </div>
            
            <div class="order-footer">
              <div class="order-total">
                共 {{ order.items.reduce((sum, item) => sum + item.quantity, 0) }} 件商品，
                合计：<span class="total-price">¥{{ order.totalAmount.toFixed(2) }}</span>
              </div>
              
              <div class="order-actions">
                <!-- 根据订单状态显示不同的按钮 -->
                <template v-if="order.status === OrderStatus.PENDING_PAYMENT">
                  <el-button size="small" @click="cancelOrder(order.orderId)">取消订单</el-button>
                  <el-button type="primary" size="small" @click="goToPay(order.orderId)">去支付</el-button>
                </template>
                
                <template v-else-if="order.status === OrderStatus.DELIVERED">
                  <el-button type="success" size="small" @click="confirmReceipt(order.orderId)">确认收货</el-button>
                </template>
                
                <template v-else-if="order.status !== OrderStatus.CANCELLED">
                  <el-button size="small" @click="router.push(`/order-detail/${order.orderId}`)">查看详情</el-button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.order-container {
  padding: 2rem;
  background-color: #f5f7fa;
  background-image: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
  min-height: calc(100vh - 60px);
}

.order-header {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 1.8rem;
  color: #e74c3c;
  display: flex;
  align-items: center;
}

.title-icon {
  margin-right: 10px;
  font-size: 1.8rem;
}

.order-content {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* 空订单样式 */
.empty-order {
  padding: 3rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 5rem;
  color: #e0e0e0;
  margin-bottom: 1rem;
}

/* 订单列表样式 */
.orders-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-item {
  background-color: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.order-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #f9f9f9;
  border-bottom: 1px solid #ebeef5;
}

.order-number {
  font-weight: 500;
  color: #303133;
}

.order-date {
  color: #909399;
  font-size: 0.9rem;
}

/* 订单商品样式 */
.order-products {
  padding: 20px;
}

.order-product-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.order-product-item:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  margin-right: 15px;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  overflow: hidden;
}

.product-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.product-info {
  flex: 1;
}

.product-title {
  font-weight: 500;
  margin-bottom: 5px;
}

.product-price {
  color: #909399;
  font-size: 0.9rem;
}

.product-subtotal {
  color: #e74c3c;
  font-weight: 500;
  margin-left: 20px;
}

/* 订单底部样式 */
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #f9f9f9;
  border-top: 1px solid #ebeef5;
}

.order-total {
  color: #606266;
}

.total-price {
  color: #e74c3c;
  font-size: 1.2rem;
  font-weight: bold;
}

.order-actions {
  display: flex;
  gap: 10px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .order-container {
    padding: 1rem;
  }
  
  .order-header-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .order-status {
    align-self: flex-end;
    margin-top: -20px;
  }
  
  .order-footer {
    flex-direction: column;
    gap: 15px;
  }
  
  .order-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>