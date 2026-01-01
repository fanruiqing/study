<template>
  <div class="app-container" :class="{ 'dark-theme': isDark }">
    <el-container class="main-container">
      <!-- Sidebar -->
      <el-aside width="60px" class="sidebar">
        <div class="sidebar-content">
          <div class="logo">
            <el-icon :size="28"><ChatDotRound /></el-icon>
          </div>
          <nav class="nav-menu">
            <el-tooltip content="对话" placement="right">
              <div 
                class="nav-item" 
                :class="{ active: currentRoute === 'chat' }"
                @click="router.push('/')"
              >
                <el-icon :size="22"><ChatLineRound /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="知识库" placement="right">
              <div 
                class="nav-item"
                :class="{ active: currentRoute === 'knowledge-base' }"
                @click="router.push('/knowledge-base')"
              >
                <el-icon :size="22"><FolderOpened /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="工作流编排" placement="right">
              <div 
                class="nav-item"
                :class="{ active: currentRoute === 'workflow-builder' }"
                @click="router.push('/workflow-builder')"
              >
                <el-icon :size="22"><Connection /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="提示词" placement="right">
              <div 
                class="nav-item"
                :class="{ active: currentRoute === 'prompts' }"
                @click="router.push('/prompts')"
              >
                <el-icon :size="22"><Document /></el-icon>
              </div>
            </el-tooltip>
            <el-tooltip content="设置" placement="right">
              <div 
                class="nav-item"
                :class="{ active: currentRoute === 'settings' }"
                @click="router.push('/settings')"
              >
                <el-icon :size="22"><Setting /></el-icon>
              </div>
            </el-tooltip>
          </nav>
          <div class="sidebar-bottom">
            <el-tooltip :content="isDark ? '切换亮色' : '切换暗色'" placement="right">
              <div class="nav-item" @click="toggleTheme">
                <el-icon :size="22">
                  <Moon v-if="!isDark" />
                  <Sunny v-else />
                </el-icon>
              </div>
            </el-tooltip>
          </div>
        </div>
      </el-aside>
      
      <!-- Main Content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ChatDotRound, 
  ChatLineRound, 
  Document, 
  Setting,
  Moon,
  Sunny,
  FolderOpened,
  Connection
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const isDark = ref(false)

const currentRoute = computed(() => {
  const path = route.path
  if (path === '/' || path.startsWith('/chat')) return 'chat'
  if (path.startsWith('/knowledge-base')) return 'knowledge-base'
  if (path.startsWith('/workflow-builder')) return 'workflow-builder'
  if (path.startsWith('/prompts')) return 'prompts'
  if (path.startsWith('/settings')) return 'settings'
  return ''
})

const toggleTheme = () => {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  isDark.value = savedTheme === 'dark'
})
</script>

<style lang="scss">
.app-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  
  &.dark-theme {
    --bg-color: #1a1a2e;
    --sidebar-bg: #16213e;
    --text-color: #eee;
    --border-color: #2a2a4a;
    --hover-bg: #2a2a4a;
    --message-user-bg: #3a3a5a;
    --message-assistant-bg: #2a2a4a;
  }
}

.main-container {
  height: 100%;
}

.sidebar {
  background: var(--sidebar-bg, #f5f5f5);
  border-right: 1px solid var(--border-color, #e0e0e0);
  
  .sidebar-content {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 12px 0;
  }
  
  .logo {
    display: flex;
    justify-content: center;
    padding: 12px 0;
    color: var(--el-color-primary);
  }
  
  .nav-menu {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding-top: 20px;
  }
  
  .nav-item {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-color, #666);
    transition: all 0.2s;
    
    &:hover {
      background: var(--hover-bg, #e0e0e0);
    }
    
    &.active {
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }
  }
  
  .sidebar-bottom {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-bottom: 12px;
  }
}

.main-content {
  padding: 0;
  background: var(--bg-color, #fff);
}
</style>
