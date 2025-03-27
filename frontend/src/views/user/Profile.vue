<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, updateUserInfo } from "../../api/user.ts"
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Message, Phone, Location, Edit } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(true)
const isEditing = ref(false)

// 用户信息
const userInfo = reactive({
  username: '',
  name: '',
  role: '',
  avatar: '',
  telephone: '',
  email: '',
  location: ''
})

// 编辑表单
const editForm = reactive({
  username: '',
  name: '',
  telephone: '',
  email: '',
  location: '',
  password: '',
  confirmPassword: ''
})

// 获取用户信息
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const username = sessionStorage.getItem('username')
    if (!username) {
      ElMessage.error('用户未登录')
      router.push('/login')
      return
    }
    
    const response = await getUserInfo(username)
    if (response.data.code === '200') {
      const userData = response.data.data
      userInfo.username = userData.username
      userInfo.name = userData.name
      userInfo.role = userData.role
      userInfo.avatar = userData.avatar
      userInfo.telephone = userData.telephone
      userInfo.email = userData.email
      userInfo.location = userData.location
      
      // 初始化编辑表单
      editForm.username = userData.username
      editForm.name = userData.name
      editForm.telephone = userData.telephone
      editForm.email = userData.email
      editForm.location = userData.location
    } else {
      ElMessage.error(response.data.msg || '获取用户信息失败')
      if (response.data.code === '401') {
        router.push('/login')
      }
    }
  } catch (error) {
    console.error('获取用户信息错误:', error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 开始编辑
const startEdit = () => {
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  editForm.name = userInfo.name
  editForm.telephone = userInfo.telephone
  editForm.email = userInfo.email
  editForm.location = userInfo.location
  editForm.password = ''
  editForm.confirmPassword = ''
}

// 保存个人信息
const saveProfile = async () => {
  // 验证手机号
  if (editForm.telephone) {
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(editForm.telephone)) {
      ElMessage.error('请输入正确的手机号码')
      return
    }
  }

  // 验证邮箱
  if (editForm.email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(editForm.email)) {
      ElMessage.error('请输入正确的邮箱地址')
      return
    }
  }

  // 验证密码
  if (editForm.password) {
    if (editForm.password.length < 6) {
      ElMessage.error('密码长度至少为6位')
      return
    }
    if (editForm.password !== editForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
  }

  try {
    // 构造更新数据
    const updateData: {
      username: string;
      name: string;
      telephone: string;
      email: string;
      location: string;
      password?: string;
    } = {
      username: editForm.username,
      name: editForm.name,
      telephone: editForm.telephone,
      email: editForm.email,
      location: editForm.location
    }

    // 如果有输入密码，则添加
    if (editForm.password) {
      updateData.password = editForm.password
    }

    const response = await updateUserInfo(updateData)
    
    if (response.data.code === '200') {
      ElMessage.success('更新成功')
      userInfo.name = editForm.name
      userInfo.telephone = editForm.telephone
      userInfo.email = editForm.email
      userInfo.location = editForm.location
      isEditing.value = false
      
      // 清空密码字段
      editForm.password = ''
      editForm.confirmPassword = ''
    } else {
      ElMessage.error(response.data.msg || '更新失败')
    }
  } catch (error) {
    console.error('更新用户信息错误:', error)
    ElMessage.error('网络错误，请稍后重试')
  }
}

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '确定要退出登录吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('username')
    router.push('/login')
    ElMessage.success('已成功退出登录')
  }).catch(() => {})
}

// 组件挂载时获取用户信息
onMounted(fetchUserInfo)
</script>

<template>
  <el-main class="profile-container">
    <el-card class="profile-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <h2>个人资料</h2>
          <div class="header-actions">
            <template v-if="!isEditing">
              <el-button type="primary" @click="startEdit" :icon="Edit">编辑资料</el-button>
            </template>
            <template v-else>
              <el-button @click="cancelEdit">取消</el-button>
              <el-button type="primary" @click="saveProfile">保存</el-button>
            </template>
          </div>
        </div>
      </template>
      
      <div v-if="!loading" class="profile-content">
        <!-- 查看模式 -->
        <template v-if="!isEditing">
          <div class="avatar-section">
            <el-avatar 
              :size="100" 
              :src="userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
            />
          </div>
          
          <div class="info-section">
            <div class="info-item">
              <span class="item-label"><el-icon><User /></el-icon> 用户名</span>
              <span class="item-value">{{ userInfo.username }}</span>
            </div>
            
            <div class="info-item">
              <span class="item-label"><el-icon><User /></el-icon> 姓名</span>
              <span class="item-value">{{ userInfo.name }}</span>
            </div>
            
            <div class="info-item">
              <span class="item-label"><el-icon><User /></el-icon> 身份</span>
              <span class="item-value">
                <el-tag v-if="userInfo.role === 'admin'" type="danger">管理员</el-tag>
                <el-tag v-else-if="userInfo.role === 'user'" type="success">普通用户</el-tag>
                <span v-else>{{ userInfo.role }}</span>
              </span>
            </div>
            
            <div class="info-item" v-if="userInfo.telephone">
              <span class="item-label"><el-icon><Phone /></el-icon> 手机号</span>
              <span class="item-value">{{ userInfo.telephone }}</span>
            </div>
            
            <div class="info-item" v-if="userInfo.email">
              <span class="item-label"><el-icon><Message /></el-icon> 邮箱</span>
              <span class="item-value">{{ userInfo.email }}</span>
            </div>
            
            <div class="info-item" v-if="userInfo.location">
              <span class="item-label"><el-icon><Location /></el-icon> 位置</span>
              <span class="item-value">{{ userInfo.location }}</span>
            </div>
          </div>
          
          <div class="action-section">
            <el-button type="danger" @click="handleLogout">退出登录</el-button>
          </div>
        </template>
        
        <!-- 编辑模式 -->
        <template v-else>
          <el-form :model="editForm" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="editForm.username" disabled />
            </el-form-item>
            
            <el-form-item label="姓名">
              <el-input v-model="editForm.name" placeholder="请输入姓名" />
            </el-form-item>
            
            <el-form-item label="手机号">
              <el-input v-model="editForm.telephone" placeholder="请输入手机号" />
            </el-form-item>
            
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            
            <el-form-item label="位置">
              <el-input v-model="editForm.location" placeholder="请输入位置" />
            </el-form-item>
            
            <el-form-item label="修改密码 (选填)">
              <el-input v-model="editForm.password" type="password" show-password placeholder="不修改请留空" />
            </el-form-item>
            
            <el-form-item label="确认密码">
              <el-input v-model="editForm.confirmPassword" type="password" show-password placeholder="不修改请留空" />
            </el-form-item>
          </el-form>
        </template>
      </div>
    </el-card>
  </el-main>
</template>

<style scoped>
.profile-container {
  padding: 2rem;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.profile-card {
  width: 100%;
  max-width: 800px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.profile-content {
  padding: 1rem 0;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 2rem;
}

.info-section {
  margin-bottom: 2rem;
}

.info-item {
  display: flex;
  padding: 1rem 0;
  border-bottom: 1px solid #ebeef5;
}

.item-label {
  flex: 0 0 120px;
  display: flex;
  align-items: center;
  color: #606266;
  font-weight: 500;
}

.item-label .el-icon {
  margin-right: 8px;
}

.item-value {
  flex: 1;
}

.action-section {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
  }
  
  .item-label {
    margin-bottom: 0.5rem;
  }
}
</style>