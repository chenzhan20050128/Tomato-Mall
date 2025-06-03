<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, updateUserInfo } from "../../api/user.ts"
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Message, Phone, Location, Edit, Plus } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(true)
const isEditing = ref(false)
const uploadLoading = ref(false) // 头像上传loading状态

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
  confirmPassword: '',
  avatar: '' // 添加头像字段
})

// 头像上传处理
const handleAvatarUpload = async (file: File) => {
  // 检查文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }

  // 检查文件大小 (限制为10MB)
  const isLt2M = file.size / 1024 / 1024 < 10
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }

  uploadLoading.value = true

  try {
    // 创建FormData对象
    const formData = new FormData()
    formData.append('image', file)

    const token = sessionStorage.getItem('token')
    

    // 调用上传接口
    const response = await fetch('http://localhost:8080/upload', {
      method: 'POST',
      headers: {
        'token': `Bearer ${sessionStorage.getItem('token')}`
      },
      body: formData
    })

    const result = await response.json()

    if (result.code === '200') {
      // 上传成功，更新头像
      editForm.avatar = result.data
      ElMessage.success('头像上传成功!')
    } else {
      ElMessage.error(result.msg || '头像上传失败')
    }
  } catch (error) {
    console.error('头像上传错误:', error)
    ElMessage.error('头像上传失败，请稍后重试')
  } finally {
    uploadLoading.value = false
  }

  return false // 阻止element-plus的默认上传行为
}

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
      sessionStorage.setItem('role', userData.role)
      sessionStorage.setItem('userId', userData.userId)
      
      // 初始化编辑表单
      editForm.username = userData.username
      editForm.name = userData.name
      editForm.telephone = userData.telephone
      editForm.email = userData.email
      editForm.location = userData.location
      editForm.avatar = userData.avatar // 初始化头像
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
  editForm.avatar = userInfo.avatar // 恢复原头像
  editForm.password = ''
  editForm.confirmPassword = ''
}

