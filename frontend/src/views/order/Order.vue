<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, DocumentChecked, ShoppingBag, Delete, Warning, Loading } from '@element-plus/icons-vue'
import { getOrders, cancelOrder, payOrder, confirmReceipt, checkPaymentStatus } from '../../api/order'

// 订单状态类型
enum OrderStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  TIMEOUT = 'TIMEOUT'
}

// 移除OrderItem类型定义

// 订单类型定义 - 移除items字段
interface Order {
  orderId: number | string
  userId: number
  username: string
  totalAmount: number
  status: string
  createTime?: string
  paymentMethod: string
  // 移除items字段
}

const router = useRouter()
const loading = ref(true)
const orders = ref<Order[]>([])
const hasDebugInfo = ref(false)

// 状态匹配辅助函数
const matchOrderStatus = (status: string | undefined, targetStatus: string) => {
  if (!status) return false
  return status.trim().toUpperCase() === targetStatus.toUpperCase()
}

// 调试功能 - 输出订单状态信息
const logOrderStatuses = () => {
  console.log('当前订单列表:', orders.value)
  if (!orders.value || orders.value.length === 0) {
    console.log('订单列表为空')
    return
  }
  
  orders.value.forEach(order => {
    console.log(`订单ID: ${order.orderId}, 状态: ${order.status}, 类型: ${typeof order.status}`)
  })
  
  // 切换调试信息显示
  hasDebugInfo.value = !hasDebugInfo.value
}

// 获取订单列表 - 移除items相关处理
const fetchOrders = async () => {
  if (!checkLoginStatus()) return
  
  loading.value = true
  try {
    const response = await getOrders()
    console.log('订单响应数据:', response)
    
    if (response && response.data && response.data.code === '200') {
      // 只处理状态标准化，移除items处理逻辑
      orders.value = (response.data.data || []).map((order: any) => {
        // 确保状态值标准化
        if (order.status) {
          order.status = String(order.status).trim().toUpperCase()
        }
        return order
      })
      
      console.log('处理后的订单数据:', orders.value)
    } else {
      ElMessage.error(response.data?.msg || '获取订单列表失败')
      orders.value = []
    }
  } catch (error) {
    console.error('获取订单数据出错:', error)
    ElMessage.error('网络错误，请稍后重试')
    orders.value = []
  } finally {
    loading.value = false
  }
}

