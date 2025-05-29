<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { ChatDotRound, Close, ArrowUp } from '@element-plus/icons-vue'
import emitter from '../../utils/eventBus'
import MarkdownIt from 'markdown-it'

// 创建Markdown渲染器
const md = new MarkdownIt()
// 修改消息类型定义
interface ChatMessage {
  id: number
  content: string
  role: 'user' | 'assistant'
  timestamp: Date
  loading?: boolean  // 新增加载状态属性
}

// 模拟的对话数据
const mockMessages: ChatMessage[] = [
  {
    id: 1,
    content: "欢迎来到番茄读书商城，有什么我可以帮您的吗？",
    role: "assistant",
    timestamp: new Date()
  }
]
// 添加loading状态变量
const isLoading = ref(false)
// 聊天窗口的显示状态
const chatVisible = ref(false)
const messages = ref<ChatMessage[]>(mockMessages)
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

const isLoggedIn = ref(false)
const username = ref('')

// 关键修改：添加全局定时器检查登录状态
let loginCheckInterval: number | null = null

// 检查登录状态
const checkLoginStatus = () => {
  const prevLoginState = isLoggedIn.value

  // 同时检查localStorage和sessionStorage
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const storedUsername = localStorage.getItem('username') || sessionStorage.getItem('username')

  // 更新登录状态和用户名
  isLoggedIn.value = !!token
  username.value = storedUsername || ''

  // 登录状态变化时进行额外处理
  if (prevLoginState !== isLoggedIn.value) {
    console.log('登录状态发生变化:', isLoggedIn.value ? '已登录' : '未登录')

    if (!isLoggedIn.value) {
      // 登出时自动关闭聊天窗口
      chatVisible.value = false
      messages.value = [...mockMessages]
    }
  }
}

// 滚动到最新消息
const scrollToBottom = () => {
  if (messagesContainer.value) {
    setTimeout(() => {
      messagesContainer.value!.scrollTop = messagesContainer.value!.scrollHeight
    }, 50)
  }
}
// 修改发送消息函数
const sendMessage = async () => {
  if (!inputMessage.value.trim()) return

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    content: inputMessage.value,
    role: 'user',
    timestamp: new Date()
  })

  const userQuestion = inputMessage.value
  inputMessage.value = ''
  scrollToBottom()

  // 添加一个带有加载状态的临时消息
  const loadingMessageId = Date.now() + 1
  messages.value.push({
    id: loadingMessageId,
    content: "思考中...",
    role: 'assistant',
    timestamp: new Date(),
    loading: true
  })

  isLoading.value = true

  try {
    // 获取token (从localStorage或sessionStorage)
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    if (!token) {
      throw new Error('用户未登录')
    }

    // 调用后端API
    const response = await fetch('http://localhost:8080/api/recommend', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      body: JSON.stringify({ query: userQuestion })
    })

    const responseData = await response.json()

    // 移除加载消息
    messages.value = messages.value.filter(msg => msg.id !== loadingMessageId)

    // 检查响应状态
    if (responseData.code === '200') {
      // 添加AI回复
      messages.value.push({
        id: Date.now(),
        content: responseData.data,
        role: 'assistant',
        timestamp: new Date()
      })
    } else {
      throw new Error(responseData.message || '请求失败')
    }
  } catch (error) {
    console.error('AI回复请求失败:', error)

    // 移除加载消息
    messages.value = messages.value.filter(msg => msg.id !== loadingMessageId)

    // 添加错误消息
    messages.value.push({
      id: Date.now(),
      content: "很抱歉，我暂时无法回应您的问题。请确保您已登录，或稍后再试。",
      role: 'assistant',
      timestamp: new Date()
    })
  } finally {
    isLoading.value = false
  }

  scrollToBottom()
}
// 处理登录/登出事件
const handleLoginEvent = (type: 'login' | 'logout') => {
  console.log(`收到${type === 'login' ? '登录' : '登出'}事件`)

  // 立即检查登录状态
  checkLoginStatus()

  // 如果是登出事件，关闭聊天窗口
  if (type === 'logout' || !isLoggedIn.value) {
    chatVisible.value = false
  }
}


// 切换聊天窗口显示状态
const toggleChat = () => {
  chatVisible.value = !chatVisible.value
  if (chatVisible.value) {
    scrollToBottom()
  }
}

