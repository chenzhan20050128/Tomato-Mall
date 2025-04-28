<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Minus, Plus, ShoppingCart, Warning, ShoppingBag } from '@element-plus/icons-vue'
import { getCartItems, removeFromCart, updateCartItemQuantity, checkoutCart, CartItem } from '../../api/cart'
// 导入事件总线
import emitter from '../../utils/eventBus'

const router = useRouter()
const loading = ref(true)
const cartItems = ref<CartItem[]>([])
const selectedItems = ref<string[]>([])
const selectAll = ref(false)
const isCheckoutDialogVisible = ref(false)

// 订单表单数据
const orderForm = reactive({
  shipping_address: {
    name: '',
    phone: '',
    zipCode: '',
    address: '',
  },
  payment_method: 'ALIPAY'
})

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('token')
  
  // 如果未登录，提示并重定向到登录页
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// 获取购物车数据
const fetchCartItems = async () => {
  if (!checkLoginStatus()) return
  
  loading.value = true
  try {
    const res = await getCartItems()
    if (res.data.code == 200) {
      cartItems.value = res.data.data.items || []
      selectedItems.value = []
      selectAll.value = false
    } else {
      ElMessage.error(res.data.msg || '获取购物车失败')
    }
  } catch (error) {
    console.error('获取购物车数据出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 选中或取消选中所有商品
const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedItems.value = cartItems.value.map(item => item.cartItemId)
  } else {
    selectedItems.value = []
  }
}

// 监听选中商品的变化，更新全选状态
const updateSelectAllStatus = () => {
  selectAll.value = cartItems.value.length > 0 && selectedItems.value.length === cartItems.value.length
}

// 移除购物车商品
const handleRemoveItem = async (cartItemId: string) => {
  ElMessageBox.confirm(
    '确定要从购物车中移除该商品吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await removeFromCart(cartItemId)
      if (res.data.code == 200) {
        ElMessage.success('商品已从购物车移除')
        // 更新本地数据
        cartItems.value = cartItems.value.filter(item => item.cartItemId !== cartItemId)
        // 更新选中状态
        selectedItems.value = selectedItems.value.filter(id => id !== cartItemId)
        updateSelectAllStatus()
        // 通知其他组件更新购物车数量
        emitter.emit('updateCartCount')
      } else {
        ElMessage.error(res.data.msg || '移除商品失败')
      }
    } catch (error) {
      console.error('移除商品出错:', error)
      ElMessage.error('网络错误，请稍后重试')
    }
  }).catch(() => {})
}

// 更新商品数量
const updateQuantity = async (cartItemId: string, quantity: number) => {
  if (quantity < 1) {
    ElMessage.warning('数量不能小于1')
    return
  }
  
  try {
    const res = await updateCartItemQuantity(cartItemId, quantity)
    if (res.data.code == 200) {
      // 更新本地数据
      const item = cartItems.value.find(item => item.cartItemId === cartItemId)
      if (item) {
        item.quantity = quantity
        // 通知其他组件更新购物车数量
        emitter.emit('updateCartCount')
      }
    } else {
      ElMessage.error(res.data.msg || '更新数量失败')
    }
  } catch (error) {
    console.error('更新商品数量出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 增加商品数量
const increaseQuantity = async (item: CartItem) => {
  await updateQuantity(item.cartItemId, item.quantity + 1)
}

// 减少商品数量
const decreaseQuantity = async (item: CartItem) => {
  if (item.quantity > 1) {
    await updateQuantity(item.cartItemId, item.quantity - 1)
  }
}

// 继续购物
const continueShopping = () => {
  router.push('/mall')
}

// 计算选中商品总价
const selectedTotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItems.value.includes(item.cartItemId))
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
    .toFixed(2)
})

// 计算选中商品数量
const selectedCount = computed(() => {
  return cartItems.value
    .filter(item => selectedItems.value.includes(item.cartItemId))
    .reduce((sum, item) => sum + item.quantity, 0)
})

// 结算 - 打开结算对话框
const openCheckoutDialog = () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  isCheckoutDialogVisible.value = true
}

