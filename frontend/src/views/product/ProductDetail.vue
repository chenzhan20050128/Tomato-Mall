<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ShoppingCart, Star, StarFilled } from '@element-plus/icons-vue'
import { getProductDetail, Product } from '../../api/mall'
import { addToCart } from '../../api/cart'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const productId = computed(() => route.params.id as string)

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
  stock: 100,  // 默认库存
  isAvailable: true
})

// 数量选择
const quantity = ref(1)
const currentImage = ref(0)

// 评论数据
const reviews = ref<Array<{
  id: number;
  avatar: string;
  username: string;
  rating: number;
  date: string;
  content: string;
}>>([])

// 增加数量
const increaseQuantity = () => {
  const stock = product.value.stock || 100
  if (quantity.value < stock) {
    quantity.value++
  } else {
    ElMessage.warning('已达到库存上限')
  }
}

// 减少数量
const decreaseQuantity = () => {
  if (quantity.value > 1) {
    quantity.value--
  }
}

// 设置数量
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

// 添加到购物车
const handleAddToCart = async () => {
  // 检查登录状态
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // 检查库存
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

// 立即购买
const buyNow = () => {
  // 检查登录状态
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  ElMessage.info('购买功能开发中，敬请期待')
}

// 返回商城
const goBack = () => {
  router.push('/mall')
}

// 格式化价格
const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

// 指定图片 - 默认只有一张封面图
const productImage = computed(() => {
  return product.value.cover || '/placeholder.jpg'
})

// 商品图片数组
const productImages = computed(() => {
  // 这里仅使用封面图片
  return [product.value.cover] 
})

// 显示的商品名称
const displayName = computed(() => {
  return product.value.title || '商品详情'
})

// 从规格中获取特定信息
const getSpecValue = (item: string): string => {
  if (!product.value.specifications) return '';
  const spec = product.value.specifications.find(s => s.item === item);
  return spec ? spec.value : '';
}

// 提取各种规格信息
const author = computed(() => getSpecValue('作者'))
const isbn = computed(() => getSpecValue('ISBN'))
const format = computed(() => getSpecValue('装帧'))
const pages = computed(() => getSpecValue('页数'))
const publisher = computed(() => getSpecValue('出版社'))
const publishDate = computed(() => getSpecValue('出版日期'))

// 原价（用于计算折扣）
const originalPrice = computed(() => {
  // 假设原价比当前价格高20%
  return product.value.price * 1.2;
})

// 计算折扣
const discount = computed(() => {
  return Math.round((1 - product.value.price / originalPrice.value) * 100);
})

// 总评分（数字类型，用于计算）
const averageRatingValue = computed(() => {
  return product.value.rate || 0;
})

// 格式化的评分
const averageRating = computed(() => {
  return product.value.rate ? product.value.rate.toFixed(1) : "0.0";
})

// 库存检查计算属性
const productStock = computed(() => product.value.stock || 0)
const hasStock = computed(() => productStock.value > 0)
const hasHighStock = computed(() => productStock.value > 10)

// 切换图片
const changeImage = (index: number) => {
  currentImage.value = index
}

// 图片加载错误处理
const handleImageError = (event: Event) => {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = '/placeholder.jpg'
  }
}

