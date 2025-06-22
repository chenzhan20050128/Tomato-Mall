<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getProductList, searchProducts, Product } from '../../api/mall'
import { addToCart } from '../../api/cart'
import { navigateWithTransition } from '../../utils/transition'
import emitter from '../../utils/eventBus'
import { axios as request } from '../../utils/request';

// CLC分类映射
const clcMapping = {
  // 'A': '马克思主义、列宁主义、毛泽东思想、邓小平理论',
  'B': '哲学、宗教',
  'C': '社会科学总论',
  'D': '政治、法律',
  // 'E': '军事',
  'F': '经济',
  'G': '文化、科学、教育、体育',
  // 'H': '语言、文字',
  'I': '文学',
  // 'I1': '世界文学',
  // 'I2': '中国文学',
  'J': '艺术',
  'K': '历史、地理',
  'N': '自然科学总论',
  'O': '数理科学和化学',
  'P': '天文学、地球科学',
  'Q': '生物科学',
  // 'R': '医药、卫生',
  // 'S': '农业科学',
  'T': '工业技术',
  'TP': '计算机技术',
  // 'U': '交通运输',
  // 'V': '航空、航天',
  // 'X': '环境科学、安全科学',
  // 'Z': '综合性图书'
}

// 存储分类代码与名称的相互映射
const categoryCodeToName = ref<Record<string, string>>({})
const categoryNameToCode = ref<Record<string, string>>({})

// 修改广告为ref，不再使用常量
const advertisements = ref([
  {
    id: '1',
    title: '新书特惠',
    description: '精选新书5折起',
    image: 'https://picsum.photos/1200/300?random=1',
    backgroundColor: '#FCE4EC',
    textColor: '#880E4F',
    productId: '12345' // 示例商品ID
  },
  {
    id: '2',
    title: '编程书籍专区',
    description: '领略代码的艺术',
    image: 'https://picsum.photos/1200/300?random=2',
    backgroundColor: '#E3F2FD',
    textColor: '#0D47A1',
    productId: '67890' // 示例商品ID
  },
  {
    id: '3',
    title: '会员专享折扣',
    description: '注册即享会员价',
    image: 'https://picsum.photos/1200/300?random=3',
    backgroundColor: '#F1F8E9',
    textColor: '#33691E',
    productId: '11223' // 示例商品ID
  }
])

// 定义广告样式映射（为不同序号的广告提供不同颜色）
const adStyles = [
  { backgroundColor: '#FCE4EC', textColor: '#880E4F' },
  { backgroundColor: '#E3F2FD', textColor: '#0D47A1' },
  { backgroundColor: '#F1F8E9', textColor: '#33691E' }
]

// 新增: 从后端获取广告数据
const fetchAdvertisements = async () => {
  try {
    const response = await request.get('/api/advertisements')
    console.log('获取广告列表:', response.data)
    
    if (response.data.code === '200' || parseInt(response.data.code) === 200) {
      // 获取前三个广告
      const ads = response.data.data?.slice(0, 3) || []
      
      // 将后端广告数据转换为前端所需格式
      advertisements.value = ads.map((ad, index) => {
        return {
          id: ad.id,
          title: ad.title,
          description: ad.content,  // 将content映射为description
          image: ad.imgUrl,         // 将imgUrl映射为image
          backgroundColor: adStyles[index % adStyles.length].backgroundColor,
          textColor: adStyles[index % adStyles.length].textColor,
          productId: ad.productId   // 保留关联商品ID
        }
      })
    }
  } catch (error) {
    console.error('获取广告列表失败:', error)
  }
}

