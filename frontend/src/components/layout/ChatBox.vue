<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ChatDotRound, Close, ArrowUp } from '@element-plus/icons-vue'

// 定义消息类型
interface ChatMessage {
  id: number
  content: string
  role: 'user' | 'assistant'
  timestamp: Date
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

const chatVisible = ref(false)
const messages = ref<ChatMessage[]>(mockMessages)
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// 滚动到最新消息
const scrollToBottom = () => {
  if (messagesContainer.value) {
    setTimeout(() => {
      messagesContainer.value!.scrollTop = messagesContainer.value!.scrollHeight
    }, 50)
  }
}

// 发送消息到后端
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
    
    // 添加错误消息
    messages.value.push({
      id: Date.now(),
      content: "很抱歉，我暂时无法回应您的问题。请确保您已登录，或稍后再试。",
      role: 'assistant',
      timestamp: new Date()
    })
  }
  
  scrollToBottom()
}

// 切换聊天窗口显示状态
const toggleChat = () => {
  chatVisible.value = !chatVisible.value
  if (chatVisible.value) {
    scrollToBottom()
  }
}

onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <div class="chat-container">
    <!-- 悬浮按钮 -->
    <div 
      class="chat-button" 
      @click="toggleChat"
      :class="{ 'active': chatVisible }"
    >
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
        <div 
          v-for="message in messages" 
          :key="message.id" 
          :class="['message', message.role]"
        >
          <div class="message-content">{{ message.content }}</div>
          <div class="message-time">
            {{ message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }}
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          placeholder="请输入您的问题..."
          :rows="2"
          type="textarea"
          resize="none"
          @keyup.enter.prevent="sendMessage"
        />
        <el-button 
          type="primary" 
          class="send-button" 
          @click="sendMessage"
          :disabled="!inputMessage.trim()"
        >
          <el-icon><ArrowUp /></el-icon>
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

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-window {
    width: 90vw;
    right: -10px;
  }
}
</style>