<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Star, Filter } from '@element-plus/icons-vue'
import { getProductList, Product } from '../../api/mall'

// 商品列表数据
const products = ref<Product[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('全部')
const sortOption = ref('default')

// 假设的产品分类（实际应从后端获取或根据现有产品提取）
const categories = ref([
  '全部', '手机', '电脑', '配件', '平板', '智能设备'
])

// 加载商品列表
const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getProductList()
    if (res.data.code === '200') {
      products.value = res.data.data || []
      
      // 如果API没有返回sales字段，为每个产品添加一个模拟的销量
      products.value.forEach(product => {
        if (product.sales === undefined) {
          // 使用库存的一定比例作为模拟销量
          product.sales = Math.floor(product.stock * (0.5 + Math.random() * 0.5))
        }
      })
    } else {
      ElMessage.error(res.data.msg || '获取商品列表失败')
    }
  } catch (error) {
    console.error('获取商品列表出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 筛选后的商品
const filteredProducts = computed(() => {
  let result = [...products.value]
  
  // 分类筛选 - 简化为名称匹配（实际项目应该使用categories字段）
  if (activeCategory.value !== '全部') {
    result = result.filter(product => 
      product.name.includes(activeCategory.value) || 
      product.description.includes(activeCategory.value)
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
    message: `已将 ${product.name} 加入购物车`,
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
    <!-- 顶部搜索和过滤区域 -->
    <div class="mall-header">
      <div class="search-wrapper">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品名称、描述..."
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
      
      <div class="filter-sort">
        <el-select v-model="sortOption" class="sort-select">
          <el-option label="默认排序" value="default" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="销量优先" value="salesDesc" />
          <el-option label="库存从低到高" value="stockAsc" />
          <el-option label="库存从高到低" value="stockDesc" />
        </el-select>
      </div>
    </div>
    
    <!-- 分类导航 -->
    <div class="categories-nav">
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
    </div>
    
    <!-- 商品列表 -->
    <div class="products-section">
      <div v-if="loading" class="loading-container">
        <el-icon class="loading-icon"><i class="el-icon-loading"></i></el-icon>
        <p>正在加载商品数据...</p>
      </div>
      
      <div v-else-if="filteredProducts.length === 0" class="empty-container">
        <el-empty description="没有找到符合条件的商品" />
      </div>
      
      <div v-else class="products-grid">
        <div v-for="product in filteredProducts" :key="product.id" class="product-card">
          <!-- 商品可用状态标签 -->
          <div v-if="!product.isAvailable" class="product-status-tag unavailable">
            已下架
          </div>
          
          <div class="product-image">
            <img 
              :src="product.image || 'https://shadow.elemecdn.com/app/element/hamburger.9cf7b091-55e9-11e9-a976-7f4d0b07eef6.png'" 
              :alt="product.name"
            >
            <div class="product-overlay" v-if="product.isAvailable && product.stock > 0">
              <el-button type="primary" size="small" @click="addToCart(product)">
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
    </div>
  </div>
</template>

<style scoped>
.mall-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.mall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-wrapper {
  flex: 1;
  max-width: 600px;
}

.search-input {
  width: 100%;
}

.filter-sort {
  margin-left: 20px;
  display: flex;
  align-items: center;
}

.sort-select {
  width: 140px;
}

/* 分类导航样式 */
.categories-nav {
  margin-bottom: 24px;
  border-radius: 8px;
  background-color: #f9f9f9;
  padding: 12px;
}

.categories-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-item {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fff;
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

/* 商品列表样式 */
.products-section {
  min-height: 400px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #909399;
}

.loading-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.empty-container {
  padding: 80px 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.product-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.product-image {
  height: 220px;
  overflow: hidden;
  position: relative;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

/* 响应式调整 */
@media (max-width: 768px) {
  .mall-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-wrapper {
    max-width: none;
    margin-bottom: 15px;
  }
  
  .filter-sort {
    margin-left: 0;
    justify-content: space-between;
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