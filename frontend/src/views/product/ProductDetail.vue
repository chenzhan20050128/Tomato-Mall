<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ShoppingCart, Star, StarFilled } from '@element-plus/icons-vue'
import { getProductDetail, Product } from '../../api/mall'
import { addToCart } from '../../api/cart'
import { getProductComments, getProductAverageRating, Comment, CommentStats, CommentResponse } from '../../api/comment'
import {reactive } from 'vue'
import { checkoutCart } from '../../api/cart' // 复用购物车的结算API

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const commentsLoading = ref(false)
const productId = computed(() => route.params.id as string)

// 标签页控制
const activeTab = ref('details')

// 商品详情 - 初始化为空对象
const product = ref<Product>({
  id: '',
  title: '',
  price: 0,
  rate: 0,
  description: '',
  cover: '',
  detail: '',
  specifications: [],
  stock: 100,
  isAvailable: true
})

// 数量选择
const quantity = ref(1)
const currentImage = ref(0)

// 评论数据 - 改为从后端获取
const reviews = ref<Comment[]>([])
const commentsPage = ref(0)
const commentsSize = ref(10)
const totalComments = ref(0)
const totalPages = ref(0)
const averageRating = ref(0)

// 评论统计计算属性
const reviewStats = computed<CommentStats>(() => {
  const total = totalComments.value
  const average = averageRating.value
  
  // 计算各星级的数量
  const starCounts = [0, 0, 0, 0, 0] // 1星到5星的数量
  reviews.value.forEach(review => {
    if (review.rating >= 1 && review.rating <= 5) {
      starCounts[review.rating - 1]++
    }
  })
  
  return {
    total,
    average: Number(average.toFixed(1)),
    starCounts
  }
})

// 获取评论统计信息（不加载具体评论内容）
const loadCommentStats = async () => {
  if (!productId.value) return
  
  try {
    // 获取第一页评论数据来获取总数统计
    const res = await getProductComments(productId.value, 0, 1) // 只获取1条记录来获取统计信息
    
    if (res.data) {
      totalComments.value = res.data.totalItems || 0
      totalPages.value = res.data.totalPages || 0
    }
  } catch (error) {
    console.error('获取评论统计失败:', error)
    // 不显示错误消息，因为这不是关键功能
  }
}

// 加载评论数据
const loadComments = async () => {
  if (!productId.value) return
  
  commentsLoading.value = true
  try {
    const res = await getProductComments(productId.value, commentsPage.value, commentsSize.value)
    
    if (res.data) {
      if (commentsPage.value === 0) {
        // 第一页，直接替换
        reviews.value = res.data.comments || []
      } else {
        // 后续页，追加到现有列表
        reviews.value.push(...(res.data.comments || []))
      }
      
      totalComments.value = res.data.totalItems || 0
      totalPages.value = res.data.totalPages || 0
    }
  } catch (error) {
    console.error('获取评论失败:', error)
    ElMessage.error('获取评论失败')
  } finally {
    commentsLoading.value = false
  }
}

// 加载平均评分
const loadAverageRating = async () => {
  if (!productId.value) return
  
  try {
    const res = await getProductAverageRating(productId.value)
    if (res.data !== undefined && res.data !== null) {
      averageRating.value = Number(res.data)
    }
  } catch (error) {
    console.error('获取平均评分失败:', error)
    // 不显示错误消息，因为可能是没有评论导致的正常情况
  }
}

// 监听标签页切换，当切换到评论页时加载评论
watch(activeTab, (newTab) => {
  if (newTab === 'reviews' && reviews.value.length === 0) {
    commentsPage.value = 0 // 重置页码
    loadComments()
  }
})

// 加载更多评论
const loadMoreComments = async () => {
  if (commentsPage.value + 1 >= totalPages.value) return
  
  commentsPage.value++
  await loadComments()
}

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 其他现有函数保持不变...
const increaseQuantity = () => {
  const stock = product.value.stock || 100
  if (quantity.value < stock) {
    quantity.value++
  } else {
    ElMessage.warning('已达到库存上限')
  }
}

const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

