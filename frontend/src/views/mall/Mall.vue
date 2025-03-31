<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Star, Filter } from '@element-plus/icons-vue'
import { ro } from 'element-plus/es/locale/index.mjs'
// 注释掉后端API调用，使用前端模拟数据
// import { getProductList, Product } from '../../api/mall'

// 定义产品数据类型
interface Product {
  id: number
  name: string
  description: string
  price: number
  stock: number
  image: string
  isAvailable: boolean
  category: string
  sales?: number
}

// 商品列表数据
const products = ref<Product[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('全部')
const sortOption = ref('default')

// 假设的产品分类
const categories = ref([
  '全部', '文学', '历史', '科学', '经济', '哲学', '艺术'
])

// 加载商品列表 - 使用前端模拟数据
const loadProducts = () => {
  loading.value = true
  
  // 模拟延迟加载
  setTimeout(() => {
    // 前端模拟数据 - 20本书
    products.value = [
      {
        id: 1,
        name: '活着',
        description: '《活着》是中国作家余华的代表作之一，讲述了一个人历经各种苦难的一生，揭示了生命的价值和意义。',
        price: 39.50,
        stock: 150,
        image: 'https://img3.doubanio.com/view/subject/s/public/s29053580.jpg',
        isAvailable: true,
        category: '文学',
        sales: 210
      },
      {
        id: 2,
        name: '百年孤独',
        description: '《百年孤独》是魔幻现实主义文学的代表作，讲述了布恩迪亚家族七代人的传奇故事。',
        price: 59.80,
        stock: 85,
        image: 'https://img2.doubanio.com/view/subject/s/public/s27237850.jpg',
        isAvailable: true,
        category: '文学',
        sales: 185
      },
      {
        id: 3,
        name: '平凡的世界',
        description: '《平凡的世界》是中国作家路遥的代表作，描述了中国普通民众在时代大背景下的人生命运。',
        price: 128.00,
        stock: 65,
        image: 'https://img1.doubanio.com/view/subject/s/public/s29966355.jpg',
        isAvailable: true,
        category: '文学',
        sales: 156
      },
      {
        id: 4,
        name: '三体',
        description: '《三体》是中国科幻文学的里程碑之作，描绘了人类文明与三体文明的接触与冲突。',
        price: 59.00,
        stock: 120,
        image: 'https://img9.doubanio.com/view/subject/s/public/s29634439.jpg',
        isAvailable: true,
        category: '科学',
        sales: 298
      },
      {
        id: 5,
        name: '围城',
        description: '《围城》被誉为中国现代文学的经典，以幽默的笔触描写了知识分子的婚姻与生活。',
        price: 32.00,
        stock: 77,
        image: 'https://img2.doubanio.com/view/subject/s/public/s1070222.jpg',
        isAvailable: true,
        category: '文学',
        sales: 88
      },
      {
        id: 6,
        name: '人类简史',
        description: '《人类简史》是一部对人类历史的宏大叙事，探讨了人类如何从史前时代发展到今天。',
        price: 68.00,
        stock: 95,
        image: 'https://img3.doubanio.com/view/subject/s/public/s27814883.jpg',
        isAvailable: true,
        category: '历史',
        sales: 176
      },
      {
        id: 7,
        name: '解忧杂货店',
        description: '《解忧杂货店》是一部温暖人心的小说，讲述了一家可以解答人们心灵困惑的杂货店的故事。',
        price: 39.50,
        stock: 160,
        image: 'https://img1.doubanio.com/view/subject/s/public/s27264181.jpg',
        isAvailable: true,
        category: '文学',
        sales: 253
      },
      {
        id: 8,
        name: '小王子',
        description: '《小王子》是一部充满诗意的童话故事，蕴含着深刻的人生哲理。',
        price: 29.80,
        stock: 210,
        image: 'https://img2.doubanio.com/view/subject/s/public/s29052169.jpg',
        isAvailable: true,
        category: '文学',
        sales: 320
      },
      {
        id: 9,
        name: '国富论',
        description: '《国富论》是经济学的经典著作，探讨了国家财富的本质和增长的原因。',
        price: 88.00,
        stock: 45,
        image: 'https://img9.doubanio.com/view/subject/s/public/s2154432.jpg',
        isAvailable: true,
        category: '经济',
        sales: 67
      },
      {
        id: 10,
        name: '西方哲学史',
        description: '《西方哲学史》是罗素的代表作之一，详细介绍了西方哲学的发展历程。',
        price: 78.00,
        stock: 60,
        image: 'https://img1.doubanio.com/view/subject/s/public/s1789059.jpg',
        isAvailable: true,
        category: '哲学',
        sales: 43
      },
      {
        id: 11,
        name: '艺术的故事',
        description: '《艺术的故事》是一部艺术史经典著作，以通俗易懂的方式讲述了艺术的发展历程。',
        price: 128.00,
        stock: 35,
        image: 'https://img2.doubanio.com/view/subject/s/public/s3219163.jpg',
        isAvailable: true,
        category: '艺术',
        sales: 56
      },
      {
        id: 12,
        name: '时间简史',
        description: '《时间简史》是霍金的科普经典，向普通读者介绍了宇宙学的复杂概念。',
        price: 45.00,
        stock: 85,
        image: 'https://img3.doubanio.com/view/subject/s/public/s1914436.jpg',
        isAvailable: true,
        category: '科学',
        sales: 132
      },
      {
        id: 13,
        name: '红楼梦',
        description: '《红楼梦》是中国古典文学巅峰之作，以贾宝玉和林黛玉的爱情悲剧为主线，描绘了封建社会的全景。',
        price: 59.90,
        stock: 110,
        image: 'https://img1.doubanio.com/view/subject/s/public/s1070959.jpg',
        isAvailable: true,
        category: '文学',
        sales: 178
      },
      {
        id: 14,
        name: '失控',
        description: '《失控》探讨了科技与文明的关系，被誉为"互联网的圣经"。',
        price: 88.00,
        stock: 48,
        image: 'https://img9.doubanio.com/view/subject/s/public/s4554820.jpg',
        isAvailable: true,
        category: '科学',
        sales: 95
      },
      {
        id: 15,
        name: '经济学原理',
        description: '《经济学原理》是曼昆的经济学教科书，深入浅出地阐释了经济学的核心概念和原理。',
        price: 108.00,
        stock: 55,
        image: 'https://img1.doubanio.com/view/subject/s/public/s3785854.jpg',
        isAvailable: true,
        category: '经济',
        sales: 76
      },
      {
        id: 16,
        name: '苏菲的世界',
        description: '《苏菲的世界》是一部哲学启蒙书，以小说的形式讲述了西方哲学的发展史。',
        price: 38.00,
        stock: 95,
        image: 'https://img3.doubanio.com/view/subject/s/public/s2153661.jpg',
        isAvailable: true,
        category: '哲学',
        sales: 124
      },
      {
        id: 17,
        name: '梵高传',
        description: '《梵高传》是一部纪实性传记，详细记录了梵高短暂而充满激情的艺术生涯。',
        price: 68.00,
        stock: 28,
        image: 'https://img1.doubanio.com/view/subject/s/public/s1167084.jpg',
        isAvailable: true,
        category: '艺术',
        sales: 47
      },
      {
        id: 18,
        name: '史记',
        description: '《史记》是中国第一部纪传体通史，记录了上至上古传说中的黄帝时代，下至汉武帝期间共3000多年的历史。',
        price: 125.00,
        stock: 42,
        image: 'https://img3.doubanio.com/view/subject/s/public/s1953384.jpg',
        isAvailable: true,
        category: '历史',
        sales: 64
      },
      {
        id: 19,
        name: '追风筝的人',
        description: '《追风筝的人》是一部关于友情、背叛与救赎的小说，讲述了阿富汗的社会变迁和人性考验。',
        price: 35.00,
        stock: 8,
        image: 'https://img9.doubanio.com/view/subject/s/public/s1727290.jpg',
        isAvailable: true,
        category: '文学',
        sales: 215
      },
      {
        id: 20,
        name: '菊与刀',
        description: '《菊与刀》是一部研究日本文化的经典著作，深入剖析了日本社会的二元性格。',
        price: 38.00,
        stock: 0,
        image: 'https://img1.doubanio.com/view/subject/s/public/s1074166.jpg',
        isAvailable: false,
        category: '历史',
        sales: 103
      }
    ]
    
    loading.value = false
  }, 500) // 模拟网络延迟500ms
}

const router = useRouter()
// 筛选后的商品
const filteredProducts = computed(() => {
  let result = [...products.value]
  
  // 分类筛选 - 简化为名称匹配
  if (activeCategory.value !== '全部') {
    result = result.filter(product => 
      product.category === activeCategory.value
    )
  }
  
  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(product => 
      product.name.toLowerCase().includes(keyword) || 
      product.description.toLowerCase().includes(keyword)
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
    case 'salesDesc':
      result.sort((a, b) => (b.sales || 0) - (a.sales || 0))
      break
    case 'stockAsc':
      result.sort((a, b) => a.stock - b.stock)
      break
    case 'stockDesc':
      result.sort((a, b) => b.stock - a.stock)
      break
  }
  
  return result
})

// 切换分类
const changeCategory = (category: string) => {
  activeCategory.value = category
}

// 处理搜索
const handleSearch = () => {
  // 搜索功能通过计算属性自动实现
}

// 添加到购物车
const addToCart = (product: Product) => {
  ElMessage({
    message: `已将《${product.name}》加入购物车`,
    type: 'success'
  })
}

// 格式化价格
const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

// 页面加载时获取商品列表
onMounted(() => {
  loadProducts()
})
</script>

<template>
  <div class="mall-container">
    <!-- 顶部英雄区域 -->
    <div class="hero-section">
      <h1 class="hero-title">番茄读书商城</h1>
      <p class="hero-subtitle">发现您的下一本心爱之书</p>
      
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
      <!-- 分类导航 -->
      <el-card class="category-card">
        <div class="categories-wrapper">
          <div 
            v-for="category in categories" 
            :key="category"
            :class="['category-item', { active: activeCategory === category }]"
            @click="changeCategory(category)"
          >
            {{ category }}
          </div>
        </div>
        
        <!-- 排序选项 -->
        <div class="sort-wrapper">
          <el-select v-model="sortOption" class="sort-select" size="default">
            <el-option label="默认排序" value="default" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
            <el-option label="销量优先" value="salesDesc" />
            <el-option label="库存从低到高" value="stockAsc" />
            <el-option label="库存从高到低" value="stockDesc" />
          </el-select>
        </div>
      </el-card>
      
      <!-- 商品列表 -->
      <el-card class="products-card" v-loading="loading" element-loading-text="正在加载图书数据...">
        <div v-if="filteredProducts.length === 0 && !loading" class="empty-container">
          <el-empty description="没有找到符合条件的图书" />
        </div>
        
        <div v-else class="products-grid">
          <div v-for="product in filteredProducts" :key="product.id" class="product-card" @click="router.push(`/product/${product.id}`)">
            <!-- 商品可用状态标签 -->
            <div v-if="!product.isAvailable" class="product-status-tag unavailable">
              已下架
            </div>
            
            <div class="product-image">
              <img 
                :src="product.image || 'https://img3.doubanio.com/view/subject/s/public/s29053580.jpg'" 
                :alt="product.name"
              >
              <div class="product-overlay" v-if="product.isAvailable && product.stock > 0">
                <el-button type="primary" size="small" @click="addToCart(product)" class="cart-btn">
                  <el-icon><ShoppingCart /></el-icon> 加入购物车
                </el-button>
              </div>
              <div class="product-overlay out-of-stock" v-else-if="product.isAvailable && product.stock <= 0">
                <el-button type="info" size="small" disabled>
                  暂时缺货
                </el-button>
              </div>
            </div>
            
            <div class="product-info">
              <h3 class="product-title">{{ product.name }}</h3>
              <p class="product-description">{{ product.description }}</p>
              
              <div class="product-footer">
                <span class="product-price">{{ formatPrice(product.price) }}</span>
                
                <!-- 显示销量（如果有）或库存 -->
                <div class="product-meta">
                  <span v-if="product.sales" class="product-sales">销量: {{ product.sales }}</span>
                  <span class="product-stock">库存: {{ product.stock }}</span>
                </div>
              </div>
            </div>
            
            <!-- 热销标签 - 基于销量或库存模拟 -->
            <div class="product-tag" v-if="product.sales && product.sales > 50">热销</div>
            <div class="product-tag low-stock" v-else-if="product.stock < 10 && product.stock > 0">
              仅剩{{ product.stock }}件
            </div>
          </div>
        </div>
      </el-card>
    </main>
    
    <footer class="mall-footer">
      <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
    </footer>
  </div>
</template>

<style scoped>
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
  margin: 0 auto;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.content-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* 分类导航样式 */
.category-card {
  margin-bottom: 2rem;
  border-radius: 8px;
}

.categories-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 15px;
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

.products-card {
  margin-bottom: 2rem;
  min-height: 400px;
}

.empty-container {
  padding: 80px 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

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

.cart-btn:hover, .cart-btn:focus {
  background-color: #c0392b;
  border-color: #c0392b;
}

.product-overlay.out-of-stock {
  background: rgba(0, 0, 0, 0.4);
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

.product-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
}

.product-sales {
  color: #909399;
  margin-bottom: 3px;
}

.product-stock {
  color: #606266;
}

.product-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #e74c3c;
  color: white;
  padding: 3px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}

.product-tag.low-stock {
  background: #e67e22;
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
}
</style>