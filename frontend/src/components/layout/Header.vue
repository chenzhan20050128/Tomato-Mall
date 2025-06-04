<script setup lang="ts">
import { ref, watch, inject, onMounted, onUnmounted,computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { House, ShoppingCart, User, UserFilled, Plus, SwitchButton, ArrowDown, ArrowRight, ShoppingBag, Document, Star } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
// 导入获取购物车数据的API
import { getCartItems } from '../../api/cart'
// 导入事件总线
import emitter from '../../utils/eventBus'

const router = useRouter()
const route = useRoute()

// 使用 ref 控制登录状态
const isLoggedIn = ref(false)
const username = ref('')
const cartItemCount = ref(0) // 新增购物车数量变量

const isMenuOpen = ref(false)

// 窗口监听
const viewport = inject('viewport', {
  isMobile: ref(false),
  viewportWidth: ref(0),
  breakpoints: { md: 768 }
})

// 计算是否需要折叠菜单
const needCollapseMenu = computed(() => {
  return viewport.viewportWidth.value < viewport.breakpoints.md
})

// 功能栏 - 修改为适合项目的导航项，添加商城入口
const funcs = [
  { icon: House, title: '首页', path: '/home' },
  { icon: ShoppingCart, title: '图书商城', path: '/mall' },
  { icon: Star, title: '图书推荐', path: '/recommend' }
]
const currentPath = ref(route.path)

// 获取购物车数量
const fetchCartItemCount = async () => {
  if (!isLoggedIn.value) {
    cartItemCount.value = 0
    return
  }
  
  try {
    const response = await getCartItems()
    
    // 正确解析后端返回的CartVO对象
    if (response && response.data && response.data.code === '200') {
      if (response.data.data && Array.isArray(response.data.data.items)) {
        cartItemCount.value = response.data.data.items.length
      } else {
        cartItemCount.value = 0
      }
    } else {
      cartItemCount.value = 0
    }
  } catch (error) {
    console.error('获取购物车数据失败:', error)
    cartItemCount.value = 0
  }
}

// 监听路由变化
watch(
  () => route.path,
  (newPath) => {
    checkLoginStatus()
    currentPath.value = newPath
    // 路由变化时更新购物车数量
    fetchCartItemCount()
  }
)

// 监听登录状态变化
watch(
  () => isLoggedIn.value,
  (newValue) => {
    if (newValue) {
      fetchCartItemCount()
    } else {
      cartItemCount.value = 0
    }
  }
)

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('token')
  const storedUsername = sessionStorage.getItem('username')
  isLoggedIn.value = !!token
  username.value = storedUsername || ''
}

// 初始化时检查登录状态和购物车数量并监听更新事件
onMounted(() => {
  checkLoginStatus()
  fetchCartItemCount()
  
  // 监听购物车更新事件
  emitter.on('updateCartCount', fetchCartItemCount)
})

// 组件销毁时移除事件监听
onUnmounted(() => {
  emitter.off('updateCartCount', fetchCartItemCount)
})

// 登出处理
const handleLogout = () => {
  ElMessageBox.confirm(
    '是否要退出登录？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      center: true
    }
  ).then(() => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    isLoggedIn.value = false
    cartItemCount.value = 0
    router.push('/login')
  }).catch(() => {})
}
</script>