const setQuantity = (val: string | number) => {
  let num = typeof val === 'string' ? parseInt(val) : val
  const stock = product.value.stock || 100
  if (isNaN(num) || num < 1) {
    num = 1
  } else if (num > stock) {
    num = stock
    ElMessage.warning('已达到库存上限')
  }
  quantity.value = num
}

const handleAddToCart = async () => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  const stock = product.value.stock || 0
  if (stock <= 0 || !product.value.isAvailable) {
    ElMessage.warning('商品暂无库存')
    return
  }
  
  try {
    const res = await addToCart(product.value.id, quantity.value)
    if (res.data.code == 200) {
      ElMessage.success(`已将《${product.value.title}》×${quantity.value}添加到购物车`)
    } else {
      ElMessage.error(res.data.msg || '添加购物车失败')
    }
  } catch (error) {
    console.error('添加到购物车出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 添加立即购买相关状态
const buyNowLoading = ref(false)
const isBuyNowDialogVisible = ref(false)

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

// 修改buyNow函数 - 显示订单确认对话框
const buyNow = async () => {
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  const stock = product.value.stock || 0
  if (stock <= 0 || !product.value.isAvailable) {
    ElMessage.warning('商品暂无库存')
    return
  }
  
  if (quantity.value < 1) {
    ElMessage.warning('请选择有效数量')
    return
  }
  
  // 直接显示订单确认对话框
  isBuyNowDialogVisible.value = true
}

// 提交立即购买订单
const submitBuyNowOrder = async () => {
  // 验证表单
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
  
  buyNowLoading.value = true
  
  try {
    // 步骤1: 先添加到购物车（临时）
    ElMessage.info('正在准备订单...')
    const cartRes = await addToCart(product.value.id, quantity.value)
    if (cartRes.data.code != 200) {
      ElMessage.error(cartRes.data.msg || '添加购物车失败')
      return
    }
    
    // 获取刚添加的购物车项ID（假设后端返回了cartItemId）
    const cartItemId = cartRes.data.data.cartItemId
    
    // 步骤2: 立即结算（复用购物车的结算逻辑）
    const params = {
      cartItemIds: [cartItemId], // 只结算当前商品
      shipping_address: orderForm.shipping_address,
      payment_method: orderForm.payment_method
    }
    
    const res = await checkoutCart(params)
    if (res.data.code == 200) {
      isBuyNowDialogVisible.value = false
      const orderId = res.data.data?.orderId || res.data.data?.id
      if (!orderId) {
        ElMessage.error('订单创建失败，请稍后重试')
        return
      }
      // 显示成功消息并跳转到订单页面
      ElMessage.success('订单创建成功，正在跳转到订单页面')
      
      // 延迟一下再跳转，让用户能看到成功消息
      setTimeout(() => {
        goToPayWithForm(orderId);
      }, 1000)
    } else {
      ElMessage.error(res.data.msg || '提交订单失败')
    }
    
  } catch (error) {
    console.error('立即购买失败:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    buyNowLoading.value = false
  }
}

// 关闭对话框时重置表单
const closeBuyNowDialog = () => {
  isBuyNowDialogVisible.value = false
  // 可选：重置表单
  // Object.assign(orderForm.shipping_address, {
  //   name: '',
  //   phone: '',
  //   zipCode: '',
  //   address: '',
  // })
}

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
    
    console.log('准备跳转支付，订单ID:', orderId)
    
    // 创建表单并添加authorization字段
    const form = document.createElement('form')
    form.method = 'POST'
    form.action = `http://localhost:8080/api/orders/${orderId}/pay`
    form.target = '_blank'
    
    // 添加authorization字段
    const authField = document.createElement('input')
    authField.type = 'hidden'
    authField.name = 'authorization'
    authField.value = `Bearer ${token}`
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
    
    console.log('支付表单已提交到:', form.action)
    
    // 跳转到页面
    setTimeout(() => {
      router.push('/mall')
    }, 1500)
    
  } catch (error) {
    console.error('支付跳转失败:', error)
    ElMessage.error('支付跳转失败，请稍后重试')
  }
}



const goBack = () => {
  router.push('/mall')
}

const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

const productImage = computed(() => {
  return product.value.cover || '/placeholder.jpg'
})

const productImages = computed(() => {
  return [product.value.cover] 
})

const displayName = computed(() => {
  return product.value.title || '商品详情'
})

const getSpecValue = (item: string): string => {
  if (!product.value.specifications) return '';
  const spec = product.value.specifications.find(s => s.item === item);
  return spec ? spec.value : '';
}

const author = computed(() => getSpecValue('作者'))
const isbn = computed(() => getSpecValue('ISBN'))
const format = computed(() => getSpecValue('装帧'))
const pages = computed(() => getSpecValue('页数'))
const publisher = computed(() => getSpecValue('出版社'))
const publishDate = computed(() => getSpecValue('出版日期'))

const originalPrice = computed(() => {
  return product.value.price * 1.2;
})

const discount = computed(() => {
  return Math.round((1 - product.value.price / originalPrice.value) * 100);
})

const averageRatingValue = computed(() => {
  return reviewStats.value.average || 0;
})

const averageRatingDisplay = computed(() => {
  return reviewStats.value.average.toFixed(1);
})

const productStock = computed(() => product.value.stock || 0)
const hasStock = computed(() => productStock.value > 0)
const hasHighStock = computed(() => productStock.value > 10)

const changeImage = (index: number) => {
  currentImage.value = index
}

const handleImageError = (event: Event) => {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = '/placeholder.jpg'
  }
}

