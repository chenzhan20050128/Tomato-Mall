<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Trophy, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { addToCart } from '../../api/cart'
import { navigateWithTransition } from '../../utils/transition'
import emitter from '../../utils/eventBus'
import { getBestSellers } from '../../api/recommend'


// 定义畅销书商品类型，匹配后端 ProductRankingDTO
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
    rank?: number  // 前端计算的排名
    isAvailable?: boolean  // 前端计算的可用性
}
// CLC分类映射
const clcMapping = {
//  'A': '马克思主义、列宁主义、毛泽东思想、邓小平理论',
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
const router = useRouter()
const loading = ref(false)
const products = ref<ProductRanking[]>([])
const searchKeyword = ref('')

// 筛选参数
const activeCategory = ref('全部')
const timePeriod = ref(30) // 默认30天
const displayLimit = ref(10) // 默认显示10项

// 分页相关状态
const currentPage = ref(1)
const pageSize = ref(10)
const categoryPage = ref(1)
const categoryPageSize = ref(8)
const showAllCategories = ref(false)

// 预设时间周期选项
const timePeriodOptions = [
    { label: '近7天', value: 7 },
    { label: '近30天', value: 30 },
    { label: '近90天', value: 90 },
    { label: '近365天', value: 365 },
]

// 分类列表，初始包含"全部"，后续从API获取的商品中提取
const categories = ref<string[]>(['全部'])
// 加载畅销书数据
const loadBestSellers = async () => {
    loading.value = true

    try {
        // 构建请求参数对象
        const params = {
            category: activeCategory.value !== '全部' ? categoryNameToCode.value[activeCategory.value] : undefined,
            days: timePeriod.value,
            limit: displayLimit.value
        }

        // 使用API函数发送请求
        const response = await getBestSellers(params)

        // 调试输出
        console.log('API Response:', response)

        // 处理返回数据 - 注意数据结构变化
        if (response.data.code === '200') {
            // 产品数据在response.data.data而不是response.data.result
            const productsData = response.data.data || []

            // 添加排名和可用性信息
            products.value = productsData.map((item, index) => ({
                ...item,
                rank: index + 1, // 添加排名
                isAvailable: item.stock > 0 // 判断是否有库存
            }))

            // 提取商品分类列表
            updateCategories()

            // 重置页码
            currentPage.value = 1
        } else {
            ElMessage.error(`获取畅销书列表失败: ${response.data.msg || '未知错误'}`)
        }
    } catch (error) {
        console.error('获取畅销书列表出错:', error)
        ElMessage.error('网络错误，请稍后重试')
    } finally {
        loading.value = false
    }
}
// 从商品数据中提取分类，并确保所有CLC分类都显示
const updateCategories = () => {
    // 先添加全部选项
    const categoriesSet = new Set<string>(['全部'])
    categoryCodeToName.value = {}
    categoryNameToCode.value = {}

    // 添加所有CLC分类，即使没有对应的书籍
    for (const code in clcMapping) {
        const name = clcMapping[code]
        categoriesSet.add(name)
        categoryCodeToName.value[code] = name
        categoryNameToCode.value[name] = code
    }

    // 从商品中获取其他可能的分类
    products.value.forEach(product => {
        if (product.category) {
            // 如果是分类代码，转换为名称
            const categoryName = clcMapping[product.category] || product.category
            categoriesSet.add(categoryName)

            // 保存代码和名称的映射关系
            if (!categoryCodeToName.value[product.category]) {
                categoryCodeToName.value[product.category] = categoryName
            }
            if (!categoryNameToCode.value[categoryName]) {
                categoryNameToCode.value[categoryName] = product.category
            }
        }
    })

    // 转换为数组并排序
    categories.value = Array.from(categoriesSet).sort((a, b) => {
        if (a === '全部') return -1
        if (b === '全部') return 1

        // 获取分类代码进行排序
        const codeA = categoryNameToCode.value[a] || ''
        const codeB = categoryNameToCode.value[b] || ''

        return codeA.localeCompare(codeB)
    })
}

// 处理分类筛选
const changeCategory = (category: string) => {
    activeCategory.value = category
    currentPage.value = 1
    loadBestSellers()
}

// 处理时间周期变更
const handleTimePeriodChange = () => {
    currentPage.value = 1
    loadBestSellers()
}

// 处理搜索
const handleSearch = () => {
    currentPage.value = 1
    // 前端搜索实现
    if (!searchKeyword.value) {
        loadBestSellers()
    }
}

// 过滤后的商品列表（本地搜索）
const filteredProducts = computed(() => {
    let result = products.value

    // 分类筛选
    if (activeCategory.value !== '全部') {
        result = result.filter(product => {
            const categoryCode = categoryNameToCode.value[activeCategory.value]
            return product.category === categoryCode
        })
    }

    // 关键词搜索
    if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        result = result.filter(product =>
            product.title.toLowerCase().includes(keyword) ||
            product.description.toLowerCase().includes(keyword) ||
            product.author.toLowerCase().includes(keyword)
        )
    }

    return result
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

// 显示的分类标签
const displayCategories = computed(() => {
    if (showAllCategories.value) {
        return categories.value
    }

    const start = (categoryPage.value - 1) * categoryPageSize.value
    const end = start + categoryPageSize.value
    return categories.value.slice(start, end)
})

// 分类总页数
const totalCategoryPages = computed(() => {
    return Math.ceil(categories.value.length / categoryPageSize.value)
})

// 分类页码切换
const handleCategoryPageChange = (direction: 'prev' | 'next') => {
    if (direction === 'prev' && categoryPage.value > 1) {
        categoryPage.value--
    } else if (direction === 'next' && categoryPage.value < totalCategoryPages.value) {
        categoryPage.value++
    }
}

// 切换显示所有分类
const toggleShowAllCategories = () => {
    showAllCategories.value = !showAllCategories.value
    if (!showAllCategories.value) {
        categoryPage.value = 1
    }
}

// 页码变更
const handlePageChange = (page: number) => {
    currentPage.value = page
    // 滚动到商品列表顶部
    document.querySelector('.bestseller-products')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    })
}

