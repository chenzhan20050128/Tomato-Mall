<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

// 跳转到首页
const goToHome = () => {
  router.push('/home')
}
</script>

<template>
  <div class="not-found-container">
    <div class="content-box">
      <div class="error-code">404</div>
      <div class="error-icon">
        <el-icon><CircleClose /></el-icon>
      </div>
      <h1 class="error-title">页面未找到</h1>
      <p class="error-message">抱歉，您访问的页面不存在或者丢失</p>
      <el-button type="primary" size="large" @click="goToHome" class="home-button">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
  padding: 20px;
}

.content-box {
  text-align: center;
  background-color: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
  position: relative;
}

.error-code {
  position: absolute;
  font-size: 180px;
  font-weight: 700;
  color: rgba(231, 76, 60, 0.1);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 0;
}

.error-icon {
  font-size: 80px;
  color: #e74c3c;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.error-message {
  font-size: 18px;
  color: #606266;
  margin-bottom: 30px;
  position: relative;
  z-index: 1;
}

.home-button {
  background-color: #e74c3c;
  border-color: #e74c3c;
  font-size: 16px;
  padding: 12px 24px;
  position: relative;
  z-index: 1;
}

.home-button:hover {
  background-color: #c0392b;
  border-color: #c0392b;
}

/* 添加响应式样式 */
@media (max-width: 768px) {
  .content-box {
    padding: 30px;
  }
  
  .error-code {
    font-size: 120px;
  }
  
  .error-icon {
    font-size: 60px;
  }
  
  .error-title {
    font-size: 26px;
  }
  
  .error-message {
    font-size: 16px;
  }
}
</style>