const loadProductDetail = async () => {
  loading.value = true
  
  try {
    const res = await getProductDetail(productId.value)
    if (res.data.code == 200 && res.data.data) {
      product.value = res.data.data
      
      if (product.value.stock === undefined) {
        product.value.stock = 100
      }
      if (product.value.isAvailable === undefined) {
        product.value.isAvailable = true
      }
      
      quantity.value = 1
      
      // 同时加载平均评分和评论统计
      await Promise.all([
        loadAverageRating(),
        loadCommentStats()
      ])
    } else {
      ElMessage.error(res.data.msg || '获取商品详情失败')
    }
  } catch (error) {
    console.error('获取商品详情出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProductDetail()
})
</script>

<template>
  <div class="product-detail-container">
    <!-- 顶部导航 -->
    <div class="breadcrumb">
      <el-button type="text" @click="goBack" class="back-button">
        <el-icon><ArrowLeft /></el-icon>
        返回商城
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/mall' }">图书商城</el-breadcrumb-item>
        <el-breadcrumb-item>{{ displayName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-card v-loading="loading" element-loading-text="加载商品详情...">
      <div class="product-main">
        <!-- 左侧图片区域 -->
        <div class="product-gallery">
          <div class="main-image">
            <img 
              :src="productImages[currentImage] || ''" 
              :alt="displayName"
              @error="handleImageError"
              referrerpolicy="no-referrer"
            >
          </div>
          <div class="thumbnail-list" v-if="productImages.length > 1">
            <div 
              v-for="(img, index) in productImages" 
              :key="index"
              :class="['thumbnail', { active: currentImage === index }]"
              @click="changeImage(index)"
            >
              <img :src="img" :alt="`${displayName} - 图片 ${index + 1}`" @error="handleImageError">
            </div>
          </div>
        </div>

        <!-- 右侧信息区域 -->
        <div class="product-info">
          <h1 class="product-title">{{ displayName }}</h1>
          <div class="product-author" v-if="author">作者: {{ author }}</div>
          
          <div class="product-rating" v-if="product.rate || reviewStats.total > 0">
            <span class="rating-stars">
              <el-icon v-for="i in 5" :key="i" :class="i <= averageRatingValue ? 'star-filled' : 'star-empty'">
                <component :is="i <= averageRatingValue ? StarFilled : Star" />
              </el-icon>
            </span>
            <span class="rating-score">{{ averageRating }}</span>
            <span class="rating-count" v-if="reviewStats.total > 0">({{ reviewStats.total }}条评价)</span>
          </div>
          
          <div class="product-price-area">
            <div class="price-wrapper">
              <span class="current-price">{{ formatPrice(product.price) }}</span>
              <span class="original-price">
                {{ formatPrice(originalPrice) }}
              </span>
              <span class="discount-tag">{{ discount }}% 优惠</span>
            </div>
          </div>
          
          <div class="product-meta">
            <div class="meta-item" v-if="publisher">
              <span class="meta-label">出版社:</span>
              <span class="meta-value">{{ publisher }}</span>
            </div>
            <div class="meta-item" v-if="publishDate">
              <span class="meta-label">出版日期:</span>
              <span class="meta-value">{{ publishDate }}</span>
            </div>
            <div class="meta-item" v-if="pages">
              <span class="meta-label">页数:</span>
              <span class="meta-value">{{ pages }}页</span>
            </div>
            <div class="meta-item" v-if="isbn">
              <span class="meta-label">ISBN:</span>
              <span class="meta-value">{{ isbn }}</span>
            </div>
          </div>
          
          <div class="product-stock">
            <span :class="['stock-status', hasHighStock ? 'in-stock' : 'low-stock']">
              {{ hasStock ? '有货' : '缺货' }}
            </span>
            <span class="stock-count" v-if="hasStock">
              {{ hasHighStock ? '库存充足' : `仅剩${productStock}件` }}
            </span>
          </div>
          
          <div class="product-actions">
            <div class="quantity-control">
              <span class="quantity-label">数量:</span>
              <el-input-number 
                v-model="quantity" 
                :min="1" 
                :max="productStock" 
                :disabled="!hasStock || !product.isAvailable" 
                @change="setQuantity"
              />
            </div>
            
            <div class="action-buttons">
              <el-button 
                type="primary" 
                :icon="ShoppingCart" 
                :disabled="!hasStock || !product.isAvailable" 
                @click="handleAddToCart" 
                class="cart-button"
              >
                加入购物车
              </el-button>
              
              <el-button 
                type="danger" 
                :disabled="!hasStock || !product.isAvailable" 
                @click="buyNow" 
                class="buy-button"
              >
                立即购买
              </el-button>
            </div>

          <!-- 立即购买订单确认对话框 -->
            <el-dialog
              v-model="isBuyNowDialogVisible"
              title="确认订单"
              width="500px"
              @close="closeBuyNowDialog"
            >
              <el-form :model="orderForm" label-width="120px">
                <!-- 商品信息预览 -->
                <div class="checkout-products">
                  <h4>购买商品</h4>
                  <div class="checkout-product-item">
                    <div class="checkout-product-info">
                      <div class="product-preview">
                        <img :src="product.cover" :alt="product.title" class="product-preview-image" @error="handleImageError">
                        <div class="product-preview-details">
                          <div class="checkout-product-title">{{ product.title }}</div>
                          <div class="checkout-product-price">¥{{ product.price.toFixed(2) }} × {{ quantity }}</div>
                        </div>
                      </div>
                    </div>
                    <div class="checkout-product-subtotal">
                      ¥{{ (product.price * quantity).toFixed(2) }}
                    </div>
                  </div>
                  <div class="checkout-total">
                    <span>总计：</span>
                    <span class="checkout-total-amount">¥{{ (product.price * quantity).toFixed(2) }}</span>
                  </div>
                </div>
                
                <el-divider />
                
                <!-- 收货信息 -->
                <h4>收货信息</h4>
                <el-form-item label="收货人姓名" required>
                  <el-input v-model="orderForm.shipping_address.name" placeholder="请输入收货人姓名" />
                </el-form-item>
                <el-form-item label="联系电话" required>
                  <el-input v-model="orderForm.shipping_address.phone" placeholder="请输入联系电话" />
                </el-form-item>
                <el-form-item label="邮政编码">
                  <el-input v-model="orderForm.shipping_address.zipCode" placeholder="请输入邮政编码（可选）" />
                </el-form-item>
                <el-form-item label="收货地址" required>
                  <el-input 
                    v-model="orderForm.shipping_address.address" 
                    type="textarea" 
                    rows="3" 
                    placeholder="请输入详细收货地址"
                  />
                </el-form-item>
                
                <el-divider />
                
                <!-- 支付方式 -->
                <h4>支付方式</h4>
                <el-form-item label="支付方式">
                  <el-radio-group v-model="orderForm.payment_method">
                    <el-radio label="ALIPAY">支付宝</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-form>
              
              <template #footer>
                <span>
                  <el-button @click="closeBuyNowDialog">取消</el-button>
                  <el-button 
                    type="primary" 
                    @click="submitBuyNowOrder" 
                    :loading="buyNowLoading"
                  >
                    提交订单
                  </el-button>
                </span>
              </template>
            </el-dialog>

          </div>
        </div>
      </div>
      
      <!-- 新增：标签页区域 -->
      <div class="product-tabs">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="图书详情" name="details">
            <div class="details-content">
              <p class="description">{{ product.description }}</p>
              <p class="detail" v-if="product.detail">{{ product.detail }}</p>
              
              <div class="spec-table" v-if="product.specifications && product.specifications.length > 0">
                <h3>商品规格</h3>
                <table>
                  <tr v-for="spec in product.specifications" :key="spec.id">
                    <th>{{ spec.item }}</th>
                    <td>{{ spec.value }}</td>
                  </tr>
                </table>
              </div>
            </div>
          </el-tab-pane>
          
          <el-tab-pane name="reviews">
            <template #label>
              <span>用户评价 <span class="review-count">({{ reviewStats.total }})</span></span>
            </template>
            
            <!-- 评价统计区域 -->
            <div class="review-summary">
              <div class="rating-overview">
                <div class="rating-big">
                  <span class="rating-num">{{ averageRating }}</span>
                  <div class="rating-stars-big">
                    <el-icon v-for="i in 5" :key="i" :class="i <= averageRatingValue ? 'star-filled' : 'star-empty'">
                      <component :is="i <= averageRatingValue ? StarFilled : Star" />
                    </el-icon>
                  </div>
                  <div class="reviews-count">{{ reviewStats.total }}条评价</div>
                </div>
                
                <!-- 星级分布 -->
                <div class="rating-distribution">
                  <div v-for="(count, index) in reviewStats.starCounts.slice().reverse()" :key="5-index" class="rating-bar">
                    <span class="rating-level">{{ 5-index }}星</span>
                    <div class="progress-bar">
                      <div 
                        class="progress-fill" 
                        :style="{ width: reviewStats.total > 0 ? (count / reviewStats.total * 100) + '%' : '0%' }"
                      ></div>
                    </div>
                    <span class="rating-count">{{ count }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 评价列表 -->
            <div class="reviews-list">
              <div v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-header">
                  <div class="review-user">
                    <img :src="review.userAvatar" class="user-avatar" @error="handleImageError">
                    <div class="user-info">
                      <span class="username">{{ review.username }}</span>
                      <div class="review-rating">
                        <span class="rating-stars">
                          <el-icon v-for="i in 5" :key="i" :class="i <= review.rating ? 'star-filled' : 'star-empty'">
                            <component :is="i <= review.rating ? StarFilled : Star" />
                          </el-icon>
                        </span>
                        <span class="review-date">{{ review.createdAt }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="review-content">
                  <p class="review-text">{{ review.content }}</p>
                </div>
              </div>
            </div>
            
            <!-- 暂无评价提示 -->
            <div v-if="reviewStats.total === 0" class="no-reviews">
              <el-empty description="暂无用户评价" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* 现有样式保持不变... */
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  background: #f8f9fa;
  min-height: 100vh;
}

.breadcrumb {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.back-button {
  display: flex;
  align-items: center;
  color: #606266;
}

.back-button:hover {
  color: #e74c3c;
}

.product-main {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
}

.product-gallery {
  flex: 0 0 40%;
}

.main-image {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

.thumbnail-list {
  display: flex;
  gap: 8px;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.thumbnail {
  width: 80px;
  height: 80px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail.active {
  border-color: #e74c3c;
}

.thumbnail img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
}

.product-info {
  flex: 1;
}

.product-title {
  font-size: 1.8rem;
  margin: 0 0 0.5rem;
  line-height: 1.3;
  color: #303133;
}

.product-author {
  color: #606266;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.product-rating {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

.rating-stars {
  display: flex;
  color: #e74c3c;
  margin-right: 8px;
}

.star-filled {
  color: #e74c3c;
}

.star-empty {
  color: #C0C4CC;
}

.rating-score {
  font-weight: bold;
  margin-right: 4px;
  color: #e74c3c;
}

.rating-count {
  color: #909399;
  font-size: 0.9rem;
}

.product-price-area {
  background: #fff9f9;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
}

.price-wrapper {
  display: flex;
  align-items: center;
}

.current-price {
  font-size: 2rem;
  font-weight: bold;
  color: #e74c3c;
  margin-right: 10px;
}

.original-price {
  color: #909399;
  text-decoration: line-through;
  font-size: 1rem;
  margin-right: 10px;
}

.discount-tag {
  background: #e74c3c;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
}

.product-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 1.5rem;
}

.meta-item {
  display: flex;
  font-size: 0.9rem;
}

.meta-label {
  color: #909399;
  margin-right: 8px;
  min-width: 70px;
}

.meta-value {
  color: #606266;
}

.product-stock {
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  gap: 10px;
}

.stock-status {
  font-weight: bold;
}

.in-stock {
  color: #67c23a;
}

.low-stock {
  color: #e6a23c;
}

.stock-count {
  color: #606266;
  font-size: 0.9rem;
}

.product-actions {
  margin-top: 2rem;
}

.quantity-control {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

.quantity-label {
  margin-right: 1rem;
  color: #606266;
}

.action-buttons {
  display: flex;
  gap: 1rem;
}

.cart-button {
  background-color: #e74c3c;
  border-color: #e74c3c;
}

.cart-button:hover {
  background-color: #c0392b;
  border-color: #c0392b;
}

.buy-button {
  background-color: #e67e22;
  border-color: #e67e22;
}

.buy-button:hover {
  background-color: #d35400;
  border-color: #d35400;
}

/* 新增：标签页相关样式 */
.product-tabs {
  margin-top: 2rem;
  border-top: 1px solid #ebeef5;
  padding-top: 1rem;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 1.5rem;
}

.detail-tabs :deep(.el-tabs__item) {
  font-size: 1.1rem;
  font-weight: 500;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  color: #e74c3c;
}

.detail-tabs :deep(.el-tabs__active-bar) {
  background-color: #e74c3c;
}

.review-count {
  color: #e74c3c;
  font-weight: bold;
}

.details-content {
  padding: 1rem 0;
}

.description {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 1.5rem;
  font-size: 1rem;
}

.detail {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.spec-table {
  margin-top: 2rem;
}

.spec-table h3 {
  color: #303133;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.spec-table table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.spec-table th, .spec-table td {
  border-bottom: 1px solid #ebeef5;
  padding: 12px 15px;
  text-align: left;
}

.spec-table th {
  background: #f8f9fa;
  width: 120px;
  color: #606266;
  font-weight: 500;
}

.spec-table td {
  color: #303133;
}

/* 评价区域样式 */
.review-summary {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.rating-overview {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
}

.rating-big {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
}

.rating-num {
  font-size: 3rem;
  font-weight: bold;
  color: #e74c3c;
  line-height: 1;
}

.rating-stars-big {
  display: flex;
  margin: 0.5rem 0;
  font-size: 1.2rem;
}

.reviews-count {
  color: #909399;
  font-size: 0.9rem;
}

.rating-distribution {
  flex: 1;
  max-width: 300px;
}

.rating-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.rating-level {
  width: 30px;
  font-size: 0.9rem;
  color: #606266;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #e74c3c;
  transition: width 0.3s ease;
}

.rating-bar .rating-count {
  width: 20px;
  text-align: right;
  font-size: 0.9rem;
  color: #909399;
}

.reviews-list {
  margin-top: 1rem;
}

.review-item {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.review-header {
  margin-bottom: 1rem;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
}

.username {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  display: block;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-date {
  color: #909399;
  font-size: 0.85rem;
}

.review-content {
  margin-left: 52px;
}

.review-text {
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.no-reviews {
  padding: 2rem 0;
  text-align: center;
}

@media (max-width: 768px) {
  .product-detail-container {
    padding: 1rem;
  }
  
  .product-main {
    flex-direction: column;
  }
  
  .product-gallery {
    flex: 0 0 100%;
  }
  
  .main-image {
    height: 350px;
  }
  
  .product-meta {
    grid-template-columns: 1fr;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .rating-overview {
    flex-direction: column;
    gap: 1rem;
  }
  
  .rating-big {
    align-self: center;
  }
  
  .review-content {
    margin-left: 0;
    margin-top: 1rem;
  }
  
  .spec-table th {
    width: 100px;
  }
}


</style>