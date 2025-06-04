<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Star, Refresh, Mic } from '@element-plus/icons-vue'
import { addToCart } from '../../api/cart'
import { navigateWithTransition } from '../../utils/transition'
import emitter from '../../utils/eventBus'
import { getRandomRecommendations } from '../../api/recommend'

// 定义随机推荐商品类型，匹配后端 ProductRankingDTO
interface ProductRanking {
  productId: number
  title: string
  cover: string
  price: number
  sales: number
  stock: number
  description: string
  category: string
  author: string
  averageScore: number
  ratingCount: number
  isAvailable?: boolean  // 前端计算的可用性
  recommendReason?: string // 推荐理由
}

const router = useRouter()
const loading = ref(false)
const products = ref<ProductRanking[]>([])
const searchKeyword = ref('')

// 当前登录用户ID，用于个性化推荐
const userId = ref<number | null>(null)

// 推荐数量
const recommendCount = ref(6)

// 分页相关状态
const currentPage = ref(1)
const pageSize = ref(6)

// 推荐理由列表（用于随机分配）
const recommendReasons = [
  '根据您的阅读历史推荐',
  '热门新书',
  '与您喜欢的类似',
  '其他用户也在看',
  '畅销新品',
  '编辑推荐'
]

// 加载随机推荐数据
const loadRandomRecommendations = async () => {
  loading.value = true
  
  try {
    // 构建请求参数对象
    const params: { userId?: number; count: number } = {
      count: recommendCount.value
    }
    if (userId.value !== null) {
      params.userId = userId.value
    }
    
    // 使用API函数发送请求
    const response = await getRandomRecommendations(params)
    
    // 调试输出
    console.log('API Response:', response)
    
    // 处理返回数据
    if (response.data.code === '200') {
      // 产品数据在response.data.data而不是response.data.result
      const productsData = response.data.data || []
      
      // 添加可用性信息和随机推荐理由
      products.value = productsData.map((item) => ({
        ...item,
        isAvailable: item.stock > 0,
        recommendReason: recommendReasons[Math.floor(Math.random() * recommendReasons.length)]
      }))
      
      // 重置页码
      currentPage.value = 1
    } else {
      ElMessage.error(`获取推荐图书失败: ${response.data.msg || '未知错误'}`)
    }
  } catch (error) {
    console.error('获取推荐图书出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const refreshRecommendations = () => {
  loadRandomRecommendations()
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
}

// 过滤后的商品列表（本地搜索）
const filteredProducts = computed(() => {
  if (!searchKeyword.value) return products.value
  
  const keyword = searchKeyword.value.toLowerCase()
  return products.value.filter(product => 
    product.title.toLowerCase().includes(keyword) || 
    product.description.toLowerCase().includes(keyword) ||
    product.author.toLowerCase().includes(keyword)
  )
})

// 分页后的商品列表
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredProducts.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredProducts.value.length / pageSize.value)
})

// 页码变更
const handlePageChange = (page: number) => {
  currentPage.value = page
  // 滚动到商品列表顶部
  document.querySelector('.recommend-products')?.scrollIntoView({ 
    behavior: 'smooth', 
    block: 'start' 
  })
}

