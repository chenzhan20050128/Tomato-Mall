<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from "../../api/user.ts"
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Phone, UserFilled, PictureRounded, Location } from '@element-plus/icons-vue'
import { captchaGenerator } from '../../utils/captcha'

const router = useRouter()

// 注册表项
const username = ref('') // 用户名，必填
const password = ref('') // 密码，必填
const confirmPassword = ref('')
const name = ref('') // 真实姓名，必填
const telephone = ref('') // 手机号，非必填
const email = ref('') // 邮箱，非必填
const location = ref('') // 位置，非必填
const role = ref('') // 角色，必填 (admin/user)
const captcha = ref('')
const captchaImage = ref('')
const captchaCode = ref('')

// 前端表单校验
const hasUsernameInput = computed(() => username.value.trim() !== '')
const hasPasswordInput = computed(() => password.value !== '')
const hasConfirmPasswordInput = computed(() => confirmPassword.value !== '')
const hasNameInput = computed(() => name.value.trim() !== '')
const hasRoleSelected = computed(() => role.value !== '')
const hasCaptchaInput = computed(() => captcha.value !== '')

// 手机号验证（可选字段，但如果有输入则必须正确）
const phoneRegex = /^1\d{10}$/
const isPhoneValid = computed(() => telephone.value === '' || phoneRegex.test(telephone.value))

// 邮箱验证（可选字段，但如果有输入则必须正确）
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const isEmailValid = computed(() => email.value === '' || emailRegex.test(email.value))

// 密码验证
const isPasswordMatching = computed(() => password.value === confirmPassword.value)
const isPasswordValid = computed(() => password.value.length >= 6)

// 注册按钮是否可用
const registerDisabled = computed(() => {
  return !(hasUsernameInput.value && hasPasswordInput.value &&
    hasConfirmPasswordInput.value && hasNameInput.value &&
    hasRoleSelected.value && isPhoneValid.value && isEmailValid.value &&
    isPasswordMatching.value && isPasswordValid.value && hasCaptchaInput.value)
})

// 从前端获取验证码
const getCaptcha = async () => {
  const { image, code } = captchaGenerator.generate()
  captchaImage.value = image
  captchaCode.value = code
}
getCaptcha()

// 注册处理
const handleRegister = async () => {
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

  // 构建符合后端要求的注册对象
  const registerData: {
    username: string;
    password: string;
    name: string;
    role: string;
    email?: string;
    telephone?: string;
    location?: string;
  } = {
    username: username.value,
    password: password.value,
    name: name.value,
    role: mapRoleValue(role.value) // 转换角色值
  }

  // 添加可选字段
  if (telephone.value) registerData.telephone = telephone.value
  if (email.value) registerData.email = email.value
  if (location.value) registerData.location = location.value

  userRegister(registerData).then(res => {
    if (res.data.code === '200') {
      ElMessage({
        message: "注册成功！请登录账号",
        type: 'success',
        center: true,
      })
      router.push('/login')
    } else {
      ElMessage({
        message: res.data.msg || "注册失败",
        type: 'error',
        center: true,
      })
    }
  }).catch(err => {
    console.error('注册请求出错:', err)
    ElMessage({
      message: "网络错误，请稍后重试",
      type: 'error',
      center: true,
    })
  })
}

// 角色值映射函数 (前端下拉框值 -> 后端需要的值)
function mapRoleValue(roleValue) {
  switch (roleValue) {
    case '1': return 'admin'
    case '2': return 'user'
    case '3': return 'merchant'
    default: return 'user'
  }
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
      
      <!-- 右侧注册表单 - 移除多余的卡片嵌套，确保可滚动 -->
      <div class="auth-form-wrapper">
        <div class="auth-form">
          <div class="auth-header">
            <h1 class="auth-title">注册</h1>
          </div>

          <el-form @keydown.enter="!registerDisabled && handleRegister()">
            <el-form-item>
              <label class="custom-label">
                <el-icon><User /></el-icon>
                <span>用户名</span>
              </label>
              <el-input v-model="username" placeholder="请输入用户名" />
            </el-form-item>
            
            <el-form-item>
              <label class="custom-label">
                <el-icon><User /></el-icon>
                <span>姓名</span>
              </label>
              <el-input v-model="name" placeholder="请输入真实姓名" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label">
                <el-icon><UserFilled /></el-icon>
                <span>身份</span>
              </label>
              <el-select v-model="role" placeholder="请选择身份">
                <el-option label="管理员" value="1" />
                <el-option label="顾客" value="2" />
                <el-option label="商家" value="3" />
              </el-select>
            </el-form-item>

            <el-form-item>
              <label class="custom-label" :class="{ 'error': !isPhoneValid }">
                <el-icon><Phone /></el-icon>
                <span>{{ isPhoneValid ? '手机号 (选填)' : '手机号格式不正确' }}</span>
              </label>
              <el-input v-model="telephone" :class="{ 'error-input': !isPhoneValid }" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label" :class="{ 'error': !isEmailValid }">
                <el-icon><Message /></el-icon>
                <span>{{ isEmailValid ? '邮箱 (选填)' : '邮箱格式不正确' }}</span>
              </label>
              <el-input v-model="email" :class="{ 'error-input': !isEmailValid }" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label">
                <el-icon><Location /></el-icon>
                <span>位置 (选填)</span>
              </label>
              <el-input v-model="location" placeholder="请输入位置" />
            </el-form-item>

            <el-form-item>
              <label class="custom-label" :class="{ 'error': hasPasswordInput && !isPasswordValid }">
                <el-icon><Lock /></el-icon>
                <span>{{ !hasPasswordInput || isPasswordValid ? '密码' : '密码长度至少为6位' }}</span>
              </label>
              <el-input type="password" v-model="password" :class="{ 'error-input': hasPasswordInput && !isPasswordValid }"
                placeholder="请输入密码" show-password />
            </el-form-item>

            <el-form-item>
              <label class="custom-label" :class="{ 'error': hasConfirmPasswordInput && !isPasswordMatching }">
                <el-icon><Lock /></el-icon>
                <span>{{ !hasConfirmPasswordInput || isPasswordMatching ? '确认密码' : '密码不匹配' }}</span>
              </label>
              <el-input type="password" v-model="confirmPassword"
                :class="{ 'error-input': hasConfirmPasswordInput && !isPasswordMatching }" placeholder="请再次输入密码"
                show-password />
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
              <el-button type="primary" @click.prevent="handleRegister" :disabled="registerDisabled">
                注册
              </el-button>

              <router-link to="/login" custom v-slot="{ navigate }">
                <el-button @click="navigate">
                  返回登录
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

/* 内容区域布局 - 不设置固定高度，允许更灵活的调整 */
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

/* 右侧表单区域 - 确保可以滚动 */
.auth-form-wrapper {
  flex: 1;
  padding: 2rem;
  display: flex;
  align-items: flex-start; /* 让表单靠上方对齐 */
  justify-content: center;
  overflow-y: auto; /* 允许垂直滚动 */
}

.auth-form {
  width: 100%;
  max-width: 420px;
  padding: 1rem 0; /* 添加些许上下内边距 */
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
  margin-bottom: 1rem;
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

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-select .el-input__wrapper) {
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
  .auth-container {
    align-items: flex-start;
    padding: 1rem 0;
  }
  
  .auth-content {
    width: 100%;
    height: auto;
    max-height: none;
    min-height: auto;
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