<template>
  <el-header>
    <el-affix>
      <el-menu class="custom-header" mode="horizontal" :ellipsis="false" router :default-active="currentPath">
        <!-- 品牌区域 -->
        <div class="brand-area" @click="router.push('/home')">
          <h1 class="brand-title">番茄读书</h1>
        </div>
        
        <!-- 功能区 - 小屏幕下的折叠菜单 -->
        <div v-if="needCollapseMenu" class="nav-group">
          <el-menu-item index="">
            <el-dropdown trigger="click" @visible-change="(v) => isMenuOpen = v">
              <div class="header-item">
                <el-icon>
                  <component :is="isMenuOpen ? ArrowDown : ArrowRight" />
                </el-icon>
                <span>功能菜单</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="mobile-menu">
                  <el-dropdown-item v-for="(item, index) in funcs" :key="index" @click="router.push(item.path)">
                    <el-icon :size="16">
                      <component :is="item.icon" />
                    </el-icon>
                    <span style="margin-left: 8px">{{ item.title }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-menu-item>
        </div>

        <!-- 功能区 - 宽屏直接展示所有菜单 -->
        <div v-else class="nav-group">
          <el-menu-item v-for="(item, index) in funcs" :key="index" :index="item.path">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </div>

        <!-- 用户区 -->
        <div class="nav-group user-area">
          <!-- 非登录状态下显示登录和注册 -->
          <template v-if="!isLoggedIn">
            <!-- 购物车按钮 -->
            <el-menu-item index="/cart">
              <el-badge :value="cartItemCount" :hidden="cartItemCount === 0" class="cart-badge">
                <el-icon><ShoppingBag /></el-icon>
              </el-badge>
              <span>购物车</span>
            </el-menu-item>
            
            <el-menu-item index="/login">
              <el-icon><User /></el-icon>
              <span>登录</span>
            </el-menu-item>
            
            <el-menu-item index="/register">
              <el-icon><Plus /></el-icon>
              <span>注册</span>
            </el-menu-item>
          </template>

          <!-- 登录状态下显示的菜单项 -->
          <template v-else>
            <!-- 购物车按钮 -->
            <el-menu-item index="/cart">
              <el-badge :value="cartItemCount" :hidden="cartItemCount === 0" class="cart-badge">
                <el-icon><ShoppingBag /></el-icon>
              </el-badge>
              <span>购物车</span>
            </el-menu-item>
            
            <!-- 订单按钮 -->
            <el-menu-item index="/order">
              <el-icon><Document /></el-icon>
              <span>订单</span>
            </el-menu-item>
            
            <!-- 用户个人中心 -->
            <el-menu-item index="/profile">
              <el-icon><UserFilled /></el-icon>
              <span>{{ username }}</span>
            </el-menu-item>
            
            <!-- 退出按钮 -->
            <el-menu-item class="logout-item" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出</span>
            </el-menu-item>
          </template>
        </div>
      </el-menu>
    </el-affix>
  </el-header>
</template>

<style scoped>
.el-header {
  --el-header-padding: 0;
  --el-header-height: 60px;
  padding: 0;
}

/* 设置菜单基本样式 */
.custom-header {
  height: var(--el-header-height);
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background-color: #fff;
}

/* 覆盖Element Plus默认菜单活动状态颜色 */
:deep(.el-menu--horizontal .el-menu-item.is-active) {
  color: #e74c3c !important;
  border-bottom-color: #e74c3c !important;
}

/* 悬停效果 */
:deep(.el-menu--horizontal .el-menu-item:not(.is-disabled):hover) {
  color: #e74c3c;
}

.brand-area {
  display: flex;
  align-items: center;
  margin-right: 20px;
  cursor: pointer;
}

.brand-title {
  color: #e74c3c;
  margin: 0;
  font-size: 1.5rem;
  padding: 0 16px;
}

.nav-group {
  display: flex;
  align-items: center;
}

.user-area {
  margin-left: auto; /* 确保用户区域在右侧 */
}

.el-menu-item {
  height: var(--el-header-height);
  padding: 0 16px;
  transition: all 0.2s;
}

/* 退出按钮特殊样式 */
.logout-item {
  color: #e74c3c !important;
}

.logout-item:hover {
  background-color: rgba(231, 76, 60, 0.1) !important;
}

.logout-item :deep(.el-icon) {
  color: #e74c3c !important;
}

.header-item {
  display: flex;
  align-items: center;
  color: var(--el-menu-text-color);
  transition: all 0.2s;
  padding: 0 4px;
}

.header-item:hover {
  color: #e74c3c;
  background-color: rgba(231, 76, 60, 0.1);
}

.header-item .el-icon {
  margin-right: 4px;
  font-size: 18px;
}

.header-item span {
  font-size: 14px;
}

/* 下拉菜单项样式 */
:deep(.el-dropdown-menu__item) {
  color: var(--el-text-color);
  font-size: 14px;
  display: flex;
  align-items: center;
  padding: 12px 20px;
}

:deep(.el-dropdown-menu__item):hover {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.1);
}

/* 优化小屏幕下的布局 */
@media (max-width: 768px) {
  .brand-title {
    font-size: 1.3rem;
    padding: 0 10px;
  }
  
  .el-menu-item {
    padding: 0 10px;
  }
}

/* 修复购物车徽章定位 */
.cart-badge {
  position: relative; /* 确保相对定位 */
}

.cart-badge :deep(.el-badge__content) {
  transform: scale(0.8);
  right: 5px;   /* 调整右侧位置，值改为正数 */
  top: 5px;     /* 保持顶部位置 */
  z-index: 2;   /* 确保在其他元素之上 */
}
</style>