<script setup lang="ts">
import { ref, watch, inject, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { House, ShoppingCart, User, UserFilled, Plus, SwitchButton, ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 使用 ref 控制登录状态
const isLoggedIn = ref(false)
const username = ref('')

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
  { icon: ShoppingCart, title: '图书商城', path: '/mall' }
]

const currentPath = ref(route.path)

// 监听路由变化
watch(
  () => route.path,
  (newPath) => {
    checkLoginStatus()
    currentPath.value = newPath
  }
)

// 检查登录状态
const checkLoginStatus = () => {
  const token = sessionStorage.getItem('token')
  const storedUsername = sessionStorage.getItem('username')
  isLoggedIn.value = !!token
  username.value = storedUsername || ''
}

// 初始化时检查登录状态
onMounted(() => {
  checkLoginStatus()
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
          <template v-if="!isLoggedIn">
            <el-menu-item index="/login">
              <el-icon>
                <User />
              </el-icon>
              <span>登录</span>
            </el-menu-item>
            <el-menu-item index="/register">
              <el-icon>
                <Plus />
              </el-icon>
              <span>注册</span>
            </el-menu-item>
          </template>

          <template v-else>
            <el-menu-item index="/profile">
              <el-icon>
                <UserFilled />
              </el-icon>
              <span>{{ username }}</span>
            </el-menu-item>
            <el-menu-item class="logout-item" @click="handleLogout">
              <el-icon>
                <SwitchButton />
              </el-icon>
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
</style>