// 添加到购物车
const handleAddToCart = async (product: ProductRanking, event: Event) => {
  event.stopPropagation()
  
  if (!localStorage.getItem('token') && !sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    const res = await addToCart(product.productId, 1)
    if (res.data.code == 200) {
      ElMessage.success(`已将《${product.title}》加入购物车`)
      emitter.emit('updateCartCount')
    } else {
      ElMessage.error('添加购物车失败')
    }
  } catch (error) {
    console.error('添加购物车出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 跳转到商品详情
const navigateToProduct = (product: ProductRanking, event: Event) => {
  if ((event.target as HTMLElement).closest('.cart-btn')) {
    return
  }
  navigateWithTransition(router, `/product/${product.productId}`, 'product')
}

// 格式化价格
const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

// 处理图片加载错误
const handleImageError = (event: Event) => {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = '/placeholder.jpg'
  }
}

// 获取评分星级展示
const getRatingStars = (score: number): string => {
  if (!score) return '☆☆☆☆☆'
  const fullStars = Math.floor(score)
  const halfStar = score - fullStars >= 0.5 ? 1 : 0
  const emptyStars = 5 - fullStars - halfStar
  
  return '★'.repeat(fullStars) + (halfStar ? '½' : '') + '☆'.repeat(emptyStars)
}

// 检查用户登录状态
const checkUserLogin = () => {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const userIdStr = localStorage.getItem('userId') || sessionStorage.getItem('userId')
  
  if (token && userIdStr) {
    userId.value = parseInt(userIdStr)
  } else {
    userId.value = null
  }
}

onMounted(() => {
  checkUserLogin()
  loadRandomRecommendations()
})
</script>

<template>
  <div class="random-recommend-container">
    <!-- 顶部区域 -->
    <div class="hero-section">
      <h1 class="hero-title">猜你喜欢</h1>
      <p class="hero-subtitle">为您精选的个性化图书推荐</p>
      
      <!-- 搜索区域 -->
      <div class="search-container">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书名、作者或描述..."
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <main class="content-section">
      <!-- 刷新与控制 -->
      <div class="refresh-section">
        <div class="refresh-info">
          <el-icon><Mic /></el-icon>
          <span class="refresh-text">
            {{ userId ? '根据您的阅读偏好生成个性化推荐' : '精选推荐图书' }}
          </span>
        </div>
        <el-button 
          type="primary" 
          :icon="Refresh" 
          @click="refreshRecommendations" 
          :loading="loading"
          class="refresh-button"
        >
          换一批
        </el-button>
      </div>
      
      <!-- 推荐图书列表 -->
      <el-card class="recommend-products" v-loading="loading" element-loading-text="正在为您精选图书...">
        <div v-if="paginatedProducts.length === 0 && !loading" class="empty-container">
          <el-empty description="暂无推荐图书">
            <template #image>
              <el-icon :size="60" class="empty-icon"><Star /></el-icon>
            </template>
            <el-button type="primary" @click="refreshRecommendations">
              刷新推荐
            </el-button>
          </el-empty>
        </div>
        
        <div v-else class="recommend-grid">
          <div 
            v-for="product in paginatedProducts" 
            :key="product.productId" 
            class="recommend-item" 
            @click="(e) => navigateToProduct(product, e)"
          >
            <!-- 推荐理由标签 -->
            <div class="recommend-reason" v-if="product.recommendReason">
              {{ product.recommendReason }}
            </div>
            
            <!-- 商品可用状态标签 -->
            <div v-if="!product.isAvailable" class="product-status-tag unavailable">
              暂无库存
            </div>
            
            <div class="product-image">
              <img 
                :src="product.cover" 
                :alt="product.title"
                @error="handleImageError"
                referrerpolicy="no-referrer"
                rossorigin="anonymous"
                :style="{ viewTransitionName: `product-image-${product.productId}` }"
              >
              <div class="product-overlay" v-if="product.isAvailable">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="(e) => handleAddToCart(product, e)" 
                  class="cart-btn"
                >
                  <el-icon><ShoppingCart /></el-icon> 加入购物车
                </el-button>
              </div>
            </div>
            
            <div class="product-info">
              <h3 class="product-title">{{ product.title }}</h3>
              <p class="product-author">{{ product.author }}</p>
              
              <div class="product-footer">
                <span class="product-price">{{ formatPrice(product.price) }}</span>
                <div class="product-rating">
                  <div class="rating-stars">{{ getRatingStars(product.averageScore) }}</div>
                  <span class="rating-value">{{ product.averageScore?.toFixed(1) || '暂无' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分页组件 -->
        <div v-if="totalPages > 1" class="pagination-container">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="filteredProducts.length"
            layout="prev, pager, next"
            @current-change="handlePageChange"
            background
          />
        </div>
      </el-card>
      
      <!-- 用户提示 -->
      <div class="user-tips" v-if="!userId">
        <el-alert
          title="登录后获取更精准的个性化推荐"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            基于您的阅读历史和兴趣偏好，为您推荐更符合口味的图书
            <el-button type="primary" size="small" @click="router.push('/login')" class="login-btn">
              立即登录
            </el-button>
          </template>
        </el-alert>
      </div>
    </main>
    
    <footer class="recommend-footer">
      <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
    </footer>
  </div>
</template>

<style scoped>
.random-recommend-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
}

.hero-section {
  text-align: center;
  padding: 4rem 2rem 2rem;
  background: linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%);
  color: white;
}

.hero-title {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.hero-subtitle {
  font-size: 1.2rem;
  font-weight: 300;
  margin-bottom: 1.5rem;
}

.search-container {
  max-width: 600px;
  margin: -1rem auto 0;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.content-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* 刷新区域样式 */
.refresh-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  background-color: white;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.refresh-info {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #606266;
}

.refresh-info .el-icon {
  color: #9b59b6;
  font-size: 20px;
}

.refresh-text {
  font-size: 16px;
}

.refresh-button {
  background-color: #9b59b6;
  border-color: #9b59b6;
}

.refresh-button:hover, .refresh-button:focus {
  background-color: #8e44ad;
  border-color: #8e44ad;
}

/* 推荐图书列表样式 */
.recommend-products {
  margin-bottom: 2rem;
  min-height: 400px;
}

.empty-container {
  padding: 80px 0;
}

.empty-icon {
  color: #dcdfe6;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
}

.recommend-item {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  height: 420px;
  cursor: pointer;
}

.recommend-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.recommend-reason {
  position: absolute;
  top: 15px;
  left: 15px;
  background-color: rgba(155, 89, 182, 0.9);
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.product-image {
  height: 250px;
  overflow: hidden;
  position: relative;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
  transition: transform 0.5s;
}

.recommend-item:hover .product-image img {
  transform: scale(1.05);
}

.product-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  padding: 10px;
  transform: translateY(100%);
  transition: transform 0.3s;
}

.recommend-item:hover .product-overlay {
  transform: translateY(0);
}

.cart-btn {
  background-color: #9b59b6;
  border-color: #9b59b6;
}

.cart-btn:hover, .cart-btn:focus {
  background-color: #8e44ad;
  border-color: #8e44ad;
}

.product-info {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.product-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  color: #303133;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 40px;
}

.product-author {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px;
}

.product-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #9b59b6;
}

.product-rating {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.rating-stars {
  color: #f39c12;
  font-size: 12px;
  letter-spacing: -1px;
}

.rating-value {
  font-size: 12px;
  color: #f39c12;
  font-weight: 600;
}

.product-status-tag {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  text-align: center;
  padding: 4px 10px;
  font-size: 13px;
  z-index: 5;
}

.product-status-tag.unavailable {
  background-color: rgba(231, 76, 60, 0.8);
}

/* 用户提示样式 */
.user-tips {
  margin-top: 2rem;
}

.user-tips :deep(.el-alert) {
  display: flex;
  align-items: center;
}

.login-btn {
  margin-left: 1rem;
}

/* 分页样式 */
.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.pagination-container :deep(.el-pagination .el-pager li.is-active) {
  background-color: #9b59b6;
  border-color: #9b59b6;
}

.recommend-footer {
  text-align: center;
  padding: 1.5rem;
  background-color: #2c3e50;
  color: white;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .content-section {
    padding: 1rem;
  }
  
  .refresh-section {
    flex-direction: column;
    gap: 15px;
    padding: 15px;
  }
  
  .refresh-button {
    width: 100%;
  }
  
  .recommend-grid {
    grid-template-columns: 1fr;
  }
  
  .recommend-item {
    height: auto;
  }
  
  .product-image {
    height: 200px;
  }
}
</style>