// 提交订单
const submitOrder = async () => {
  if (!orderForm.shipping_address.name) {
    ElMessage.warning('请输入收货人姓名')
    return
  }
  if (!orderForm.shipping_address.phone) {
    ElMessage.warning('请输入联系电话')
    return
  }
  if (!orderForm.shipping_address.address) {
    ElMessage.warning('请输入收货地址')
    return
  }
  
  try {
    const params = {
      cartItemIds: selectedItems.value,
      shipping_address: orderForm.shipping_address,
      payment_method: orderForm.payment_method
    }
    
    const res = await checkoutCart(params)
    if (res.data.code == 200) {
      ElMessage.success('订单提交成功，即将跳转支付页面')
      isCheckoutDialogVisible.value = false
      
      // 结算成功后刷新购物车数据
      await fetchCartItems()
      // 通知其他组件更新购物车数量
      emitter.emit('updateCartCount')
    } else {
      ElMessage.error(res.data.msg || '提交订单失败')
    }
  } catch (error) {
    console.error('提交订单出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 图片加载错误处理
const handleImageError = (event) => {
  event.target.src = '/placeholder.jpg'
}

// 页面加载时获取购物车数据
onMounted(() => {
  fetchCartItems()
})
</script>

<template>
  <div class="cart-container">
    <div class="cart-header">
      <h1 class="page-title">
        <el-icon class="title-icon"><ShoppingCart /></el-icon>
        我的购物车
      </h1>
    </div>
    
    <el-card class="cart-content" v-loading="loading">
      <!-- 购物车为空 -->
      <div v-if="!loading && cartItems.length === 0" class="empty-cart">
        <el-empty description="购物车还是空的">
          <template #image>
            <el-icon class="empty-icon"><ShoppingCart /></el-icon>
          </template>
          <template #description>
            <p>您的购物车还没有商品</p>
          </template>
          <el-button type="primary" @click="continueShopping">
            <el-icon><ShoppingBag /></el-icon>
            去选购图书
          </el-button>
        </el-empty>
      </div>
      
      <!-- 购物车有商品 -->
      <template v-else-if="!loading">
        <!-- 购物车列表标题 -->
        <div class="cart-table-header">
          <div class="check-col">
            <el-checkbox v-model="selectAll" @change="toggleSelectAll" />
          </div>
          <div class="item-col">商品信息</div>
          <div class="price-col">单价</div>
          <div class="quantity-col">数量</div>
          <div class="subtotal-col">小计</div>
          <div class="action-col">操作</div>
        </div>
        
        <!-- 购物车商品列表 -->
        <div class="cart-items">
          <div v-for="item in cartItems" :key="item.cartItemId" class="cart-item">
            <div class="check-col">
              <el-checkbox 
                v-model="selectedItems" 
                :label="item.cartItemId" 
                @change="updateSelectAllStatus" 
              />
            </div>
            
            <div class="item-col">
              <div class="item-info">
                <div class="item-image">
                  <img 
                    :src="item.cover" 
                    :alt="item.title"
                    @error="handleImageError"
                  >
                </div>
                <div class="item-details">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-desc">{{ item.description }}</div>
                </div>
              </div>
            </div>
            
            <div class="price-col">
              ¥{{ item.price.toFixed(2) }}
            </div>
            
            <div class="quantity-col">
              <div class="quantity-control">
                <el-button 
                  type="primary"
                  :icon="Minus"
                  size="small"
                  circle
                  plain
                  :disabled="item.quantity <= 1"
                  @click="decreaseQuantity(item)"
                />
                <span class="quantity-display">{{ item.quantity }}</span>
                <el-button 
                  type="primary"
                  :icon="Plus"
                  size="small"
                  circle
                  plain
                  @click="increaseQuantity(item)"
                />
              </div>
            </div>
            
            <div class="subtotal-col">
              <span class="subtotal-amount">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
            
            <div class="action-col">
              <el-button 
                type="danger"
                :icon="Delete"
                circle
                plain
                size="small"
                @click="handleRemoveItem(item.cartItemId)"
              />
            </div>
          </div>
        </div>
        
        <!-- 购物车底部结算区 -->
        <div class="cart-footer">
          <div class="footer-left">
            <el-checkbox 
              v-model="selectAll" 
              @change="toggleSelectAll"
            >全选</el-checkbox>
          </div>
          
          <div class="footer-right">
            <div class="selected-info">
              已选 <span class="highlight">{{ selectedCount }}</span> 件商品，
              合计：<span class="total-price">¥{{ selectedTotal }}</span>
            </div>
            <el-button 
              type="primary"
              :disabled="selectedItems.length === 0"
              @click="openCheckoutDialog"
            >
              去结算
            </el-button>
          </div>
        </div>
      </template>
    </el-card>
    
    <!-- 结算对话框 -->
    <el-dialog
      v-model="isCheckoutDialogVisible"
      title="确认订单"
      width="500px"
    >
      <el-form :model="orderForm" label-width="120px">
        <div class="checkout-products">
          <h4>已选商品</h4>
          <div class="checkout-product-list">
            <div v-for="item in cartItems.filter(item => selectedItems.includes(item.cartItemId))" 
                :key="item.cartItemId" 
                class="checkout-product-item">
              <div class="checkout-product-info">
                <div class="checkout-product-title">{{ item.title }}</div>
                <div class="checkout-product-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
              </div>
              <div class="checkout-product-subtotal">
                ¥{{ (item.price * item.quantity).toFixed(2) }}
              </div>
            </div>
          </div>
          <div class="checkout-total">
            <span>总计：</span>
            <span class="checkout-total-amount">¥{{ selectedTotal }}</span>
          </div>
        </div>
        
        <el-divider />
        
        <h4>收货信息</h4>
        <el-form-item label="收货人姓名">
          <el-input v-model="orderForm.shipping_address.name" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="orderForm.shipping_address.phone" />
        </el-form-item>
        <el-form-item label="邮政编码">
          <el-input v-model="orderForm.shipping_address.zipCode" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="orderForm.shipping_address.address" type="textarea" rows="2" />
        </el-form-item>
        
        <el-divider />
        
        <h4>支付方式</h4>
        <el-form-item label="支付方式">
          <el-radio-group v-model="orderForm.payment_method">
            <el-radio label="ALIPAY">支付宝</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span>
          <el-button @click="isCheckoutDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitOrder">提交订单</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>
<style scoped>
.cart-container {
  padding: 2rem;
  background-color: #f5f7fa;
  background-image: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
  min-height: calc(100vh - 60px);
}

.cart-header {
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

.cart-content {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

/* 空购物车样式 */
.empty-cart {
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

/* 购物车表格样式 */
.cart-table-header {
  display: flex;
  align-items: center;
  padding: 1rem;
  background-color: #f5f7fa;
  font-weight: bold;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

.cart-items {
  margin-bottom: 1rem;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 1.5rem 1rem;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.3s;
}

.cart-item:hover {
  background-color: #f9f9f9;
}

.check-col {
  flex: 0 0 5%;
  display: flex;
  justify-content: center;
}

.item-col {
  flex: 0 0 50%;
}

.price-col, .subtotal-col {
  flex: 0 0 10%;
  text-align: center;
}

.subtotal-amount {
  color: #e74c3c;
  font-weight: bold;
}

.quantity-col {
  flex: 0 0 15%;
  display: flex;
  justify-content: center;
}

.action-col {
  flex: 0 0 10%;
  display: flex;
  justify-content: center;
}

/* 商品信息样式 */
.item-info {
  display: flex;
  align-items: center;
}

.item-image {
  width: 100px;
  height: 100px;
  margin-right: 1rem;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  overflow: hidden;
}

.item-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.item-details {
  flex: 1;
}

.item-title {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.item-desc {
  color: #909399;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 数量控制 */
.quantity-control {
  display: flex;
  align-items: center;
}

.quantity-display {
  width: 30px;
  text-align: center;
  margin: 0 0.5rem;
}

/* 底部结算栏 */
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-top: 1px solid #ebeef5;
}

.footer-left {
  flex: 1;
}

.footer-right {
  display: flex;
  align-items: center;
}

.selected-info {
  margin-right: 1rem;
}

.highlight {
  color: #e74c3c;
  font-weight: bold;
}

.total-price {
  color: #e74c3c;
  font-size: 1.4rem;
  font-weight: bold;
}

/* 结算对话框样式 */
.checkout-products {
  margin-bottom: 1.5rem;
}

.checkout-products h4 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #303133;
}

.checkout-product-list {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 1rem;
}

.checkout-product-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.8rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px dashed #ebeef5;
}

.checkout-product-title {
  font-weight: 500;
  margin-bottom: 5px;
}

.checkout-product-price {
  color: #909399;
  font-size: 0.9rem;
}

.checkout-product-subtotal {
  color: #e74c3c;
  font-weight: 500;
}

.checkout-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 1rem;
  font-weight: bold;
}

.checkout-total-amount {
  color: #e74c3c;
  font-size: 1.2rem;
  margin-left: 0.5rem;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .cart-container {
    padding: 1rem;
  }
  
  .cart-item {
    flex-wrap: wrap;
  }
  
  .check-col {
    flex: 0 0 10%;
  }
  
  .item-col {
    flex: 0 0 90%;
    margin-bottom: 1rem;
  }
  
  .price-col, .quantity-col, .subtotal-col {
    flex: 1;
  }
  
  .action-col {
    flex: 0 0 20%;
  }
  
  .footer-right {
    flex-direction: column;
    align-items: flex-end;
  }
  
  .selected-info {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }
}
</style>