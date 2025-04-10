<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin } from "../../api/user.ts"
import { ElMessage } from 'element-plus'
import { User, Lock, PictureRounded } from '@element-plus/icons-vue'
import { captchaGenerator } from '../../utils/captcha'

// 导入共享样式
import '@/assets/css/auth.css'

const router = useRouter()

// 登录表单数据
const username = ref('') // 用户名，必填
const password = ref('') // 密码，必填
const captcha = ref('')
const captchaImage = ref('')
const captchaCode = ref('')

// 前端表单校验
const hasUsernameInput = computed(() => username.value.trim() !== '')
const hasPasswordInput = computed(() => password.value !== '')
const hasCaptchaInput = computed(() => captcha.value !== '')

const loginDisabled = computed(() => {
  return !(hasUsernameInput.value && hasPasswordInput.value && hasCaptchaInput.value)
})

// 从前端获取验证码
const getCaptcha = async () => {
  const { image, code } = captchaGenerator.generate()
  captchaImage.value = image
  captchaCode.value = code
}

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
    if (res.data.code == 200) {
      // 保存用户信息和token
      sessionStorage.setItem('token', res.data.data) // 后端返回的是直接的token字符串
      sessionStorage.setItem('username', username.value) // 保存用户名
      
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

// 页面加载时自动获取验证码
onMounted(() => {
  getCaptcha()
})
</script>

<template>
  <el-main class="auth-container">
    <div class="auth-content">
      <!-- 左侧品牌区域 -->
      <div class="auth-branding">
        <h1 class="brand-title">番茄读书</h1>
        <p class="brand-subtitle">您的线上实体书本<br/>购买平台</p>
      </div>
      
      <!-- 右侧登录表单 -->
      <div class="auth-form-wrapper">
        <div class="auth-form">
          <div class="auth-header">
            <div class="tomato-icon"></div>
            <h1 class="auth-title">欢迎登录</h1>
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
/* 引入共享样式文件，特定组件样式可以在这里覆盖 */
.el-form {
  margin-top: 10px;
}

.el-input {
  margin-bottom: 15px;
}

/* 确保输入框高度一致 */
:deep(.el-input__wrapper) {
  height: 2.5rem;
  width: 100%;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #626aef inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #626aef inset;
}
</style>