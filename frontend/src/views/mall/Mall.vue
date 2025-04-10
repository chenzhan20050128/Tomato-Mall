<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ShoppingCart } from '@element-plus/icons-vue'
import { getProductList, searchProducts, Product, updateProduct, deleteProduct } from '../../api/mall'
import { addToCart } from '../../api/cart'

const router = useRouter()
const products = ref<Product[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('全部')
const sortOption = ref('default')

// 管理员相关逻辑
const isAdmin = computed(() => sessionStorage.getItem('role') === 'admin')
const adminMode = ref(false)

// 分类映射 - 从规格中提取分类信息
const extractCategories = (products: Product[]): string[] => {
  const categories = new Set<string>(['全部'])
  
  products.forEach(product => {
    // 尝试从出版社获取分类
    const publisher = product.specifications?.find(s => s.item === '出版社')
    if (publisher) {
      categories.add(publisher.value)
    }
    
    // 尝试从副标题获取分类线索
    const subtitle = product.specifications?.find(s => s.item === '副标题')
    if (subtitle) {
      // 根据副标题中的关键词判断分类
      const value = subtitle.value.toLowerCase()
      if (value.includes('编程') || value.includes('程序') || value.includes('开发')) {
        categories.add('编程开发')
      } else if (value.includes('设计') || value.includes('交互')) {
        categories.add('设计')
      } else if (value.includes('经验') || value.includes('实践')) {
        categories.add('最佳实践')
      }
    }
  })
  
  return Array.from(categories)
}

// 分类列表
const categories = ref<string[]>(['全部'])

const loadProducts = async () => {
  loading.value = true
  
  try {
    const res = await getProductList()
    if (res.data.code == 200) { 
      products.value = res.data.data || []
      
      // 设置默认库存和可用状态
      products.value.forEach(product => {
        if (product.stock === undefined) {
          product.stock = 100
        }
        if (product.isAvailable === undefined) {
          product.isAvailable = true
        }
      })
      
      // 提取分类
      categories.value = extractCategories(products.value)
    } else {
      console.log(res.data.code == 200)
      ElMessage.error('获取商品列表失败')
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
  
  // 分类筛选
  if (activeCategory.value !== '全部') {
    result = result.filter(product => {
      const publisher = product.specifications?.find(s => s.item === '出版社')
      return publisher && publisher.value === activeCategory.value
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
  
  return result
})

// 切换分类
const changeCategory = (category: string) => {
  activeCategory.value = category
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await loadProducts()
    return
  }
  
  loading.value = true
  try {
    const res = await searchProducts(searchKeyword.value)
    if (res.data.code == 200) {
      products.value = res.data.data || []
      
      // 设置默认值
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
// 添加到购物车
const handleAddToCart = async (product: Product, event: Event) => {
  // 阻止事件冒泡，避免触发卡片点击跳转
  event.stopPropagation()
  
  // 检查登录状态
  if (!sessionStorage.getItem('token')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  try {
    const res = await addToCart(product.id, 1)
    if (res.data.code == 200) {
      ElMessage.success(`已将《${product.title}》加入购物车`)
    } else {
      ElMessage.error('添加购物车失败')
    }
  } catch (error) {
    console.error('添加购物车出错:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 格式化价格
const formatPrice = (price: number) => {
  return '¥' + price.toFixed(2)
}

// 图片加载错误处理
const handleImageError = (event: Event) => {
  if (event.target instanceof HTMLImageElement) {
    event.target.src = '/placeholder.jpg'
  }
}

// 从规格中获取作者
const getAuthor = (product: Product): string => {
  const authorSpec = product.specifications?.find(spec => spec.item === '作者')
  return authorSpec ? authorSpec.value : '未知作者'
}
// 处理管理员模式变更
const handleAdminModeChange = (value) => {
  adminMode.value = value
  if (value) {
    ElMessage.success('已启用管理员编辑模式')
  } else {
    ElMessage.info('已退出管理员编辑模式')
  }
}

// 删除商品
const handleDeleteProduct = async (productId) => {
  if (!isAdmin.value || !adminMode.value) return
  
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteProduct(productId)
    if (res.data.code == 200) {
      ElMessage.success('商品删除成功')
      // 重新加载商品列表
      loadProducts()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除商品出错:', error)
      ElMessage.error('操作失败，请重试')
    }
  }
}

// 显示编辑表单
const showEditForm = ref(false)
const currentEditProduct = ref<Product | null>(null)

const handleEditProduct = (product) => {
  currentEditProduct.value = JSON.parse(JSON.stringify(product)) // 深拷贝防止直接修改
  showEditForm.value = true
}

// 保存商品修改
const saveProductChanges = async () => {
  if (!currentEditProduct.value) return
  
  try {
    const res = await updateProduct(currentEditProduct.value)
    if (res.data.code == 200) {
      ElMessage.success('商品信息更新成功')
      showEditForm.value = false
      // 重新加载商品列表
      loadProducts()
    } else {
      ElMessage.error(res.data.msg || '更新失败')
    }
  } catch (error) {
    console.error('更新商品出错:', error)
    ElMessage.error('操作失败，请重试')
  }
}

// 页面加载时获取商品列表
onMounted(() => {
  loadProducts()
})
</script>

<template>
  <div class="mall-container" :class="{ 'admin-mode': adminMode }">
    <!-- 顶部区域 -->
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
    
    <!-- 管理员模式切换 -->
    <div v-if="isAdmin" class="admin-mode-toggle">
      <el-switch
        v-model="adminMode"
        active-text="管理员编辑模式"
        inactive-text="普通浏览模式"
        @change="handleAdminModeChange"
      />
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
            <el-option label="评分优先" value="rateDesc" />
          </el-select>
        </div>
      </el-card>
      
      <!-- 商品列表 -->
      <el-card class="products-card" v-loading="loading" element-loading-text="正在加载图书数据...">
        <div v-if="filteredProducts.length === 0 && !loading" class="empty-container">
          <el-empty description="没有找到符合条件的图书" />
        </div>
        
        <div v-else class="products-grid">
          <div 
            v-for="product in filteredProducts" 
            :key="product.id" 
            class="product-card" 
            @click="router.push(`/product/${product.id}`)"
          >
            <!-- 商品可用状态标签 -->
            <div v-if="!product.isAvailable" class="product-status-tag unavailable">
              暂无库存
            </div>
            
            <!-- 管理员模式下显示的按钮 -->
            <div v-if="isAdmin && adminMode" class="admin-actions">
              <el-button 
                type="primary" 
                size="small" 
                @click.stop="handleEditProduct(product)"
                icon="Edit"
              >
                修改
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click.stop="handleDeleteProduct(product.id)"
                icon="Delete"
              >
                删除
              </el-button>
            </div>
            
            <div class="product-image">
              <img 
                :src="product.cover" 
                :alt="product.title"
                @error="handleImageError"
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
      </el-card>
    </main>
    
    <footer class="mall-footer">
      <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
    </footer>

    <!-- 编辑商品的对话框 -->
    <el-dialog
      v-model="showEditForm"
      title="编辑商品信息"
      width="50%"
      :before-close="() => showEditForm = false"
    >
      <el-form v-if="currentEditProduct" label-width="100px" :model="currentEditProduct">
        <el-form-item label="商品名称">
          <el-input v-model="currentEditProduct.title"></el-input>
        </el-form-item>
        <el-form-item label="商品价格">
          <el-input-number v-model="currentEditProduct.price" :precision="2" :step="0.1" :min="0"></el-input-number>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-input v-model="currentEditProduct.cover" placeholder="图片URL"></el-input>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="currentEditProduct.description" type="textarea" rows="3"></el-input>
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input v-model="currentEditProduct.detail" type="textarea" rows="5"></el-input>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditForm = false">取消</el-button>
          <el-button type="primary" @click="saveProductChanges">保存修改</el-button>
        </span>
      </template>
    </el-dialog>
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

.admin-mode-toggle {
  margin: 15px auto;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
  max-width: 1200px;
  display: flex;
  justify-content: flex-end;
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

.admin-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 5px;
  z-index: 10;
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

/* 管理员模式样式 */
.admin-mode .product-card {
  border: 1px dashed #e74c3c;
}

.admin-mode .product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(231, 76, 60, 0.3);
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
  
  .admin-actions {
    flex-direction: column;
    top: 5px;
    right: 5px;
  }
  
  .admin-actions .el-button {
    padding: 4px 8px;
    font-size: 12px;
  }
}
</style>