<script setup lang="ts">
import Header from './Header.vue'
import { useViewport } from '../../composables/useViewport'
import { provide } from 'vue'
import ChatBot from './ChatBox.vue' 

const viewport = useViewport()
provide('viewport', viewport)
</script>

<template>
  <el-container direction="vertical" class="layout-container">
    <Header />
    <router-view />
    <ChatBot /> <!-- 添加聊天组件 -->
  </el-container>
</template>

<style>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>