const router = useRouter()
const products = ref<Product[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('全部')
const sortOption = ref('default')

// 新增：分页相关状态
const currentPage = ref(1)
const pageSize = ref(12) // 每页显示12个商品
const totalProducts = ref(0)

// 新增：标签分页相关状态
const categoryPage = ref(1)
const categoryPageSize = ref(8) // 每页显示8个分类标签
const showAllCategories = ref(false)

const navigateToAdvertisedProduct = (productId: string | undefined, event: Event) => {
  event.preventDefault()
  event.stopPropagation()
  
  if (!productId) {
    ElMessage.info('此广告未关联商品')
    return
  }
  
  navigateWithTransition(router, `/product/${productId}`, 'product')
}

// 现有函数保持不变...
const navigateToProduct = (product: Product, event: Event) => {
  if ((event.target as HTMLElement).closest('.cart-btn')) {
    return
  }
  navigateWithTransition(router, `/product/${product.id}`, 'product')
}

const extractCategories = (products: Product[]): string[] => {
  const categories = new Set<string>(['全部'])
  categoryCodeToName.value = {}
  categoryNameToCode.value = {}

  products.forEach(product => {
    const categorySpec = product.specifications?.find(s => s.item === '分类')
    if (categorySpec) {
      const categoryCode = categorySpec.value
      const categoryName = clcMapping[categoryCode] || categoryCode

      categories.add(categoryName)
      categoryCodeToName.value[categoryCode] = categoryName
      categoryNameToCode.value[categoryName] = categoryCode
    }
  })

  return Array.from(categories)
}

const categories = ref<string[]>(['全部'])

const loadProducts = async () => {
  loading.value = true

  try {
    const res = await getProductList()
    if (res.data.code == 200) {
      products.value = res.data.data || []

      products.value.forEach(product => {
        if (product.stock === undefined) {
          product.stock = 100
        }
        if (product.isAvailable === undefined) {
          product.isAvailable = true
        }
      })

      categories.value = extractCategories(products.value)

      // 重置分页
      currentPage.value = 1
    } else {
      ElMessage.error('获取商品列表失败')
    }
  } catch (error) {
    console.error('获取商品列表出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 修改：添加分页逻辑的商品筛选
const filteredProducts = computed(() => {
  let result = [...products.value]

  // 分类筛选
  if (activeCategory.value !== '全部') {
    result = result.filter(product => {
      const categorySpec = product.specifications?.find(s => s.item === '分类')
      return categorySpec && categorySpec.value === categoryNameToCode.value[activeCategory.value]
    })
  }

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(product =>
      product.title.toLowerCase().includes(keyword) ||
      product.description.toLowerCase().includes(keyword) ||
      getAuthor(product).toLowerCase().includes(keyword)
    )
  }

  // 排序
  switch (sortOption.value) {
    case 'priceAsc':
      result.sort((a, b) => a.price - b.price)
      break
    case 'priceDesc':
      result.sort((a, b) => b.price - a.price)
      break
    case 'rateDesc':
      result.sort((a, b) => b.rate - a.rate)
      break
  }

  // 更新总数
  totalProducts.value = result.length

  return result
})

// 新增：当前页显示的商品
const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredProducts.value.slice(start, end)
})

// 新增：显示的分类标签（支持分页或展开全部）
const displayCategories = computed(() => {
  if (showAllCategories.value) {
    return categories.value
  }

  const start = (categoryPage.value - 1) * categoryPageSize.value
  const end = start + categoryPageSize.value
  return categories.value.slice(start, end)
})

// 新增：分类总页数
const totalCategoryPages = computed(() => {
  return Math.ceil(categories.value.length / categoryPageSize.value)
})

// 新增：商品总页数
const totalPages = computed(() => {
  return Math.ceil(totalProducts.value / pageSize.value)
})

// 修改：切换分类时重置页码
const changeCategory = (category: string) => {
  activeCategory.value = category
  currentPage.value = 1 // 重置到第一页
}

// 新增：分页处理函数
const handlePageChange = (page: number) => {
  currentPage.value = page
  // 滚动到商品列表顶部
  document.querySelector('.products-card')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  })
}

// 新增：分类标签分页处理
const handleCategoryPageChange = (direction: 'prev' | 'next') => {
  if (direction === 'prev' && categoryPage.value > 1) {
    categoryPage.value--
  } else if (direction === 'next' && categoryPage.value < totalCategoryPages.value) {
    categoryPage.value++
  }
}

// 新增：切换显示所有分类
const toggleShowAllCategories = () => {
  showAllCategories.value = !showAllCategories.value
  if (!showAllCategories.value) {
    categoryPage.value = 1
  }
}