onMounted(() => {
  console.log('ChatBox组件已挂载')

  // 初始化时检查登录状态
  checkLoginStatus()

  // 设置定时器，每500ms检查一次登录状态（确保实时响应）
  loginCheckInterval = setInterval(checkLoginStatus, 500)

  // 仍然保留事件监听
  emitter.on('login', () => {
    console.log('收到login事件，立即检查登录状态')
    checkLoginStatus()
  })

  emitter.on('logout', () => {
    console.log('收到logout事件，立即检查登录状态')
    handleLoginEvent('logout')
  })

  scrollToBottom()
})

// 清理定时器和事件监听
onUnmounted(() => {
  console.log('清理定时器和事件监听')
  if (loginCheckInterval) {
    clearInterval(loginCheckInterval)
  }
  emitter.off('login')
  emitter.off('logout')
})

// 移除多余的watch（保留一个即可）
watch([
  () => localStorage.getItem('token'),
  () => sessionStorage.getItem('token')
], () => {
  console.log('存储状态变化，立即检查登录状态')
  checkLoginStatus()
}, { immediate: true })
</script>

<template>
  <div class="chat-container" v-if="isLoggedIn">
    <!-- 悬浮按钮 -->
    <div class="chat-button" @click="toggleChat" :class="{ 'active': chatVisible }">
      <el-icon :size="24" class="icon">
        <component :is="chatVisible ? Close : ChatDotRound" />
      </el-icon>
    </div>

    <!-- 对话窗口 -->
    <div class="chat-window" :class="{ 'visible': chatVisible }">
      <div class="chat-header">
        <h3>番茄书城AI助手</h3>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <!-- 在template部分替换消息渲染的代码 -->
        <div v-for="message in messages" :key="message.id" :class="['message', message.role]">
          <div v-if="message.loading" class="message-loading">
            <el-icon class="loading-icon">
              <Loading />
            </el-icon>
            <span>思考中...</span>
          </div>
          <template v-else>
            <div v-if="message.role === 'assistant'" class="message-content" v-html="md.render(message.content)"></div>
            <div v-else class="message-content">{{ message.content }}</div>
          </template>
          <div class="message-time">
            {{ message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input v-model="inputMessage" placeholder="请输入您的问题..." :rows="2" type="textarea" resize="none"
          @keyup.enter.prevent="sendMessage" />
        <!-- 替换发送按钮代码 -->
        <el-button type="primary" class="send-button" @click="sendMessage" :disabled="!inputMessage.trim()"
          :loading="isLoading">
          <el-icon>
            <ArrowUp />
          </el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 1000;
}

.chat-button {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: #e74c3c;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s ease;
}

.chat-button:hover {
  transform: scale(1.05);
}

.chat-button.active {
  background-color: #c0392b;
}

.chat-button .icon {
  color: white;
}

.chat-window {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 350px;
  height: 500px;
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
  transform: translateY(20px);
  transition: all 0.3s ease;
}

.chat-window.visible {
  opacity: 1;
  pointer-events: all;
  transform: translateY(0);
}

.chat-header {
  background-color: #e74c3c;
  color: white;
  padding: 12px 16px;
  text-align: center;
}

.chat-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message {
  max-width: 80%;
  padding: 10px 12px;
  border-radius: 12px;
  position: relative;
}

.message.user {
  align-self: flex-end;
  background-color: #e74c3c;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant {
  align-self: flex-start;
  background-color: #f2f2f2;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-content {
  font-size: 14px;
  line-height: 1.4;
}

.message-time {
  font-size: 10px;
  opacity: 0.7;
  margin-top: 4px;
  text-align: right;
}

.chat-input {
  padding: 12px;
  display: flex;
  gap: 8px;
  border-top: 1px solid #eee;
}

.chat-input :deep(.el-textarea__inner) {
  resize: none;
}

.send-button {
  background-color: #e74c3c;
  border-color: #e74c3c;
  padding: 8px;
  margin-top: 3px;
  height: 40px;
  width: 40px;
}

.send-button:hover {
  background-color: #c0392b;
  border-color: #c0392b;
}

/* 加载动画 */
.message-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

/* Markdown 渲染样式 */
.message-content :deep(p) {
  margin: 0.5em 0;
}

.message-content :deep(code) {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
  font-family: monospace;
}

.message-content :deep(pre) {
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 5px;
  padding: 0.8em;
  overflow-x: auto;
}

.message-content :deep(a) {
  color: #e74c3c;
  text-decoration: none;
}

.message-content :deep(a:hover) {
  text-decoration: underline;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.message-content :deep(blockquote) {
  border-left: 4px solid #e74c3c;
  margin: 0.5em 0;
  padding-left: 1em;
  color: #666;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-window {
    width: 90vw;
    right: -10px;
  }
}
</style>