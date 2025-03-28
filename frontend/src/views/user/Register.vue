<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userRegister } from "../../api/user.ts"
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Phone, UserFilled, PictureRounded, Location } from '@element-plus/icons-vue'
import { captchaGenerator } from '../../utils/captcha'

// 导入共享样式
import '@/assets/css/auth.css'

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

// 获取验证码
const getCaptcha = async () => {
  const { image, code } = captchaGenerator.generate()
  captchaImage.value = image
  captchaCode.value = code
}

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
    telephone?: string;
    email?: string;
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
      
      <!-- 右侧注册表单 -->
      <div class="auth-form-wrapper">
        <div class="auth-form">
          <div class="auth-header">
            <div class="tomato-icon"></div>
            <h1 class="auth-title">账号注册</h1>
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
              <el-select v-model="role" placeholder="请选择身份" class="full-width">
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
/* 引入共享样式文件，特定组件样式可以在这里覆盖 */
.el-form {
  margin-top: 10px;
}

.el-input {
  margin-bottom: 15px;
}

/* 确保输入框和下拉框宽度一致 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-select .el-input__wrapper) {
  height: 2.5rem;
  width: 100%;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.2s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #626aef inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus),
:deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #626aef inset;
}

.full-width {
  width: 100%;
}
</style>