// 修改：搜索时重置页码
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await loadProducts()
    return
  }

  currentPage.value = 1 // 重置页码

  loading.value = true
  try {
    const res = await searchProducts(searchKeyword.value)
    if (res.data.code == 200) {
      products.value = res.data.data || []

      products.value.forEach(product => {
        if (product.stock === undefined) {
          product.stock = 100
        }
        if (product.isAvailable === undefined) {
          product.isAvailable = true
        }
      })
    } else {
      ElMessage.error('搜索失败')
    }
  } catch (error) {
    console.error('搜索出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 其他现有函数保持不变...
const handleAddToCart = async (product: Product, event: Event) => {
  event.stopPropagation()

  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const res = await addToCart(product.id, 1)
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

const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

const handleImageError = (event: Event) => {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = '/placeholder.jpg'
  }
}

const getAuthor = (product: Product): string => {
  const authorSpec = product.specifications?.find(spec => spec.item === '作者')
  return authorSpec ? authorSpec.value : '未知作者'
}

onMounted(() => {
  loadProducts()
  fetchAdvertisements()
})
</script>

<template>
  <div class="mall-container">
    <!-- 顶部区域 -->
    <div class="hero-section">
      <h1 class="hero-title">番茄读书商城</h1>
      <p class="hero-subtitle">发现您的下一本心爱之书</p>

      <!-- 搜索区域 -->
      <div class="search-container">
        <el-input v-model="searchKeyword" placeholder="搜索书名、作者或描述..." class="search-input" @keyup.enter="handleSearch">
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 广告轮播 -->
    <div class="carousel-container">
      <el-carousel :interval="5000" type="card" height="300px">
        <el-carousel-item v-for="item in advertisements" :key="item.id">
          <div class="carousel-content" :style="{ backgroundColor: item.backgroundColor }">
            <div class="carousel-text" :style="{ color: item.textColor }">
              <h2>{{ item.title }}</h2>
              <p>{{ item.description }}</p>
              <el-button 
                type="primary" 
                size="small" 
                class="carousel-btn"
                :disabled="!item.productId"
                @click="(e) => navigateToAdvertisedProduct(item.productId, e)"
              >
                了解更多
                <el-icon class="el-icon--right" v-if="item.productId">
                  <ArrowRight />
                </el-icon>
              </el-button>
            </div>
            <div class="carousel-image">
              <img :src="item.image" :alt="item.title"
                @error="(e) => (e.target as HTMLImageElement).style.display = 'none'" />
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <main class="content-section">
      <!-- 分类导航 -->
      <el-card class="category-card">
        <div class="categories-header">
          <h3>商品分类</h3>
          <div class="category-controls">
            <!-- 分类分页控制 -->
            <div v-if="!showAllCategories && totalCategoryPages > 1" class="category-pagination">
              <el-button size="small" :disabled="categoryPage === 1" @click="handleCategoryPageChange('prev')">
                <el-icon>
                  <ArrowLeft />
                </el-icon>
              </el-button>
              <span class="page-info">{{ categoryPage }} / {{ totalCategoryPages }}</span>
              <el-button size="small" :disabled="categoryPage === totalCategoryPages"
                @click="handleCategoryPageChange('next')">
                <el-icon>
                  <ArrowRight />
                </el-icon>
              </el-button>
            </div>

            <!-- 展开/收起按钮 -->
            <el-button size="small" type="text" @click="toggleShowAllCategories"
              v-if="categories.length > categoryPageSize">
              {{ showAllCategories ? '收起' : `展开全部(${categories.length})` }}
            </el-button>
          </div>
        </div>

        <div class="categories-wrapper">
          <div v-for="category in displayCategories" :key="category"
            :class="['category-item', { active: activeCategory === category }]" @click="changeCategory(category)">
            {{ category }}
          </div>
        </div>

        <!-- 排序选项 -->
        <div class="sort-wrapper">
          <el-select v-model="sortOption" class="sort-select" size="default">
            <el-option label="默认排序" value="default" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
            <el-option label="评分优先" value="rateDesc" />
          </el-select>
        </div>
      </el-card>

      <!-- 商品列表 -->
      <el-card class="products-card" v-loading="loading" element-loading-text="正在加载图书数据...">
        <!-- 商品数量统计 -->
        <div class="products-header">
          <span class="products-count">
            共找到 {{ totalProducts }} 本图书
            <span v-if="activeCategory !== '全部'" class="filter-info">
              (分类: {{ activeCategory }})
            </span>
            <span v-if="searchKeyword" class="filter-info">
              (搜索: "{{ searchKeyword }}")
            </span>
          </span>
        </div>

        <div v-if="paginatedProducts.length === 0 && !loading" class="empty-container">
          <el-empty description="没有找到符合条件的图书" />
        </div>

        <div v-else class="products-grid">
          <div v-for="product in paginatedProducts" :key="product.id" class="product-card"
            @click="(e) => navigateToProduct(product, e)">
            <!-- 商品可用状态标签 -->
            <div v-if="!product.isAvailable" class="product-status-tag unavailable">
              暂无库存
            </div>

            <div class="product-image">
              <img :src="product.cover" :alt="product.title" @error="handleImageError" referrerpolicy="no-referrer"
                rossorigin="anonymous" :style="{ viewTransitionName: `product-image-${product.id}` }">
              <div class="product-overlay" v-if="product.isAvailable">
                <el-button type="primary" size="small" @click="(e) => handleAddToCart(product, e)" class="cart-btn">
                  <el-icon>
                    <ShoppingCart />
                  </el-icon> 加入购物车
                </el-button>
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-title">{{ product.title }}</h3>
              <p class="product-author">{{ getAuthor(product) }}</p>
              <p class="product-description">{{ product.description }}</p>

              <div class="product-footer">
                <span class="product-price">{{ formatPrice(product.price) }}</span>
                <div class="product-rating">
                  <span class="rating-value">{{ product.rate.toFixed(1) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页组件 -->
        <div v-if="totalPages > 1" class="pagination-container">
          <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="totalProducts"
            :page-sizes="[8, 12, 16, 24]" layout="total, sizes, prev, pager, next, jumper"
            @current-change="handlePageChange" @size-change="(size) => { pageSize = size; currentPage = 1; }"
            background />
        </div>
      </el-card>
    </main>

    <footer class="mall-footer">
      <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
    </footer>
  </div>
</template>

<style scoped>
/* 现有样式保持不变... */
.mall-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
}

.hero-section {
  text-align: center;
  padding: 4rem 2rem 2rem;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
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

/* 新增：分类导航增强样式 */
.category-card {
  margin-bottom: 2rem;
  border-radius: 8px;
}

.categories-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.categories-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.category-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-info {
  font-size: 14px;
  color: #606266;
  min-width: 40px;
  text-align: center;
}

.categories-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 15px;
  min-height: 40px;
  /* 防止布局跳动 */
}

.category-item {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #f5f7fa;
  border: 1px solid #e0e0e0;
}

.category-item:hover {
  background-color: #f0f0f0;
}

.category-item.active {
  background-color: #e74c3c;
  color: white;
  border-color: #e74c3c;
}

.sort-wrapper {
  display: flex;
  justify-content: flex-end;
}

/* 新增：商品列表增强样式 */
.products-card {
  margin-bottom: 2rem;
  min-height: 400px;
}

.products-header {
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.products-count {
  font-size: 14px;
  color: #606266;
}

.filter-info {
  color: #e74c3c;
  font-weight: 500;
}

.empty-container {
  padding: 80px 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  min-height: 300px;
  /* 确保分页时布局稳定 */
}

/* 新增：分页样式 */
.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}

.pagination-container :deep(.el-pagination) {
  gap: 8px;
}

.pagination-container :deep(.el-pagination .btn-prev),
.pagination-container :deep(.el-pagination .btn-next) {
  background-color: #f5f7fa;
}

.pagination-container :deep(.el-pagination .el-pager li.is-active) {
  background-color: #e74c3c;
  border-color: #e74c3c;
}

/* 其他现有样式保持不变... */
.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.product-image {
  height: 220px;
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

.product-card:hover .product-image img {
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

.product-card:hover .product-overlay {
  transform: translateY(0);
}

.cart-btn {
  background-color: #e74c3c;
  border-color: #e74c3c;
}

.cart-btn:hover,
.cart-btn:focus {
  background-color: #c0392b;
  border-color: #c0392b;
}

.product-info {
  padding: 15px;
  display: flex;
  flex-direction: column;
  flex: 1;
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
  height: 44px;
}

.product-author {
  font-size: 14px;
  color: #909399;
  margin: 0 0 8px;
}

.product-description {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #e74c3c;
}

.product-rating {
  background: #f0f7ff;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.product-status-tag {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  text-align: center;
  padding: 4px;
  font-size: 13px;
  z-index: 5;
}

.product-status-tag.unavailable {
  background-color: rgba(231, 76, 60, 0.8);
}

.mall-footer {
  text-align: center;
  padding: 1.5rem;
  background-color: #2c3e50;
  color: white;
}

/* 轮播广告样式保持不变... */
.carousel-container {
  max-width: 1200px;
  margin: -20px auto 30px;
  padding: 0 2rem;
  position: relative;
  z-index: 10;
}

.carousel-content {
  height: 100%;
  border-radius: 10px;
  display: flex;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.carousel-text {
  flex: 1;
  padding: 30px 35px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.carousel-text h2 {
  font-size: 28px;
  margin: 0 0 15px;
  font-weight: 600;
}

.carousel-text p {
  font-size: 18px;
  margin: 0 0 20px;
}

.carousel-btn {
  align-self: flex-start;
  font-size: 16px;
  padding: 8px 20px;
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

  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 15px;
  }

  .product-image {
    height: 180px;
  }

  .carousel-container {
    padding: 0 1rem;
    margin-top: -30px;
  }

  .carousel-text h2 {
    font-size: 22px;
  }

  .carousel-text p {
    font-size: 16px;
  }

  .carousel-image {
    display: none;
  }

  /* 移动端分类和分页调整 */
  .categories-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .category-controls {
    align-self: stretch;
    justify-content: space-between;
  }

  .pagination-container :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>