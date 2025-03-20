<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateToLogin = () => {
    router.push('/login')
}

const navigateToRegister = () => {
    router.push('/register')
}
</script>

<template>
    <div class="home-container">

        <div class="hero-section">
            <h1 class="hero-title">欢迎来到番茄读书商城</h1>
            <p class="hero-subtitle">您的线上实体书本购买平台</p>
        </div>

        <main class="content-section">
            <el-card class="intro-card">
                <h2>关于番茄读书商城</h2>
                <p>近年来，随着人们对知识的追求和文化消费的不断升级，实体书本市场展现出强劲的复苏与增长潜力。尽管数字阅读的浪潮汹涌，但实体书本所具有的独特质感、收藏价值以及阅读仪式感，使其在文化消费领域中依然占据着不可替代的地位。越来越多的读者开始回归实体书本，享受翻阅纸张的乐趣和深度阅读带来的精神滋养。与此同时，线上购物的便捷性也为实体书本的销售提供了新的机遇。
                </p>

                <p>番茄读书敏锐地捕捉到这一市场趋势，拟开展线上商城购买实体书本业务，旨在为读者提供更加便捷、丰富的购书体验。通过线上商城，顾客可以轻松浏览海量的书目，获取精准的书籍推荐，享受快速配送服务，同时还能参与线上线下互动活动，拓展阅读视野。对于出版社和作者而言，番茄读书的线上商城将提供一个全新的展示与销售渠道，助力优质书籍触达更广泛的读者群体，推动文化市场的繁荣发展。
                </p>
            </el-card>

            <el-row :gutter="20" class="service-section">
                <el-col :xs="24" :sm="12">
                    <el-card class="service-card">
                        <div class="service-icon customer-icon">👤</div>
                        <h3>顾客服务</h3>
                        <ul>
                            <li>便捷的注册和登录系统</li>
                            <li>个人信息查看与修改</li>
                            <li>丰富的书籍浏览功能</li>
                            <li>简单高效的购买流程</li>
                            <li>订单跟踪与历史记录</li>
                        </ul>
                    </el-card>
                </el-col>

                <el-col :xs="24" :sm="12">
                    <el-card class="service-card">
                        <div class="service-icon admin-icon">👨‍💼</div>
                        <h3>管理员功能</h3>
                        <ul>
                            <li>全面的书籍信息管理</li>
                            <li>库存更新与调整</li>
                            <li>订单处理与管理</li>
                            <li>支付流程监控</li>
                            <li>用户数据分析</li>
                        </ul>
                    </el-card>
                </el-col>
            </el-row>

            <el-card class="vision-card">
                <h2>我们的愿景</h2>
                <p>番茄读书的线上商城业务将围绕顾客和管理员两大核心服务对象展开。顾客可以便捷地注册、登录，查看与修改个人信息，浏览并购买心仪的实体书本。管理员则负责管理书籍信息，包括更新、创建、删除书籍，调整库存，以及处理订单和支付等事务。通过精准的市场定位与优质的服务体验，番茄读书的线上商城有望成为实体书本销售领域的一颗璀璨明珠，为读者和出版界搭建起一座高效、便捷的文化交流桥梁。
                </p>
            </el-card>
        </main>

        <footer class="home-footer">
            <p>© 2025 番茄读书商城 - 南京大学软件工程与计算2</p>
        </footer>
    </div>
</template>

<style scoped>
.home-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
}

.hero-section {
    text-align: center;
    padding: 4rem 2rem;
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
}

.content-section {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
}

.intro-card {
    margin-bottom: 2rem;
}

.intro-card h2 {
    color: #e74c3c;
    margin-bottom: 1rem;
}

.intro-card p {
    line-height: 1.6;
    margin-bottom: 1rem;
    text-align: justify;
}

.service-section {
    margin-bottom: 2rem;
}

.service-card {
    height: 100%;
    transition: transform 0.3s;
}

.service-card:hover {
    transform: translateY(-5px);
}

.service-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 1rem;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 3rem;
}

.service-card h3 {
    text-align: center;
    color: #e74c3c;
    margin-bottom: 1rem;
}

.service-card ul {
    padding-left: 1.5rem;
}

.service-card li {
    margin-bottom: 0.5rem;
}

.vision-card {
    margin-bottom: 2rem;
}

.vision-card h2 {
    color: #e74c3c;
    margin-bottom: 1rem;
}

.vision-card p {
    line-height: 1.6;
    text-align: justify;
}

.home-footer {
    text-align: center;
    padding: 1.5rem;
    background-color: #2c3e50;
    color: white;
}

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
}
</style>