// 添加到购物车
const handleAddToCart = async (product: ProductRanking, event: Event) => {
    event.stopPropagation()

    if (!sessionStorage.getItem('token')) {
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

// 获取销量文本
const getSalesText = (count: number): string => {
    if (count > 10000) {
        return (count / 10000).toFixed(1) + '万册'
    }
    return count + '册'
}

onMounted(() => {
    // 初始化所有分类
    updateCategories()
    loadBestSellers()
})
</script>

<template>
    <div class="bestseller-container">
        <!-- 顶部区域 -->
        <div class="hero-section">
            <h1 class="hero-title">畅销书榜单</h1>
            <p class="hero-subtitle">发现最受欢迎的热门图书</p>

            <!-- 搜索区域 -->
            <div class="search-container">
                <el-input v-model="searchKeyword" placeholder="搜索书名、作者或描述..." class="search-input"
                    @keyup.enter="handleSearch">
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

        <main class="content-section">
            <!-- 筛选选项 -->
            <div class="categories-header">
                <h3>图书分类</h3>
                <!-- 移除分页控制和展开/收起按钮 -->
            </div>
            <div class="categories-wrapper">
                <div v-for="category in categories" :key="category"
                    :class="['category-item', { active: activeCategory === category }]"
                    @click="changeCategory(category)">
                    {{ category }}
                </div>
            </div>

            <!-- 畅销书列表 -->
            <el-card class="bestseller-products" v-loading="loading" element-loading-text="正在加载畅销书数据...">
                <div class="products-header">
                    <span class="products-count">
                        共找到 {{ filteredProducts.length }} 本畅销图书
                        <span v-if="activeCategory !== '全部'" class="filter-info">
                            (分类: {{ activeCategory }})
                        </span>
                        <span v-if="searchKeyword" class="filter-info">
                            (搜索: "{{ searchKeyword }}")
                        </span>
                    </span>
                </div>

                <div v-if="paginatedProducts.length === 0 && !loading" class="empty-container">
                    <el-empty description="没有找到符合条件的畅销书" />
                </div>

                <div v-else class="bestseller-list">
                    <div v-for="product in paginatedProducts" :key="product.productId" class="bestseller-item"
                        @click="(e) => navigateToProduct(product, e)">
                        <!-- 排名标志 -->
                        <div class="rank-badge" :class="{
                            'top-1': product.rank === 1,
                            'top-2': product.rank === 2,
                            'top-3': product.rank === 3
                        }">
                            <el-icon v-if="product.rank !== undefined && product.rank <= 3">
                                <Trophy />
                            </el-icon>
                            {{ product.rank }}
                        </div>



                        <div class="product-image">
                            <img :src="product.cover" :alt="product.title" @error="handleImageError"
                                referrerpolicy="no-referrer" rossorigin="anonymous"
                                :style="{ viewTransitionName: `product-image-${product.productId}` }">
                            <div class="product-overlay" v-if="product.isAvailable">
                                <el-button type="primary" size="small" @click="(e) => handleAddToCart(product, e)"
                                    class="cart-btn">
                                    <el-icon>
                                        <ShoppingCart />
                                    </el-icon> 加入购物车
                                </el-button>
                            </div>
                        </div>

                        <div class="product-info">
                            <h3 class="product-title">{{ product.title }}</h3>
                            <p class="product-author">{{ product.author }}</p>
                            <p class="product-description">{{ product.description }}</p>

                            <div class="product-footer">
                                <span class="product-price">{{ formatPrice(product.price) }}</span>
                                <div class="product-stats">
                                    <div class="product-rating">
                                        <span class="rating-count" v-if="product.ratingCount">({{ product.ratingCount
                                        }}人评分)</span>
                                    </div>
                                    <div class="product-sales">
                                        销量: <span>{{ getSalesText(product.sales) }}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 分页组件 -->
                <div v-if="totalPages > 1" class="pagination-container">
                    <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
                        :total="filteredProducts.length" :page-sizes="[10, 20, 30, 50]"
                        layout="total, sizes, prev, pager, next, jumper" @current-change="handlePageChange"
                        @size-change="(size) => { pageSize = size; currentPage = 1; }" background />
                </div>
            </el-card>
        </main>

        <footer class="bestseller-footer">
            <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
        </footer>
    </div>
</template>

<style scoped>
.bestseller-container {
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

/* 筛选卡片样式 */
.filter-card {
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

/* 调整分类展示区样式 */
.categories-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 15px;
  min-height: 40px;
  max-height: none; /* 移除高度限制 */
}

.category-item {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #f5f7fa;
  border: 1px solid #e0e0e0;
  margin-bottom: 8px; /* 添加底部间距 */
}

/* 如果分类过多，可以考虑减小每个分类项的字体和内边距 */
@media (min-width: 768px) and (max-width: 1200px) {
  .category-item {
    padding: 4px 12px;
    font-size: 13px;
  }
}

/* 在更小的屏幕上进一步调整 */
@media (max-width: 767px) {
  .category-item {
    padding: 3px 10px;
    font-size: 12px;
  }
}
.category-item:hover {
    background-color: #f0f0f0;
}

.category-item.active {
    background-color: #e74c3c;
    color: white;
    border-color: #e74c3c;
}

.time-filter {
    margin-top: 15px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;
}

.filter-label {
    font-size: 14px;
    color: #606266;
}

.display-limit {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 8px;
}

/* 畅销书列表样式 */
.bestseller-products {
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

.bestseller-list {
    display: grid;
    grid-template-columns: 1fr;
    gap: 20px;
}

.bestseller-item {
    display: flex;
    background: white;
    border-radius: 8px;
    overflow: hidden;
    position: relative;
    transition: transform 0.3s, box-shadow 0.3s;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    min-height: 200px;
    cursor: pointer;
}

.bestseller-item:hover {
    transform: translateY(-5px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.15);
}

.rank-badge {
    position: absolute;
    top: 15px;
    left: 15px;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: #606266;
    color: white;
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: bold;
    font-size: 16px;
    z-index: 10;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.rank-badge.top-1 {
    background-color: #f39c12;
    width: 42px;
    height: 42px;
    font-size: 18px;
}

.rank-badge.top-2 {
    background-color: #bdc3c7;
    width: 40px;
    height: 40px;
    font-size: 17px;
}

.rank-badge.top-3 {
    background-color: #d35400;
    width: 38px;
    height: 38px;
    font-size: 16px;
}

.product-image {
    width: 180px;
    min-width: 180px;
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

.bestseller-item:hover .product-image img {
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

.bestseller-item:hover .product-overlay {
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
    flex: 1;
    padding: 20px;
    display: flex;
    flex-direction: column;
    position: relative;
}

.product-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 8px;
    color: #303133;
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
    margin-top: auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.product-price {
    font-size: 20px;
    font-weight: 600;
    color: #e74c3c;
}

.product-stats {
    display: flex;
    gap: 15px;
    align-items: center;
}

.product-rating {
    background: #f0f7ff;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 600;
    color: #409eff;
}

.rating-count {
    font-size: 12px;
    color: #909399;
    font-weight: normal;
}

.product-sales {
    font-size: 14px;
    color: #67c23a;
    font-weight: 500;
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

/* 分页样式 */
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

.bestseller-footer {
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

    .bestseller-item {
        flex-direction: column;
    }

    .product-image {
        width: 100%;
        height: 200px;
    }

    .rank-badge {
        top: 10px;
        left: 10px;
    }

    .time-filter {
        flex-direction: column;
        align-items: flex-start;
    }

    .display-limit {
        margin-left: 0;
        margin-top: 12px;
    }

    .categories-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 10px;
    }

    .category-controls {
        align-self: stretch;
        justify-content: space-between;
    }
}
</style>