// 保存个人信息
const saveProfile = async () => {
  // 验证手机号
  if (editForm.telephone) {
    const phoneRegex = /^1[0-9]\d{9}$/
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
      avatar: string; // 添加头像字段
      password?: string;
    } = {
      username: editForm.username,
      name: editForm.name,
      telephone: editForm.telephone,
      email: editForm.email,
      location: editForm.location,
      avatar: editForm.avatar // 包含头像更新
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
      userInfo.avatar = editForm.avatar // 更新用户信息中的头像
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
          <div class="header-actions" v-if="!isEditing">
            <el-button type="primary" @click="startEdit" :icon="Edit">编辑资料</el-button>
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
              fit="cover"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
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
                <el-tag v-else-if="userInfo.role === 'merchant'" type="warning">商家</el-tag>
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
        
        <!-- 编辑模式 - 美化后的表单 -->
        <template v-else>
          <div class="edit-form-container">
            <!-- 表单左侧的装饰元素 -->
            <div class="form-decoration">
              <div class="avatar-edit">
                <!-- 头像上传组件 -->
                <el-upload
                  class="avatar-uploader"
                  :show-file-list="false"
                  :before-upload="handleAvatarUpload"
                  accept="image/*"
                >
                  <div class="avatar-upload-container">
                    <el-avatar 
                      :size="100" 
                      :src="editForm.avatar || userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
                      fit="cover"
                    >
                      <el-icon><User /></el-icon>
                    </el-avatar>
                    <div class="avatar-upload-overlay" v-loading="uploadLoading">
                      <el-icon class="upload-icon"><Plus /></el-icon>
                      <span class="upload-text">点击上传</span>
                    </div>
                  </div>
                </el-upload>
              </div>
              <div class="decoration-text">
                <h3>更新个人信息</h3>
                <p>您可以修改以下信息，点击保存完成更新</p>
              </div>
            </div>
            
            <!-- 表单右侧的输入区域 -->
            <el-form :model="editForm" label-position="top" class="edit-form">
              <div class="form-section">
                <h4 class="section-title">基本信息</h4>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><User /></el-icon>
                      <span>用户名</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.username" disabled prefix-icon="User" />
                </el-form-item>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><User /></el-icon>
                      <span>姓名</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.name" placeholder="请输入姓名" prefix-icon="User" />
                </el-form-item>
              </div>
              
              <div class="form-section">
                <h4 class="section-title">联系方式</h4>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><Phone /></el-icon>
                      <span>手机号</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.telephone" placeholder="请输入手机号" prefix-icon="Phone" />
                </el-form-item>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><Message /></el-icon>
                      <span>邮箱</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.email" placeholder="请输入邮箱" prefix-icon="Message" />
                </el-form-item>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><Location /></el-icon>
                      <span>位置</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.location" placeholder="请输入位置" prefix-icon="Location" />
                </el-form-item>
              </div>
              
              <div class="form-section">
                <h4 class="section-title">安全设置</h4>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><Lock /></el-icon>
                      <span>修改密码 (选填)</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.password" type="password" show-password placeholder="不修改请留空" prefix-icon="Lock" />
                </el-form-item>
                
                <el-form-item>
                  <template #label>
                    <div class="form-label">
                      <el-icon><Lock /></el-icon>
                      <span>确认密码</span>
                    </div>
                  </template>
                  <el-input v-model="editForm.confirmPassword" type="password" show-password placeholder="不修改请留空" prefix-icon="Lock" />
                </el-form-item>
              </div>
              
              <!-- 按钮区域 -->
              <div class="form-actions">
                <el-button @click="cancelEdit">取消</el-button>
                <el-button type="primary" @click="saveProfile">保存更改</el-button>
              </div>
            </el-form>
          </div>
        </template>
      </div>
    </el-card>
  </el-main>
</template>

<style scoped>
.profile-container {
  padding: 2rem;
  background-color: #f5f7fa;
  background-image: linear-gradient(135deg, #fff5f5 0%, #f0f7ff 100%);
  min-height: calc(100vh - 60px);
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.profile-card {
  width: 100%;
  max-width: 800px;
  border-radius: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.3s ease;
}

.profile-card:hover {
  box-shadow: 0 12px 25px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  color: #e74c3c;
  font-size: 1.8rem;
  margin: 0;
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
  color: #e74c3c;
}

.item-value {
  flex: 1;
  font-weight: 500;
}

.action-section {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

/* 美化编辑表单样式 */
.edit-form-container {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.form-decoration {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1rem 0;
  border-bottom: 1px dashed #e0e0e0;
  margin-bottom: 1rem;
}

.avatar-edit {
  position: relative;
  margin-bottom: 1rem;
}

/* 头像上传样式 */
.avatar-uploader {
  position: relative;
  cursor: pointer;
}

.avatar-upload-container {
  position: relative;
  display: inline-block;
  border-radius: 50%;
  overflow: hidden;
  transition: all 0.3s ease;
}

.avatar-upload-container:hover {
  transform: scale(1.05);
}

.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 50%;
}

.avatar-upload-container:hover .avatar-upload-overlay {
  opacity: 1;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.upload-text {
  font-size: 12px;
}

.decoration-text h3 {
  color: #e74c3c;
  margin-bottom: 0.5rem;
}

.decoration-text p {
  color: #909399;
  font-size: 0.9rem;
  max-width: 300px;
  margin: 0 auto;
}

.edit-form {
  padding: 0 1rem;
}

.form-section {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.form-section:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.section-title {
  color: #e74c3c;
  font-size: 1.2rem;
  margin-top: 0;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid rgba(231, 76, 60, 0.2);
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-label .el-icon {
  color: #e74c3c;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  padding-left: 0.5rem;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #e74c3c inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #e74c3c inset;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 1rem 0;
}

.form-actions .el-button {
  min-width: 100px;
  transition: all 0.3s ease;
}

.form-actions .el-button:hover {
  transform: translateY(-2px);
}

:deep(.el-button--primary) {
  background-color: #e74c3c;
  border-color: #e74c3c;
}

:deep(.el-button--primary:hover),
:deep(.el-button--primary:focus) {
  background-color: #d44335;
  border-color: #d44335;
}

:deep(.el-tag--danger) {
  background-color: rgba(231, 76, 60, 0.1);
  border-color: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

/* 头像上传相关样式 */
:deep(.el-upload) {
  border: none !important;
  border-radius: 50% !important;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .info-item {
    flex-direction: column;
  }
  
  .item-label {
    margin-bottom: 0.5rem;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions .el-button {
    width: 100%;
    margin-left: 0 !important;
    margin-bottom: 10px;
  }
}
</style>