// 加载商品详情
const loadProductDetail = async () => {
  loading.value = true
  
  try {
    const res = await getProductDetail(productId.value)
    if (res.data.code == 200 && res.data.data) {
      product.value = res.data.data
      
      // 设置默认库存和可用状态
      if (product.value.stock === undefined) {
        product.value.stock = 100
      }
      if (product.value.isAvailable === undefined) {
        product.value.isAvailable = true
      }
      
      // 重置数量选择器
      quantity.value = 1
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

// 页面加载时获取商品详情
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
          
          <div class="product-rating" v-if="product.rate || reviews.length > 0">
            <span class="rating-stars">
              <el-icon v-for="i in 5" :key="i" :class="i <= averageRatingValue ? 'star-filled' : 'star-empty'">
                <component :is="i <= averageRatingValue ? StarFilled : Star" />
              </el-icon>
            </span>
            <span class="rating-score">{{ averageRating }}</span>
            <span class="rating-count" v-if="reviews.length > 0">({{ reviews.length }}条评价)</span>
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
          </div>
        </div>
      </div>
      
      <!-- 商品详情 -->
      <div class="product-details">
        <h2 class="section-title">图书详情</h2>
        <div class="details-content">
          <p class="description">{{ product.description }}</p>
          <p class="detail" v-if="product.detail">{{ product.detail }}</p>
          
          <div class="spec-table" v-if="product.specifications && product.specifications.length > 0">
            <table>
              <tr v-for="spec in product.specifications" :key="spec.id">
                <th>{{ spec.item }}</th>
                <td>{{ spec.value }}</td>
              </tr>
            </table>
          </div>
        </div>
      </div>
      
      <!-- 评价区域 -->
      <div class="product-reviews" v-if="reviews.length > 0">
        <h2 class="section-title">用户评价</h2>
        <div class="review-summary">
          <div class="rating-big">
            <span class="rating-num">{{ averageRating }}</span>
            <div class="rating-stars-big">
              <el-icon v-for="i in 5" :key="i" :class="i <= averageRatingValue ? 'star-filled' : 'star-empty'">
                <component :is="i <= averageRatingValue ? StarFilled : Star" />
              </el-icon>
            </div>
            <div class="reviews-count">{{ reviews.length }}条评价</div>
          </div>
        </div>
        
        <div class="reviews-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <div class="review-user">
              <img :src="review.avatar" class="user-avatar" @error="handleImageError">
              <span class="username">{{ review.username }}</span>
            </div>
            <div class="review-content">
              <div class="review-rating">
                <span class="rating-stars">
                  <el-icon v-for="i in 5" :key="i" :class="i <= review.rating ? 'star-filled' : 'star-empty'">
                    <component :is="i <= review.rating ? StarFilled : Star" />
                  </el-icon>
                </span>
                <span class="review-date">{{ review.date }}</span>
              </div>
              <p class="review-text">{{ review.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>
<style scoped>
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

.section-title {
  font-size: 1.5rem;
  color: #303133;
  margin: 2rem 0 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #ebeef5;
}

.description {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.spec-table {
  margin-top: 1.5rem;
}

.spec-table table {
  width: 100%;
  border-collapse: collapse;
}

.spec-table th, .spec-table td {
  border: 1px solid #ebeef5;
  padding: 10px 15px;
  text-align: left;
}

.spec-table th {
  background: #f5f7fa;
  width: 100px;
  color: #606266;
  font-weight: normal;
}

.review-summary {
  display: flex;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f5f7fa;
  border-radius: 8px;
}

.rating-big {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rating-num {
  font-size: 3rem;
  font-weight: bold;
  color: #e74c3c;
}

.rating-stars-big {
  display: flex;
  margin: 0.5rem 0;
}

.reviews-count {
  color: #909399;
}

.reviews-list {
  margin-top: 1.5rem;
}

.review-item {
  display: flex;
  gap: 1rem;
  padding: 1.5rem 0;
  border-bottom: 1px solid #ebeef5;
}

.review-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 120px;
}

.user-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 8px;
}

.username {
  font-size: 0.9rem;
  color: #606266;
  text-align: center;
}

.review-content {
  flex: 1;
}

.review-rating {
  display: flex;
  margin-bottom: 0.5rem;
  align-items: center;
}

.review-date {
  margin-left: 10px;
  color: #909399;
  font-size: 0.8rem;
}

.review-text {
  color: #303133;
  line-height: 1.5;
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
  
  .review-item {
    flex-direction: column;
  }
  
  .review-user {
    flex-direction: row;
    justify-content: flex-start;
    width: 100%;
    margin-bottom: 1rem;
  }
  
  .user-avatar {
    margin-bottom: 0;
    margin-right: 10px;
  }
}
</style>