// 取消订单
const handleCancelOrder = async (orderId: string | number) => {
  try {
    ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      const response = await cancelOrder(orderId.toString())
      if (response && response.data && response.data.code === '200') {
        ElMessage.success('订单取消成功')
        fetchOrders() // 重新加载订单列表
      } else {
        ElMessage.error(response.data?.msg || '取消订单失败')
      }
    }).catch(() => {
      // 用户点击取消，不做任何操作
    })
  } catch (error) {
    console.error('取消订单出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 去支付 - 处理HTML或JSON响应
const goToPay = async (orderId: string | number) => {
  try {
    // 显示加载中
    ElMessage.info('正在准备支付页面...')
    
    // 确保有支付窗口
    const payWindow = window.open('about:blank', '_blank')
    if (!payWindow) {
      ElMessage.warning('浏览器阻止了弹出窗口，请允许弹出窗口后重试')
      return
    }
    
    // 获取支付信息
    console.log('正在请求支付链接:', `/api/orders/${orderId}/generate-pay-url`)
    const response = await fetch(`/api/orders/${orderId}/generate-pay-url`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('token')}`
      }
    })
    
    console.log('支付API响应状态:', response.status, response.statusText)
    const contentType = response.headers.get("content-type")
    console.log('响应内容类型:', contentType)
    
    if (!response.ok) {
      // 错误处理
      payWindow.close()
      ElMessage.error(`支付请求失败: ${response.status} ${response.statusText}`)
      return
    }
    
    if (contentType && contentType.includes("application/json")) {
      // 处理JSON响应
      const result = await response.json()
      console.log('支付API返回数据:', result)
      
      if (result.data && result.data.payUrl) {
        console.log('支付链接:', result.data.payUrl)
        // 增加延时确保窗口完全准备好
        setTimeout(() => {
          payWindow.location.href = result.data.payUrl
        }, 100)
        ElMessage.success('正在跳转到支付页面')
      } else {
        payWindow.close()
        ElMessage.error('支付链接无效')
        console.error('无效的支付数据:', result)
      }
    } else {
      // 处理HTML响应
      const htmlContent = await response.text()
      console.log('HTML内容前100个字符:', htmlContent.substring(0, 100))
      
      payWindow.document.open()
      payWindow.document.write(htmlContent)
      payWindow.document.close()
      
      // 检查HTML内容是否包含支付表单
      if (htmlContent.includes('form') && htmlContent.includes('submit')) {
        ElMessage.success('正在准备支付页面')
      } else {
        ElMessage.warning('返回的HTML内容可能不包含支付表单')
      }
    }
  } catch (error) {
    console.error('支付跳转失败详情:', error)
    ElMessage.error('支付跳转失败，请稍后重试')
  }
}

// 添加直接表单提交方法
const goToPayWithForm = (orderId: string | number) => {
  try {
    ElMessage.info('正在准备支付页面...')
    
    // 获取token并检查
    const token = sessionStorage.getItem('token')
    if (!token) {
      ElMessage.warning('未检测到登录信息，请重新登录')
      router.push('/login')
      return
    }
    
    // 方式1: 创建表单并添加authorization字段
    const form = document.createElement('form')
    form.method = 'POST'
    form.action = `http://localhost:8080/api/orders/${orderId}/pay`
    form.target = '_blank'
    
    // 添加authorization字段
    const authField = document.createElement('input')
    authField.type = 'hidden'
    authField.name = 'authorization'
    authField.value = `Bearer ${token}` // 添加Bearer前缀
    form.appendChild(authField)
    
    // 添加token字段(作为备用)
    const tokenField = document.createElement('input')
    tokenField.type = 'hidden' 
    tokenField.name = 'token'
    tokenField.value = token
    form.appendChild(tokenField)
    document.body.appendChild(form)
    form.submit()
    document.body.removeChild(form)
        // 直接显示支付确认弹窗
        setTimeout(() => {
      ElMessageBox.confirm(
        '请在新窗口中完成支付。支付完成后点击"已完成支付"刷新订单状态。',
        '支付确认',
        {
          confirmButtonText: '已完成支付',
          cancelButtonText: '稍后支付',
          type: 'info',
          center: true
        }
      ).then(() => {
        // 用户点击"已完成支付"
        ElMessage.info('正在刷新订单状态...')
        checkAllPendingOrders() // 刷新订单列表
      }).catch(() => {
        // 用户点击"稍后支付"
        ElMessage.info('您可以稍后在订单页面点击"去支付"按钮继续支付')
        checkAllPendingOrders() // 刷新订单列表
      })
    }, 1000) // 短暂延迟确保支付页面已打开
  } catch (error) {
    console.error('支付跳转失败:', error)
    ElMessage.error('支付跳转失败，请稍后重试')
  }
}


// 确认收货
const handleConfirmReceipt = async (orderId: string | number) => {
  try {
    ElMessageBox.confirm('确认已收到商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }).then(async () => {
      const response = await confirmReceipt(orderId.toString())
      if (response && response.data && response.data.code === '200') {
        ElMessage.success('确认收货成功')
        fetchOrders() // 重新加载订单列表
      } else {
        ElMessage.error(response.data?.msg || '确认收货失败')
      }
    }).catch(() => {
      // 用户点击取消，不做任何操作
    })
  } catch (error) {
    console.error('确认收货出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 格式化订单状态
const formatOrderStatus = (status: string) => {
  const statusMap: Record<string, string> = {
    'PENDING': '待付款',
    'PAID': '已付款',
    'SHIPPED': '已发货',
    'DELIVERED': '待收货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消',
    'TIMEOUT': '已超时'
  }
  return statusMap[status] || '未知状态'
}

// 获取状态对应的颜色
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    'PENDING': '#E6A23C',
    'PAID': '#409EFF',
    'SHIPPED': '#67C23A',
    'DELIVERED': '#409EFF',
    'COMPLETED': '#67C23A',
    'CANCELLED': '#909399',
    'TIMEOUT': '#F56C6C'
  }
  return colorMap[status] || '#909399'
}

// 格式化日期时间
const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '未知时间'
  
  try {
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch (e) {
    return dateStr
  }
}

// 图片加载错误处理
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = '/placeholder.jpg'
}

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

// 修改checkOrderPaymentStatus函数中的fetch请求
const checkOrderPaymentStatus = async (orderId, showNotification = true) => {
  try {
    if (showNotification) {
      ElMessage.info('正在检查订单支付状态...')
    }
    
    // 使用API模块中的函数替代直接fetch调用
    const response = await checkPaymentStatus(orderId.toString())
    
    if (response && response.data && response.data.code === '200') {
      // 根据返回的订单状态更新本地数据
      const updatedOrder = response.data.data
      
      // 找到并更新本地订单数据
      const index = orders.value.findIndex(o => o.orderId === orderId)
      if (index !== -1) {
        orders.value[index] = updatedOrder
        
        // 仅当状态有变更且需要显示通知时才显示
        if (showNotification && updatedOrder.status === 'PAID') {
          ElMessage.success('确认订单已支付！')
        }
      }
      return updatedOrder.status
    } else if (showNotification) {
      ElMessage.error(response?.data?.msg || '检查支付状态失败')
    }
    return null
  } catch (error) {
    console.error('检查支付状态出错:', error)
    if (showNotification) {
      ElMessage.error('网络错误，请稍后重试')
    }
    return null
  }
}

// 在 checkLoginStatus 函数后面，onMounted 之前添加

// 定义消息处理函数
const messageHandler = async (event) => {
  // 检查消息来源和类型
  if (event.data && event.data.type === 'PAYMENT_COMPLETE') {
    console.log('收到支付完成消息:', event.data);
    
    // 刷新指定订单状态
    if (event.data.orderId) {
      await checkOrderPaymentStatus(event.data.orderId, true);
    }
    
    // 刷新订单列表
    fetchOrders();
    ElMessage.success('支付完成，订单状态已更新');
  }
};

// 页面加载时获取订单数据
onMounted(async () => {
  // 获取订单列表
  await fetchOrders();
  
  // 添加支付完成消息监听
  window.addEventListener('message', messageHandler);
  
  // 自动检查所有待支付订单的支付状态
  await checkAllPendingOrders();
});

// 添加检查所有待支付订单的函数
const checkAllPendingOrders = async () => {
  if (orders.value && orders.value.length > 0) {
    // 寻找所有待支付状态的订单
    const pendingOrders = orders.value.filter(order => order.status === 'PENDING');
    
    if (pendingOrders.length > 0) {
      ElMessage.info('正在检查订单支付状态...');
      
      // 创建检查所有待支付订单的Promise数组
      const checkPromises = pendingOrders.map(order => 
        checkOrderPaymentStatus(order.orderId, false)
      );
      
      // 并行检查所有待支付订单
      await Promise.all(checkPromises);
      
      // 刷新订单列表以获取最新状态
      await fetchOrders();
    }
  }
};

// 记得在组件卸载时清理
onBeforeUnmount(() => {
  window.removeEventListener('message', messageHandler);
});
</script>

<template>
  <div class="order-container">
    <div class="order-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><Document /></el-icon>
        我的订单
      </h1>
      <!-- 调试按钮 - 开发完成后可移除 -->
      <el-button @click="logOrderStatuses" type="info" size="small" style="margin-left: 15px;">
        调试信息
      </el-button>
    </div>
    
    <el-card class="order-content" v-loading="loading">
      <!-- 订单为空 -->
      <div v-if="!loading && (!orders || orders.length === 0)" class="empty-order">
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
                订单号：{{ order.orderId }}
              </div>
              <div class="order-date">
                下单时间：{{ formatDateTime(order.createTime) }}
              </div>
              <div class="order-status">
                <el-tag :color="getStatusColor(order.status)" effect="dark" size="small">
                  {{ formatOrderStatus(order.status) }}
                </el-tag>
              </div>
            </div>
            
            <!-- 移除订单商品部分 -->
            <div class="order-summary">
              <div class="order-details">
                <p>订单总金额: <span class="price">¥{{ order.totalAmount?.toFixed(2) || '0.00' }}</span></p>
                <p>支付方式: {{ order.paymentMethod || '未指定' }}</p>
                <p>订单状态: {{ formatOrderStatus(order.status) }}</p>
              </div>
            </div>
            
            <!-- 订单底部 - 移除items相关逻辑 -->
            <div class="order-footer">
              <div class="order-total">
                合计：<span class="total-price">¥{{ order.totalAmount?.toFixed(2) || '0.00' }}</span>
              </div>
              
              <!-- 订单操作按钮区域 -->
              <div class="order-actions">
                <!-- 待付款状态 -->
                <template v-if="matchOrderStatus(order.status, 'PENDING')">
                  <el-button size="small" @click="handleCancelOrder(order.orderId)">取消订单</el-button>
                  <!-- 降低存在感的检查按钮 -->
                  <el-tooltip content="如果您已完成支付但订单状态未更新，可点击此按钮刷新">
                    <el-button link type="info" @click="checkOrderPaymentStatus(order.orderId)">
                      刷新状态
                    </el-button>
                  </el-tooltip>
                  <el-button type="primary" size="small" @click="goToPayWithForm(order.orderId)">去支付</el-button>
                </template>
                
                <!-- 其他状态保持不变 -->
              </div>
            </div>
          </div>
        </div>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
/* 保留大部分样式，删除与items相关的样式 */
.order-container {
  padding: 2rem;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.order-header {
  margin-bottom: 2rem;
  display: flex;
  align-items: center;
}

.page-title {
  font-size: 1.8rem;
  color: #e74c3c;
  display: flex;
  align-items: center;
  margin: 0;
}

.title-icon {
  margin-right: 10px;
  font-size: 1.8rem;
}

.order-content {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* 调试面板 */
.debug-panel {
  background: #fffbeb;
  padding: 15px;
  margin-bottom: 20px;
  border-radius: 8px;
  border: 1px solid #ffeeba;
}

.debug-order {
  background: #fff;
  padding: 10px;
  margin: 5px 0;
  border-radius: 4px;
  border-left: 3px solid #e74c3c;
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

/* 订单摘要样式替代商品列表 */
.order-summary {
  padding: 20px;
  background-color: #fcfcfc;
  border-bottom: 1px solid #ebeef5;
}

.order-details {
  line-height: 1.8;
}

.price {
  color: #e74c3c;
  font-weight: bold;
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