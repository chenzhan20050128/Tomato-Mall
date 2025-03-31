<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ShoppingCart, Star, StarFilled, Picture } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const productId = computed(() => Number(route.params.id))

// 商品详情
const product = ref({
  id: 1,
  name: '活着',
  author: '余华',
  description: '《活着》是中国作家余华的代表作之一，讲述了一个人历经各种苦难的一生，揭示了生命的价值和意义。主人公福贵出身于地主家庭，因赌博而家破人亡，之后经历了战争、饥荒、文化大革命等一系列历史变故，最终只剩他一人苟活于世。小说以极其朴素的笔调讲述了在苦难中求生的故事，展现了生命的坚韧与尊严。',
  price: 39.50,
  originalPrice: 45.00,
  stock: 150,
  image: '/images/books/huozhe.jpg',
  isAvailable: true,
  category: '文学',
  sales: 210,
  publisher: '作家出版社',
  publishDate: '2012-8-1',
  pages: 226,
  isbn: '9787506365437',
  language: '简体中文',
  format: '平装',
  images: [
    '/images/books/huozhe.jpg',
    '/images/books/huozhe-detail1.jpg',
    '/images/books/huozhe-detail2.jpg',
  ]
})

// 数量选择
const quantity = ref(1)

// 增加数量
const increaseQuantity = () => {
  if (quantity.value < product.value.stock) {
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
  if (isNaN(num) || num < 1) {
    num = 1
  } else if (num > product.value.stock) {
    num = product.value.stock
    ElMessage.warning('已达到库存上限')
  }
  quantity.value = num
}

// 添加到购物车
const addToCart = () => {
  ElMessage({
    message: `已将《${product.value.name}》×${quantity.value}添加到购物车`,
    type: 'success'
  })
}

// 立即购买
const buyNow = () => {
  ElMessage({
    message: '购买功能暂未实现',
    type: 'info'
  })
}

// 返回商城
const goBack = () => {
  router.push('/mall')
}

// 格式化价格
const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

// 计算折扣
const discount = computed(() => {
  if (!product.value.originalPrice) return null
  return Math.round((1 - product.value.price / product.value.originalPrice) * 100)
})

// 模拟评论数据
const reviews = ref([
  {
    id: 1,
    username: '文学爱好者',
    avatar: '/images/avatars/avatar1.jpg',
    rating: 5,
    date: '2023-10-15',
    content: '这是一本让人深思的好书，余华的文笔太有震撼力了，真实地描绘了那个年代普通人的生活。'
  },
  {
    id: 2,
    username: '安静的读者',
    avatar: '/images/avatars/avatar2.jpg',
    rating: 4,
    date: '2023-09-22',
    content: '很喜欢这本书，虽然情节有些悲伤，但它展示了生命的顽强。装帧很好，纸质也不错。'
  },
  {
    id: 3,
    username: '书中自有黄金屋',
    avatar: '/images/avatars/avatar3.jpg',
    rating: 5,
    date: '2023-08-18',
    content: '这是我第三次读《活着》，每次都有不同的感受，强烈推荐！'
  }
])

// 总评分（数字类型，用于计算）
const averageRatingValue = computed(() => {
  if (reviews.value.length === 0) return 0;
  const total = reviews.value.reduce((sum, review) => sum + review.rating, 0)
  return total / reviews.value.length
})

// 总评分
const averageRating = computed(() => {
  const total = reviews.value.reduce((sum, review) => sum + review.rating, 0)
  return (total / reviews.value.length).toFixed(1)
})

// 当前展示图片
const currentImage = ref(0)

// 切换图片
const changeImage = (index: number) => {
  currentImage.value = index
}

// 图片处理函数，处理加载错误
const handleImageError = (event) => {
  event.target.src = '/images/book-placeholder.jpg'
}

// 页面加载
onMounted(() => {
  // 模拟API加载
  setTimeout(() => {
    loading.value = false
  }, 500)
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
        <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-card v-loading="loading" element-loading-text="加载商品详情...">
      <div class="product-main">
        <!-- 左侧图片区域 -->
        <div class="product-gallery">
          <div class="main-image">
            <img 
              :src="product.images?.[currentImage] || product.image" 
              :alt="product.name"
              @error="handleImageError"
            >
          </div>
          <div class="thumbnail-list" v-if="product.images && product.images.length > 1">
            <div 
              v-for="(img, index) in product.images" 
              :key="index"
              :class="['thumbnail', { active: currentImage === index }]"
              @click="changeImage(index)"
            >
              <img :src="img" :alt="`${product.name} - 图片 ${index + 1}`" @error="handleImageError">
            </div>
          </div>
        </div>

        <!-- 右侧信息区域 -->
        <div class="product-info">
          <h1 class="product-title">{{ product.name }}</h1>
          <div class="product-author">作者: {{ product.author }}</div>
          
          <div class="product-rating">
            <span class="rating-stars">
              <el-icon v-for="i in 5" :key="i">
                <StarFilled v-if="i <= Math.floor(averageRatingValue)" class="star-filled" />
                <Star v-else class="star-empty" />
              </el-icon>
            </span>
            <span class="rating-score">{{ averageRating }}</span>
            <span class="rating-count">({{ reviews.length }}条评价)</span>
          </div>
          
          <div class="product-price-area">
            <div class="price-wrapper">
              <span class="current-price">{{ formatPrice(product.price) }}</span>
              <span class="original-price" v-if="product.originalPrice">
                {{ formatPrice(product.originalPrice) }}
              </span>
              <span class="discount-tag" v-if="discount">{{ discount }}% 优惠</span>
            </div>
          </div>
          
          <div class="product-meta">
            <div class="meta-item">
              <span class="meta-label">出版社:</span>
              <span class="meta-value">{{ product.publisher }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">出版日期:</span>
              <span class="meta-value">{{ product.publishDate }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">页数:</span>
              <span class="meta-value">{{ product.pages }}页</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">ISBN:</span>
              <span class="meta-value">{{ product.isbn }}</span>
            </div>
          </div>
          
          <div class="product-stock">
            <span :class="['stock-status', product.stock > 10 ? 'in-stock' : 'low-stock']">
              {{ product.stock > 0 ? '有货' : '缺货' }}
            </span>
            <span class="stock-count" v-if="product.stock > 0">
              {{ product.stock > 10 ? '库存充足' : `仅剩${product.stock}件` }}
            </span>
          </div>
          
          <div class="product-actions">
            <div class="quantity-control">
              <span class="quantity-label">数量:</span>
              <el-input-number 
                v-model="quantity" 
                :min="1" 
                :max="product.stock" 
                :disabled="product.stock <= 0"
                @change="setQuantity"
              />
            </div>
            
            <div class="action-buttons">
              <el-button 
                type="primary" 
                :icon="ShoppingCart" 
                :disabled="product.stock <= 0"
                @click="addToCart"
                class="cart-button"
              >
                加入购物车
              </el-button>
              
              <el-button 
                type="danger" 
                :disabled="product.stock <= 0"
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
          
          <div class="spec-table">
            <table>
              <tbody>
                <tr>
                  <th>书名</th>
                  <td>{{ product.name }}</td>
                  <th>作者</th>
                  <td>{{ product.author }}</td>
                </tr>
                <tr>
                  <th>出版社</th>
                  <td>{{ product.publisher }}</td>
                  <th>出版日期</th>
                  <td>{{ product.publishDate }}</td>
                </tr>
                <tr>
                  <th>ISBN</th>
                  <td>{{ product.isbn }}</td>
                  <th>版本</th>
                  <td>{{ product.format }}</td>
                </tr>
                <tr>
                  <th>页数</th>
                  <td>{{ product.pages }}页</td>
                  <th>语言</th>
                  <td>{{ product.language }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      
      <!-- 评价区域 -->
      <div class="product-reviews">
        <h2 class="section-title">用户评价</h2>
        <div class="review-summary">
          <div class="rating-big">
            <span class="rating-num">{{ averageRating }}</span>
            <div class="rating-stars-big">
              <el-icon v-for="i in 5" :key="i" :size="24">
                <StarFilled v-if="i <= Math.floor(averageRatingValue)" class="star-filled" />
                <Star v-else class="star-empty" />
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
                <el-icon v-for="i in 5" :key="i">
                  <StarFilled v-if="i <= review.rating" class="star-filled" />
                  <Star v-else class="star-empty" />
                </el-icon>
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