<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Warning } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const paymentStatus = ref('processing')
const paymentInfo = ref<any>({})

onMounted(() => {
  // 从URL获取token参数
  const urlToken = route.query.token
  
  // 如果URL中有token，则恢复登录状态
  if (urlToken && typeof urlToken === 'string') {
    sessionStorage.setItem('token', urlToken)
    // 可选：验证token有效性
    verifyToken(urlToken)
  }
  
  // 其他初始化代码...
})

// 验证token有效性
const verifyToken = async (token) => {
  try {
    // 调用验证token的API
    const response = await fetch('/api/users/validate-token', {
      headers: { 'Authorization': `Bearer ${token}` }
    })
    
    if (!response.ok) {
      // token无效，跳转到登录页
      router.push('/login')
    }
  } catch (error) {
    console.error('验证token失败:', error)
  }
}

// 检查支付状态
const checkPaymentStatus = async (orderNumber) => {
  try {
    const response = await fetch(`/api/orders/${orderNumber}/status`, {
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('token')}`
      }
    })
    
    if (response.ok) {
      const result = await response.json()
      
      if (result.code === '200' && result.data) {
        const orderStatus = result.data.status
        
        // 处理不同的订单状态
        if (orderStatus === 'PAID' || orderStatus === 'PROCESSING' || 
            orderStatus === 'SHIPPED' || orderStatus === 'COMPLETED') {
          paymentStatus.value = 'success'
          ElMessage.success('订单支付成功！')
        } else if (orderStatus === 'PENDING_PAYMENT') {
          paymentStatus.value = 'pending'
          // 添加轮询逻辑，因为异步通知可能有延迟
          setTimeout(() => checkPaymentStatus(orderNumber), 3000)
        } else {
          paymentStatus.value = 'error'
        }
      } else {
        paymentStatus.value = 'error'
      }
    } else {
      paymentStatus.value = 'error'
    }
  } catch (error) {
    console.error('检查支付状态失败:', error)
    paymentStatus.value = 'error'
  }
}

// 回到订单页面
const goToOrders = () => {
  router.push('/order')
}
</script>

<template>
  <div class="payment-result">
    <h1 class="title">支付结果</h1>
    
    <div v-if="paymentStatus === 'processing'" class="status-card processing">
      <el-icon class="status-icon"><Loading /></el-icon>
      <h2>处理中...</h2>
      <p>正在查询支付结果，请稍候</p>
    </div>
    
    <div v-else-if="paymentStatus === 'success'" class="status-card success">
      <el-icon class="status-icon"><CircleCheckFilled /></el-icon>
      <h2>支付成功</h2>
      <p>您的订单已支付成功</p>
      <div class="payment-info">
        <p>订单号: {{ paymentInfo.outTradeNo }}</p>
        <p>交易号: {{ paymentInfo.tradeNo }}</p>
        <p>支付金额: ¥{{ paymentInfo.totalAmount }}</p>
      </div>
      <el-button type="primary" @click="goToOrders">查看我的订单</el-button>
    </div>
    
    <div v-else-if="paymentStatus === 'pending'" class="status-card pending">
      <el-icon class="status-icon"><Warning /></el-icon>
      <h2>支付处理中</h2>
      <p>您的支付请求已提交，但还在处理中</p>
      <p>请稍后在订单列表中查看支付状态</p>
      <el-button type="primary" @click="goToOrders">返回订单列表</el-button>
    </div>
    
    <div v-else class="status-card error">
      <el-icon class="status-icon"><CircleCloseFilled /></el-icon>
      <h2>支付状态未知</h2>
      <p>无法获取支付结果，请查看订单列表确认支付状态</p>
      <el-button type="primary" @click="goToOrders">返回订单列表</el-button>
    </div>
  </div>
</template>

<style scoped>
.payment-result {
  max-width: 600px;
  margin: 50px auto;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
}

.status-card {
  padding: 30px;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.status-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.success {
  background-color: #f0f9eb;
}

.success .status-icon {
  color: #67c23a;
}

.processing {
  background-color: #ecf5ff;
}

.processing .status-icon {
  color: #409eff;
}

.pending {
  background-color: #fdf6ec;
}

.pending .status-icon {
  color: #e6a23c;
}

.error {
  background-color: #fef0f0;
}

.error .status-icon {
  color: #f56c6c;
}

.payment-info {
  margin: 20px 0;
  padding: 15px;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 6px;
  text-align: left;
}
</style>