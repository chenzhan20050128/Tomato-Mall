<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin } from "../../api/user.ts"
import { ElMessage } from 'element-plus'
import { User, Lock, PictureRounded } from '@element-plus/icons-vue'
import { captchaGenerator } from '../../utils/captcha'

const router = useRouter()

// 登录表单数据（修改为 username）
const username = ref('') // 用户名，必填
const password = ref('') // 密码，必填
const captcha = ref('')
const captchaImage = ref('')
const captchaCode = ref('')

// 前端表单校验
const hasUsernameInput = computed(() => username.value.trim() !== '')
const hasPasswordInput = computed(() => password.value !== '')
const hasCaptchaInput = computed(() => captcha.value !== '')

// 不再强制要求是手机号格式，因为可以是用户名
const loginDisabled = computed(() => {
  return !(hasUsernameInput.value && hasPasswordInput.value && hasCaptchaInput.value)
})

// 从前端获取验证码
const getCaptcha = async () => {
  const { image, code } = captchaGenerator.generate()
  captchaImage.value = image
  captchaCode.value = code
}
getCaptcha()

// 登录处理
const handleLogin = async () => {
  if (!captchaGenerator.validate(captcha.value)) {
    ElMessage({
      message: "验证码错误",
      type: 'error',
      center: true,
    })
    getCaptcha()
    captcha.value = ''
    return
  }

  userLogin({
    username: username.value,
    password: password.value
  }).then(res => {
    if (res.data.code === '200') {
      // 保存用户信息和token
      sessionStorage.setItem('token', res.data.data) // 后端返回的是直接的token字符串
      sessionStorage.setItem('username', username.value) // 保存用户名

      // 可根据需要保存其他信息
      if (res.data.role) {
        sessionStorage.setItem('role', res.data.role)
      }
      
      ElMessage({
        message: "登录成功",
        type: 'success',
        center: true,
      })
      
      // 所有用户统一跳转到个人资料页面
      router.push('/profile')
    } else {
      ElMessage({
        message: res.data.msg || "登录失败，请检查账号密码",
        type: 'error',
        center: true,
      })
    }
  }).catch(err => {
    console.error('登录请求出错:', err)
    ElMessage({
      message: "网络错误，请稍后重试",
      type: 'error',
      center: true,
    })
  })
}
</script>
<template>
  <el-main class="auth-container">
    <div class="auth-content">
      <!-- 左侧标题区域 -->
      <div class="auth-branding">
        <h1 class="brand-title">番茄读书</h1>
        <p class="brand-subtitle">您的线上实体书本<br/>购买平台</p>
      </div>
      
      <!-- 右侧登录表单 - 移除多余的卡片嵌套 -->
      <div class="auth-form-wrapper">
        <div class="auth-form">
          <div class="auth-header">
            <h1 class="auth-title">登录</h1>
          </div>

          <el-form @keydown.enter="!loginDisabled && handleLogin()">
            <el-form-item>
              <label class="custom-label">
                <el-icon><User /></el-icon>
                <span>用户名</span>
              </label>
              <el-input v-model="username" placeholder="请输入用户名" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label">
                <el-icon><Lock /></el-icon>
                <span>密码</span>
              </label>
              <el-input type="password" v-model="password" show-password placeholder="请输入密码" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label">
                <el-icon><PictureRounded /></el-icon>
                <span>验证码</span>
              </label>
              <div class="auth-verify-group">
                <el-input v-model="captcha" placeholder="请输入验证码" />
                <div class="captcha-image" @click="getCaptcha">
                  <img :src="captchaImage" alt="验证码" title="点击刷新" />
                </div>
              </div>
            </el-form-item>

            <div class="auth-button-group">
              <el-button type="primary" @click.prevent="handleLogin" :disabled="loginDisabled">
                登录
              </el-button>

              <router-link to="/register" custom v-slot="{ navigate }">
                <el-button @click="navigate">
                  注册
                </el-button>
              </router-link>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </el-main>
</template>

<style scoped>
/* 整体背景和容器设置 */
.auth-container {
  min-height: 100vh;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f7fa 0%, #cfb0d4 100%);
  position: relative;
  overflow: hidden;
}

/* 添加背景图案效果 */
.auth-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('../../assets/background-pattern.svg');
  background-size: cover;
  opacity: 0.05;
  z-index: 0;
}

/* 内容区域布局 */
.auth-content {
  display: flex;
  width: 90%;
  max-width: 1200px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  min-height: 600px;
  max-height: 90vh;
}

/* 左侧品牌区域 */
.auth-branding {
  flex: 1;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  color: white;
  padding: 3rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
}

.brand-title {
  font-size: 3.5rem;
  font-weight: bold;
  margin-bottom: 1.5rem;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 1.8rem;
  font-weight: 300;
  line-height: 1.5;
}

/* 右侧表单区域 - 优化滚动和布局 */
.auth-form-wrapper {
  flex: 1;
  padding: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
}

.auth-form {
  width: 100%;
  max-width: 420px;
}

.auth-header {
  text-align: center;
  margin-bottom: 2rem;
}

.auth-title {
  color: #303030;
  font-size: 2rem;
  font-weight: 600;
}

/* 验证码区域样式 */
.auth-verify-group {
  display: flex;
  gap: 10px;
}

.captcha-image {
  width: 120px;
  height: 2.5rem;
  cursor: pointer;
  border: 1px solid var(--el-border-color-base);
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-image img {
  max-width: 100%;
  max-height: 100%;
}

/* 按钮组样式 */
.auth-button-group {
  display: flex;
  justify-content: space-between;
  margin-top: 2rem;
}

.auth-button-group .el-button {
  width: 48%;
}

/* 自定义标签样式 */
.custom-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #606266;
}

.custom-label.error {
  color: var(--el-color-danger);
}

.error-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset;
}

:deep(.el-input__wrapper) {
  height: 2.5rem;
  width: 100%;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .auth-content {
    flex-direction: column;
    width: 95%;
    height: auto;
  }
  
  .auth-branding {
    padding: 2rem;
    align-items: center;
    text-align: center;
  }
  
  .brand-title {
    font-size: 2.5rem;
  }
  
  .brand-subtitle {
    font-size: 1.4rem;
  }
}

@media (max-width: 576px) {
  .auth-content {
    width: 100%;
    height: auto;
    min-height: 100vh;
    border-radius: 0;
  }
  
  .auth-form-wrapper {
    padding: 1.5rem;
  }
  
  .auth-branding {
    padding: 1.5rem;
  }
  
  .brand-title {
    font-size: 2rem;
  }
  
  .brand-subtitle {
    font-size: 1.2rem;
  }
}
</style>