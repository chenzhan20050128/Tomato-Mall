<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Star, HotWater, Trophy, Medal } from '@element-plus/icons-vue'
import BestSeller from './BestSeller.vue'
import TopRate from './TopRate.vue'
import RandomRecommend from './RandomRecommend.vue'

// 推荐菜单项
const recommendMenus = [
    { key: 'bestseller', label: '畅销榜', icon: HotWater, description: '发现最畅销的热门图书' },
    { key: 'toprate', label: '评分榜', icon: Medal, description: '最受读者好评的高分图书' },
    { key: 'random', label: '猜你喜欢', icon: Star, description: '根据您的阅读习惯个性化推荐' }
]

const route = useRoute()
const router = useRouter()

// 当前激活的推荐类型
const activeTab = ref('bestseller')

// 监听路由变化更新标签
watch(() => route.query.type, (newType) => {
    if (newType) {
        activeTab.value = newType as string
    }
}, { immediate: true })

// 切换标签时更新URL
const handleTabChange = (tab: string) => {
    activeTab.value = tab
    router.push({ path: '/recommend', query: { type: tab } })
}

onMounted(() => {
    // 如果URL中没有指定类型，默认设置为bestseller
    if (!route.query.type) {
        router.replace({ path: '/recommend', query: { type: activeTab.value } })
    }
})
</script>

<template>
    <div class="recommend-container">
        <!-- 顶部导航栏 -->
        <div class="recommend-header">
            <h1 class="recommend-title">图书推荐</h1>
            <p class="recommend-description">
                {{recommendMenus.find(item => item.key === activeTab)?.description}}
            </p>
        </div>

        <!-- 导航菜单 -->
        <div class="recommend-tabs">
            <div class="tabs-container">
                <div v-for="menu in recommendMenus" :key="menu.key"
                    :class="['tab-item', { active: activeTab === menu.key }]" @click="handleTabChange(menu.key)">
                    <el-icon>
                        <component :is="menu.icon" />
                    </el-icon>
                    <span>{{ menu.label }}</span>
                </div>
            </div>
        </div>

        <!-- 内容区域 -->
        <div class="recommend-content">
            <!-- 使用keep-alive保持组件状态 -->
            <keep-alive>
                <component :is="activeTab === 'bestseller'
                    ? BestSeller
                    : activeTab === 'toprate'
                        ? TopRate
                        : RandomRecommend" />
            </keep-alive>
        </div>
    </div>
</template>

<style scoped>
.recommend-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
}

.recommend-header {
    text-align: center;
    padding: 2rem 1rem 1rem;
    background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
    color: white;
}

.recommend-title {
    font-size: 2rem;
    margin-bottom: 0.5rem;
}

.recommend-description {
    font-size: 1rem;
    opacity: 0.8;
}

.recommend-tabs {
    position: sticky;
    top: 60px;
    /* 与Header高度匹配 */
    z-index: 10;
    background-color: white;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.tabs-container {
    display: flex;
    max-width: 1200px;
    margin: 0 auto;
    border-bottom: 1px solid #ebeef5;
}

.tab-item {
    padding: 1rem 2rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    transition: all 0.3s;
    position: relative;
    color: #606266;
}

.tab-item:hover {
    color: #e74c3c;
    background-color: rgba(231, 76, 60, 0.05);
}

.tab-item.active {
    color: #e74c3c;
    font-weight: 500;
}

.tab-item.active::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 2px;
    background-color: #e74c3c;
}

.recommend-content {
    width: 100%;
}

.placeholder-content {
    padding: 120px 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.placeholder-icon {
    color: #dcdfe6;
    margin-bottom: 20px;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .recommend-title {
        font-size: 1.5rem;
    }

    .recommend-description {
        font-size: 0.875rem;
    }

    .tabs-container {
        justify-content: space-between;
    }

    .tab-item {
        padding: 0.75rem 1rem;
        font-size: 0.875rem;
    }
}

@media (max-width: 480px) {
    .tab-item {
        flex-direction: column;
        padding: 0.5rem 0.75rem;
        font-size: 0.75rem;
    }
}
</style>