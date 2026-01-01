<template>
  <div class="chat-view">
    <!-- Session List Sidebar -->
    <div class="session-sidebar">
      <div class="sidebar-header">
        <el-button 
          v-if="!batchMode" 
          type="primary" 
          @click="createNewSession" 
          :icon="Plus"
          style="width: 100%"
        >
          新对话
        </el-button>
        <div v-if="!batchMode && sessionStore.filteredSessions.length > 0" class="batch-actions">
          <el-button 
            text 
            @click="enterBatchMode"
            :icon="Operation"
            style="width: 100%"
          >
            批量管理
          </el-button>
        </div>
        <div v-if="batchMode" class="batch-mode-header">
          <el-checkbox 
            v-model="selectAll" 
            @change="toggleSelectAll"
            :indeterminate="isIndeterminate"
          >
            全选 ({{ selectedSessions.length }})
          </el-checkbox>
          <div class="batch-mode-actions">
            <el-button 
              type="danger" 
              size="small"
              :disabled="selectedSessions.length === 0"
              @click="deleteSelectedSessions"
              :icon="Delete"
            >
              删除
            </el-button>
            <el-button 
              size="small"
              @click="exitBatchMode"
            >
              取消
            </el-button>
          </div>
        </div>
      </div>
      <div class="search-box">
        <el-input
          v-model="sessionStore.searchQuery"
          placeholder="搜索对话..."
          :prefix-icon="Search"
          clearable
        />
      </div>
      <div class="session-list">
        <div
          v-for="session in sessionStore.filteredSessions"
          :key="session.id"
          class="session-item"
          :class="{ 
            active: session.id === sessionStore.currentSessionId,
            'batch-mode': batchMode,
            selected: selectedSessions.includes(session.id)
          }"
        >
          <el-checkbox 
            v-if="batchMode"
            :model-value="selectedSessions.includes(session.id)"
            @change="toggleSession(session.id)"
            class="session-checkbox"
            @click.stop
          />
          <div class="session-info" @click="batchMode ? toggleSession(session.id) : selectSession(session.id)">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-meta">
              {{ formatTime(session.updatedAt) }}
            </div>
          </div>
          <div v-if="!batchMode" class="session-actions">
            <el-tooltip content="重命名" placement="top">
              <el-icon class="action-icon" @click.stop="handleSessionAction('rename', session.id)">
                <Edit />
              </el-icon>
            </el-tooltip>
            <el-tooltip content="删除" placement="top">
              <el-icon class="action-icon delete-icon" @click.stop="handleSessionAction('delete', session.id)">
                <Delete />
              </el-icon>
            </el-tooltip>
          </div>
        </div>
        <el-empty v-if="sessionStore.filteredSessions.length === 0" description="暂无对话" />
      </div>
    </div>

    <!-- Chat Area -->
    <div class="chat-area">
      <div class="chat-header">
        <div class="chat-title">
          {{ sessionStore.currentSession?.title || '选择或创建对话' }}
        </div>
        <div class="header-controls">
          <el-select
            v-model="selectedKnowledgeBases"
            placeholder="选择知识库（可选）"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :disabled="!sessionStore.currentSessionId"
            @change="handleKnowledgeBaseChange"
            style="width: 250px; margin-right: 16px"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            >
              <div class="kb-option">
                <span>{{ kb.name }}</span>
                <div class="kb-tags">
                  <el-tag v-if="kb.providerName" size="small" type="success">{{ kb.providerName }}</el-tag>
                  <el-tag size="small" type="info">{{ kb.embeddingModel }}</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
          <el-switch
            v-model="smartMode"
            active-text="智能模式"
            inactive-text="手动选择"
            @change="handleSmartModeChange"
            style="margin-right: 16px"
          />
          <el-select
            v-model="selectedModelId"
            placeholder="选择模型"
            filterable
            :disabled="smartMode"
            @change="handleModelChange"
            style="width: 300px"
          >
            <el-option
              value="auto"
              label="🤖 智能选择"
              v-if="smartMode"
            />
            <template v-for="provider in modelStore.activeProviders" :key="provider.id">
              <el-option-group v-if="provider.modelInfos && provider.modelInfos.length > 0" :label="provider.name">
                <template v-for="group in getGroupedModels(provider.modelInfos)" :key="`${provider.id}-${group.name}`">
                  <el-option
                    v-for="model in group.models"
                    :key="`${provider.id}:${model.id}`"
                    :label="model.name"
                    :value="`${provider.id}:${model.id}`"
                  >
                    <div class="model-option">
                      <span class="model-name" :class="{ 'model-failed': model.isFailed }">
                        {{ model.name }}
                        <el-icon v-if="model.isFailed" color="#f56c6c" :size="14" title="该模型调用失败过">
                          <WarningFilled />
                        </el-icon>
                      </span>
                      <div class="model-badges">
                        <el-icon v-if="model.supportTools" :size="14" color="#409eff" title="支持工具">
                          <Tools />
                        </el-icon>
                        <el-icon v-if="model.supportVision" :size="14" color="#67c23a" title="支持视觉">
                          <View />
                        </el-icon>
                        <el-tag v-if="model.type === 'embedding'" size="small" type="warning">嵌入</el-tag>
                      </div>
                    </div>
                  </el-option>
                </template>
              </el-option-group>
            </template>
          </el-select>
        </div>
      </div>

      <div class="message-list" ref="messageListRef">
        <template v-if="sessionStore.currentSessionId">
          <div
            v-for="message in chatStore.messages"
            :key="message.id"
            class="message-item"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'user'"><User /></el-icon>
              <el-icon v-else><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <!-- 显示附件（图片） -->
              <div v-if="message.attachments && message.attachments.length > 0" class="message-attachments">
                <div v-for="(attachment, index) in message.attachments" :key="index" class="attachment-preview">
                  <img v-if="attachment.type === 'image'" :src="attachment.url" :alt="attachment.name" class="attachment-image" />
                  <div v-else class="attachment-file">
                    <el-icon><Document /></el-icon>
                    <span>{{ attachment.name }}</span>
                  </div>
                </div>
              </div>
              
              <!-- 显示思考过程 -->
              <div v-if="message.role === 'assistant' && (message.thinking || (chatStore.isThinking && message.content === streamingContent))" class="thinking-block">
                <div class="thinking-header" @click="toggleThinking(message.id)">
                  <el-icon class="thinking-icon" :class="{ 'is-loading': chatStore.isThinking && message.content === streamingContent }">
                    <Loading v-if="chatStore.isThinking && message.content === streamingContent" />
                    <ChatDotRound v-else />
                  </el-icon>
                  <span>思考过程</span>
                  <el-icon class="expand-icon" :class="{ expanded: expandedThinking[message.id] }"><ArrowDown /></el-icon>
                </div>
                <el-collapse-transition>
                  <div v-show="expandedThinking[message.id] !== false" class="thinking-content">
                    {{ message.thinking || chatStore.thinkingContent }}
                  </div>
                </el-collapse-transition>
              </div>

              <!-- 引用来源 - 简洁版 -->
              <div v-if="message.role === 'assistant' && messageCitations[message.id] && messageCitations[message.id].length > 0" class="simple-citations">
                <div class="citations-header">📄 引用来源 ({{ messageCitations[message.id].length }})</div>
                <div class="citations-list">
                  <span
                    v-for="(citation, index) in messageCitations[message.id]"
                    :key="index"
                    class="citation-tag"
                    @click="showCitationDetail(citation)"
                  >{{ citation.documentName }}</span>
                </div>
              </div>

              <!-- 显示文本内容 -->
              <div v-if="message.content || chatStore.isStreaming" class="message-text" @click="handleCitationClick">
                <div v-if="message.role === 'assistant' && chatStore.isStreaming && message.content === streamingContent" 
                     v-html="renderMarkdown(message.content || '正在思考...', message.id)">
                </div>
                <div v-else v-html="renderMarkdown(message.content, message.id)"></div>
              </div>
              <!-- 用户消息操作 -->
              <div v-if="!chatStore.isStreaming && message.role === 'user'" class="message-actions">
                <el-tooltip content="重新发送">
                  <el-icon @click="resendMessage(message)"><RefreshRight /></el-icon>
                </el-tooltip>
              </div>
              <!-- 助手消息操作 -->
              <div v-if="!chatStore.isStreaming && message.role === 'assistant'" class="message-actions">
                <el-tooltip content="复制">
                  <el-icon @click="copyMessage(message.content)"><CopyDocument /></el-icon>
                </el-tooltip>
                <el-tooltip content="重新生成">
                  <el-icon @click="regenerateMessage(message.id)"><RefreshRight /></el-icon>
                </el-tooltip>
                <el-tooltip content="删除">
                  <el-icon @click="deleteMessage(message.id)"><Delete /></el-icon>
                </el-tooltip>
              </div>

            </div>
          </div>
        </template>
        <el-empty v-else description="选择或创建一个对话开始聊天" />
      </div>

      <div class="input-area">
        <div class="attachments-preview" v-if="attachments.length > 0">
          <div v-for="(file, index) in attachments" :key="index" class="attachment-preview-item">
            <div class="preview-content">
              <img v-if="file.type.startsWith('image/')" :src="getFilePreviewUrl(file)" :alt="file.name" class="preview-image" />
              <div v-else class="preview-file">
                <el-icon><Document /></el-icon>
                <span class="file-name">{{ file.name }}</span>
              </div>
            </div>
            <el-icon class="remove-icon" @click="removeAttachment(index)"><Close /></el-icon>
          </div>
        </div>
        <div class="input-row">
          <div class="input-actions">
            <el-tooltip placement="top">
              <template #content>
                <div style="text-align: left;">
                  <div><strong>上传图片</strong></div>
                  <div>支持格式：JPG、PNG、GIF、BMP、WEBP</div>
                  <div>最大大小：10MB</div>
                </div>
              </template>
              <el-upload
                :show-file-list="false"
                :before-upload="handleImageUpload"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/bmp,image/webp"
                :disabled="!sessionStore.currentSessionId || chatStore.isStreaming"
              >
                <el-icon class="action-icon"><Picture /></el-icon>
              </el-upload>
            </el-tooltip>
            <el-tooltip placement="top">
              <template #content>
                <div style="text-align: left;">
                  <div><strong>上传文件</strong></div>
                  <div>支持格式：TXT、PDF、DOC、DOCX、XLS、XLSX、CSV、JSON、XML、MD</div>
                  <div>最大大小：10MB</div>
                </div>
              </template>
              <el-upload
                :show-file-list="false"
                :before-upload="handleFileUpload"
                accept=".txt,.pdf,.doc,.docx,.xls,.xlsx,.csv,.json,.xml,.md"
                :disabled="!sessionStore.currentSessionId || chatStore.isStreaming"
              >
                <el-icon class="action-icon"><Folder /></el-icon>
              </el-upload>
            </el-tooltip>
          </div>
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="3"
            placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
            :disabled="!sessionStore.currentSessionId || chatStore.isStreaming"
            @keydown="handleKeyDown"
          />
          <div class="button-group">
            <el-button
              v-if="chatStore.isStreaming"
              type="danger"
              :icon="CircleClose"
              @click="stopGeneration"
            >
              停止
            </el-button>
            <el-button
              v-else
              type="primary"
              :icon="Promotion"
              :disabled="!inputContent.trim() || !sessionStore.currentSessionId"
              @click="sendMessage"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useSessionStore } from '@/stores/session'
import { useChatStore } from '@/stores/chat'
import { useModelStore } from '@/stores/model'
import { chatApi, knowledgeBaseApi } from '@/api'
import { marked } from 'marked'
import hljs from 'highlight.js'
import {
  Plus, Search, MoreFilled, User, ChatDotRound,
  CopyDocument, RefreshRight, Delete, Promotion, Tools, View, CircleClose, WarningFilled, Edit,
  Picture, Folder, Document, Close, Operation, Loading, ArrowDown
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElCollapseTransition } from 'element-plus'

const sessionStore = useSessionStore()
const chatStore = useChatStore()
const modelStore = useModelStore()

const inputContent = ref('')
const messageListRef = ref<HTMLElement>()
const selectedModelId = ref<string>('')
const smartMode = ref(false)
const attachments = ref<File[]>([])
const streamingContent = computed(() => chatStore.streamingContent)

// 知识库相关
const knowledgeBases = ref<any[]>([])
const selectedKnowledgeBases = ref<string[]>([])

// 引用来源相关
const messageCitations = ref<Record<string, any[]>>({})

// 设置引用轮播页码
const setCitationPage = (messageId: string, pageIndex: number) => {
  // 保留此函数以防未来需要
}

// 思考过程相关
const expandedThinking = ref<Record<string, boolean>>({})

// 切换思考过程展开/折叠
const toggleThinking = (messageId: string) => {
  if (expandedThinking.value[messageId] === undefined) {
    expandedThinking.value[messageId] = false
  } else {
    expandedThinking.value[messageId] = !expandedThinking.value[messageId]
  }
}

// 批量管理模式
const batchMode = ref(false)
const selectedSessions = ref<string[]>([])
const selectAll = ref(false)

const isIndeterminate = computed(() => {
  const count = selectedSessions.value.length
  return count > 0 && count < sessionStore.filteredSessions.length
})

// 按分组整理模型
const getGroupedModels = (modelInfos: any[]) => {
  const groups: Record<string, any[]> = {}
  modelInfos.forEach(model => {
    const group = model.group || 'other'
    if (!groups[group]) {
      groups[group] = []
    }
    groups[group].push(model)
  })
  
  return Object.entries(groups).map(([name, models]) => ({
    name,
    models
  }))
}

// Configure marked
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (e) {
        return hljs.highlightAuto(code).value
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// 缓存已渲染的 Markdown
const markdownCache = new Map<string, string>()

const renderMarkdown = (content: string, messageId?: string) => {
  if (!content) return ''

  const cacheKey = `${content}_${messageId || ''}_${messageCitations.value[messageId || '']?.length || 0}`
  if (markdownCache.has(cacheKey)) {
    return markdownCache.get(cacheKey)!
  }

  let rendered = marked(content)

  // 总是处理引用标记，无论是否有引用数据
  if (messageId) {
    rendered = processInlineCitations(rendered, messageId)
  }

  if (markdownCache.size > 100) {
    const firstKey = markdownCache.keys().next().value
    markdownCache.delete(firstKey)
  }

  markdownCache.set(cacheKey, rendered)
  return rendered
}

// 处理内联引用标记
const processInlineCitations = (html: string, messageId: string) => {
  const citations = messageCitations.value[messageId]
  const citationRegex = /\[\^(\d+)\]/g

  return html.replace(citationRegex, (match, num) => {
    const index = parseInt(num) - 1

    // 如果有引用数据，显示带弹出框的上标
    if (citations && index >= 0 && index < citations.length) {
      const citation = citations[index]
      const docName = escapeHtml(citation.documentName || '未知文档')
      const content = escapeHtml(citation.content || '')
      const contentPreview =
        content.length > 150 ? content.substring(0, 150) + '...' : content

      return `<sup class="cite-num" data-citation-index="${index}" data-message-id="${messageId}">${num}<span class="cite-popup"><b>${docName}</b><br/>${contentPreview}</span></sup>`
    }

    // 没有引用数据时，显示简单的绿色上标数字
    return `<sup class="cite-num-simple">${num}</sup>`
  })
}

// HTML 转义函数
const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// 处理引用标记点击
const handleCitationClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  const cite = target.closest('.cite-num')

  if (cite) {
    event.preventDefault()
    const index = parseInt(cite.getAttribute('data-citation-index') || '0')
    const messageId = cite.getAttribute('data-message-id') || ''

    if (messageCitations.value[messageId]) {
      const citation = messageCitations.value[messageId][index]
      showCitationDetail(citation)
    }
  }
}

// 显示引用详情
const showCitationDetail = (citation: any) => {
  ElMessageBox.alert(
    `<div style="max-height: 400px; overflow-y: auto;">
      <p><strong>文档：</strong>${citation.documentName}</p>
      <p><strong>相似度：</strong>${(citation.similarity * 100).toFixed(1)}%</p>
      <p><strong>内容：</strong></p>
      <div style="background: #f5f7fa; padding: 12px; border-radius: 4px; white-space: pre-wrap;">
        ${citation.content}
      </div>
    </div>`,
    '引用详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}

const createNewSession = async () => {
  await sessionStore.createSession()
  chatStore.clearMessages()
  selectedKnowledgeBases.value = []
}

const selectSession = async (id: string) => {
  sessionStore.setCurrentSession(id)
  await chatStore.fetchMessages(id)
  await loadSessionKnowledgeBases(id)
  await loadSessionCitations(id)
  scrollToBottom()
}

// 加载会话所有助手消息的引用
const loadSessionCitations = async (sessionId: string) => {
  const messages = chatStore.messages.filter(m => m.role === 'assistant')
  for (const message of messages) {
    try {
      const citations = await chatApi.getCitations(message.id)
      if (citations && citations.length > 0) {
        messageCitations.value[message.id] = citations
      }
    } catch (error) {
      console.error('加载引用失败:', message.id, error)
    }
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  try {
    knowledgeBases.value = await knowledgeBaseApi.getAll()
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 加载会话关联的知识库
const loadSessionKnowledgeBases = async (sessionId: string) => {
  try {
    const kbs = await knowledgeBaseApi.getBySession(sessionId)
    selectedKnowledgeBases.value = kbs.map((kb: any) => kb.id)
  } catch (error) {
    console.error('加载会话知识库失败:', error)
    selectedKnowledgeBases.value = []
  }
}

// 处理知识库选择变化
const handleKnowledgeBaseChange = async (value: string[]) => {
  if (!sessionStore.currentSessionId) return
  
  try {
    await knowledgeBaseApi.associateToSession(sessionStore.currentSessionId, value)
    selectedKnowledgeBases.value = value
    
    if (value.length > 0) {
      ElMessage.success(`已关联 ${value.length} 个知识库`)
    } else {
      ElMessage.success('已清除知识库关联')
    }
  } catch (error) {
    console.error('更新知识库关联失败:', error)
    ElMessage.error('更新知识库关联失败')
    await loadSessionKnowledgeBases(sessionStore.currentSessionId)
  }
}

const handleSessionAction = async (action: string, sessionId: string) => {
  if (action === 'rename') {
    const session = sessionStore.sessions.find(s => s.id === sessionId)
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名', {
      inputValue: session?.title,
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    if (value) {
      await sessionStore.updateSession(sessionId, { title: value })
    }
  } else if (action === 'delete') {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await sessionStore.deleteSession(sessionId)
    if (sessionStore.currentSessionId === sessionId) {
      chatStore.clearMessages()
    }
  }
}

const handleModelChange = (value: string) => {
  if (value && value !== 'auto') {
    const [providerId, modelName] = value.split(':')
    console.log('切换模型:', { providerId, modelName, fullValue: value })
    modelStore.setActiveModel(providerId)
    modelStore.setActiveModelName(modelName)

    // 保存为默认模型
    localStorage.setItem('defaultChatModel', value)
    ElMessage.success(`已切换到模型: ${modelName}（已设为默认）`)
  }
}

const handleSmartModeChange = (enabled: boolean) => {
  if (enabled) {
    selectedModelId.value = 'auto'
    ElMessage.success('已开启智能模式，将根据输入自动选择最合适的模型')
  } else {
    if (modelStore.activeModelId) {
      const provider = modelStore.activeProvider
      if (provider) {
        selectedModelId.value = `${provider.id}:${modelStore.activeModelName || provider.modelName || ''}`
      }
    }
  }
  localStorage.setItem('smartMode', enabled ? 'true' : 'false')
}

const handleImageUpload = (file: File) => {
  let maxImages = 1
  
  if (selectedModelId.value && selectedModelId.value !== 'auto') {
    const [providerId, modelName] = selectedModelId.value.split(':')
    const provider = modelStore.providers.find(p => p.id === providerId)
    if (provider && provider.modelInfos) {
      const model = provider.modelInfos.find(m => m.id === modelName)
      if (model) {
        if (model.name.toLowerCase().includes('gpt-4') || 
            model.name.toLowerCase().includes('claude-3') ||
            model.name.toLowerCase().includes('gemini')) {
          maxImages = 10
        }
      }
    }
  }
  
  const existingImages = attachments.value.filter(f => f.type.startsWith('image/'))
  if (existingImages.length >= maxImages) {
    ElMessage.warning(`当前模型最多支持 ${maxImages} 张图片，请先删除已有图片`)
    return false
  }
  
  const allowedImageTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/bmp', 'image/webp']
  if (!allowedImageTypes.includes(file.type)) {
    ElMessage.error({
      message: '不支持的图片格式！仅支持：JPG、PNG、GIF、BMP、WEBP',
      duration: 3000
    })
    return false
  }
  
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error({
      message: `图片大小不能超过 10MB，当前大小：${(file.size / 1024 / 1024).toFixed(2)}MB`,
      duration: 3000
    })
    return false
  }
  
  attachments.value.push(file)
  ElMessage.success(`已添加图片: ${file.name} (${existingImages.length + 1}/${maxImages})`)
  return false
}

const handleFileUpload = (file: File) => {
  const allowedExtensions = ['txt', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'csv', 'json', 'xml', 'md']
  const fileName = file.name.toLowerCase()
  const extension = fileName.substring(fileName.lastIndexOf('.') + 1)
  
  if (!allowedExtensions.includes(extension)) {
    ElMessage.error({
      message: `不支持的文件格式！仅支持：${allowedExtensions.join('、').toUpperCase()}`,
      duration: 3000
    })
    return false
  }
  
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error({
      message: `文件大小不能超过 10MB，当前大小：${(file.size / 1024 / 1024).toFixed(2)}MB`,
      duration: 3000
    })
    return false
  }
  
  attachments.value.push(file)
  ElMessage.success(`已添加文件: ${file.name}`)
  return false
}

const removeAttachment = (index: number) => {
  const file = attachments.value[index]
  if (file.type.startsWith('image/')) {
    const url = URL.createObjectURL(file)
    URL.revokeObjectURL(url)
  }
  attachments.value.splice(index, 1)
}

const getFilePreviewUrl = (file: File) => {
  return URL.createObjectURL(file)
}

const sendMessage = async () => {
  if (!inputContent.value.trim() || !sessionStore.currentSessionId) return
  
  const content = inputContent.value
  inputContent.value = ''
  
  try {
    let modelId = selectedModelId.value
    
    // 智能模式：根据输入自动选择模型
    if (smartMode.value || modelId === 'auto') {
      const hasImage = attachments.value.some(f => f.type.startsWith('image/'))
      const hasFile = attachments.value.some(f => !f.type.startsWith('image/'))
      
      try {
        const smartSelection = await chatApi.smartSelectModel({
          content,
          hasImage,
          hasFile,
          sessionId: sessionStore.currentSessionId
        })
        
        if (smartSelection && smartSelection.providerId && smartSelection.modelName) {
          console.log('智能选择结果:', smartSelection)
          modelId = `${smartSelection.providerId}:${smartSelection.modelName}`
          modelStore.setActiveModel(smartSelection.providerId)
          modelStore.setActiveModelName(smartSelection.modelName)
          ElMessage.info(`智能选择: ${smartSelection.modelName}`)
        }
      } catch (error) {
        console.error('智能选择失败:', error)
      }
    }
    
    // 如果没有选择模型，使用默认模型
    if (!modelId || modelId === 'auto') {
      const defaultProvider = modelStore.activeProviders[0]
      if (defaultProvider && defaultProvider.modelInfos && defaultProvider.modelInfos.length > 0) {
        modelId = `${defaultProvider.id}:${defaultProvider.modelInfos[0].id}`
      } else {
        ElMessage.error('请先选择模型')
        return
      }
    }
    
    console.log('发送消息使用模型:', modelId)
    
    // 清空附件（暂不支持附件）
    attachments.value = []
    
    // 根据是否选择知识库来决定使用哪个接口
    const useKnowledgeBase = selectedKnowledgeBases.value.length > 0

    // 使用 chatStore.sendMessage，它会自动处理用户消息和助手消息
    await chatStore.sendMessage(
      sessionStore.currentSessionId,
      content,
      modelId,
      useKnowledgeBase
    )

    scrollToBottom()

    // 如果使用了知识库，等待一小段时间后获取引用（确保后端已保存）
    if (useKnowledgeBase) {
      // 先刷新消息列表，获取最新的消息ID
      await chatStore.fetchMessages(sessionStore.currentSessionId)

      // 获取最新的助手消息并加载引用
      const assistantMessages = chatStore.messages.filter(m => m.role === 'assistant')
      if (assistantMessages.length > 0) {
        const lastMessage = assistantMessages[assistantMessages.length - 1]
        try {
          const citations = await chatApi.getCitations(lastMessage.id)
          if (citations && citations.length > 0) {
            messageCitations.value[lastMessage.id] = citations
            // 清除缓存以触发重新渲染
            markdownCache.delete(`${lastMessage.content}_${lastMessage.id}`)
            // 强制更新视图
            await nextTick()
          }
        } catch (error) {
          console.error('获取引用失败:', error)
        }
      }
    }
    
    // 更新会话标题
    const currentSession = sessionStore.currentSession
    if (currentSession && currentSession.title === '新对话') {
      const title = content.length > 20 ? content.substring(0, 20) + '...' : content
      await sessionStore.updateSession(currentSession.id, { title })
    }
    
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
  }
}

// 重新发送用户消息
const resendMessage = async (message: any) => {
  inputContent.value = message.content
  // 自动触发发送
  await sendMessage()
}

// 批量管理模式
const enterBatchMode = () => {
  batchMode.value = true
  selectedSessions.value = []
  selectAll.value = false
}

const exitBatchMode = () => {
  batchMode.value = false
  selectedSessions.value = []
  selectAll.value = false
}

const toggleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedSessions.value = sessionStore.filteredSessions.map(s => s.id)
  } else {
    selectedSessions.value = []
  }
}

const toggleSession = (sessionId: string) => {
  const index = selectedSessions.value.indexOf(sessionId)
  if (index > -1) {
    selectedSessions.value.splice(index, 1)
  } else {
    selectedSessions.value.push(sessionId)
  }
  
  // 更新全选状态
  selectAll.value = selectedSessions.value.length === sessionStore.filteredSessions.length
}

const deleteSelectedSessions = async () => {
  if (selectedSessions.value.length === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedSessions.value.length} 个对话吗？此操作不可恢复。`,
      '批量删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )
    
    const loading = ElMessage({
      message: `正在删除 ${selectedSessions.value.length} 个对话...`,
      type: 'info',
      duration: 0
    })
    
    // 批量删除
    const sessionsToDelete = [...selectedSessions.value]
    let successCount = 0
    
    for (const sessionId of sessionsToDelete) {
      try {
        await sessionStore.deleteSession(sessionId)
        successCount++
      } catch (error) {
        console.error('删除会话失败:', sessionId, error)
      }
    }
    
    loading.close()
    
    if (successCount === sessionsToDelete.length) {
      ElMessage.success(`成功删除 ${successCount} 个对话`)
    } else {
      ElMessage.warning(`删除了 ${successCount}/${sessionsToDelete.length} 个对话`)
    }
    
    // 退出批量模式
    exitBatchMode()
    
    // 如果当前会话被删除，清空消息
    if (sessionsToDelete.includes(sessionStore.currentSessionId || '')) {
      chatStore.clearMessages()
    }
  } catch (error) {
    // 用户取消
  }
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

const regenerateMessage = async (messageId: string) => {
  try {
    await chatStore.regenerateMessage(messageId)
    scrollToBottom()
  } catch (error) {
    ElMessage.error('重新生成失败')
  }
}

const deleteMessage = async (messageId: string) => {
  await ElMessageBox.confirm('删除此消息及之后的所有消息？', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await chatStore.deleteMessage(messageId)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 防抖的滚动函数
let scrollTimeout: number | null = null
const debouncedScrollToBottom = () => {
  if (scrollTimeout) {
    clearTimeout(scrollTimeout)
  }
  scrollTimeout = window.setTimeout(scrollToBottom, 100)
}

const stopGeneration = () => {
  chatStore.stopStreaming()
}

// 监听流式内容变化，自动滚动
watch(() => chatStore.streamingContent, () => {
  debouncedScrollToBottom()
})

onMounted(async () => {
  await sessionStore.fetchSessions()
  await modelStore.fetchProviders()
  await loadKnowledgeBases()

  // 加载智能模式状态
  const savedSmartMode = localStorage.getItem('smartMode')
  if (savedSmartMode === 'true') {
    smartMode.value = true
  }

  // 设置默认选中的模型
  if (smartMode.value) {
    selectedModelId.value = 'auto'
  } else {
    // 优先使用用户保存的默认模型
    const savedDefaultModel = localStorage.getItem('defaultChatModel')
    if (savedDefaultModel) {
      // 验证保存的模型是否仍然可用
      const [providerId, modelName] = savedDefaultModel.split(':')
      const provider = modelStore.providers.find(p => p.id === providerId)
      if (provider && provider.modelInfos) {
        const model = provider.modelInfos.find(m => m.id === modelName)
        if (model && model.type !== 'embedding') {
          selectedModelId.value = savedDefaultModel
          modelStore.setActiveModel(providerId)
          modelStore.setActiveModelName(modelName)
        } else {
          // 保存的模型不可用，选择第一个非嵌入模型
          selectFirstChatModel()
        }
      } else {
        selectFirstChatModel()
      }
    } else {
      // 没有保存的默认模型，选择第一个非嵌入模型
      selectFirstChatModel()
    }
  }

  // Select first session if exists
  if (sessionStore.sessions.length > 0 && !sessionStore.currentSessionId) {
    await selectSession(sessionStore.sessions[0].id)
  }
})

// 选择第一个非嵌入的聊天模型
const selectFirstChatModel = () => {
  for (const provider of modelStore.activeProviders) {
    if (provider.modelInfos && provider.modelInfos.length > 0) {
      // 找到第一个非嵌入模型
      const chatModel = provider.modelInfos.find(m => m.type !== 'embedding')
      if (chatModel) {
        selectedModelId.value = `${provider.id}:${chatModel.id}`
        modelStore.setActiveModel(provider.id)
        modelStore.setActiveModelName(chatModel.id)
        return
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-view {
  display: flex;
  height: 100%;
}

.session-sidebar {
  width: 280px;
  border-right: 1px solid var(--border-color, #e0e0e0);
  display: flex;
  flex-direction: column;
  background: var(--sidebar-bg, #f9f9f9);
}

.sidebar-header {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  .el-button {
    width: 100%;
  }
  
  .batch-mode-header {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px;
    background: var(--el-color-primary-light-9);
    border-radius: 8px;
    
    .batch-mode-actions {
      display: flex;
      gap: 8px;
      
      .el-button {
        flex: 1;
      }
    }
  }
}

.search-box {
  padding: 0 16px 16px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.session-item {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  position: relative;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s;
  
  &:hover {
    background: var(--el-fill-color-light);
    
    .session-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background: var(--el-color-primary-light-9);
  }
  
  &.batch-mode {
    padding-left: 8px;
    
    .session-checkbox {
      margin-right: 8px;
    }
  }
  
  &.selected {
    background: var(--el-color-primary-light-8);
  }
  
  .session-info {
    flex: 1;
    min-width: 0;
  }
  
  .session-title {
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .session-meta {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
  
  .session-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s;
    
    .action-icon {
      padding: 4px;
      cursor: pointer;
      border-radius: 4px;
      
      &:hover {
        background: var(--el-fill-color);
      }
      
      &.delete-icon:hover {
        color: var(--el-color-danger);
      }
    }
  }
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color, #e0e0e0);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .chat-title {
    font-size: 18px;
    font-weight: 600;
  }
  
  .header-controls {
    display: flex;
    align-items: center;
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  
  &.user {
    flex-direction: row-reverse;
    
    .message-content {
      align-items: flex-end;
    }
    
    .message-text {
      background: var(--el-color-primary);
      color: white;
      border-radius: 16px 16px 4px 16px;
    }
  }
  
  &.assistant {
    .message-text {
      background: var(--el-fill-color-light);
      border-radius: 16px 16px 16px 4px;
    }
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
  
  .message-attachments {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 8px;
    
    .attachment-preview {
      .attachment-image {
        max-width: 200px;
        max-height: 200px;
        border-radius: 8px;
      }
      
      .attachment-file {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 8px 12px;
        background: var(--el-fill-color-light);
        border-radius: 8px;
      }
    }
  }
  
  .message-text {
    padding: 12px 16px;
    line-height: 1.6;
    word-break: break-word;
    
    :deep(pre) {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 12px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 8px 0;
      
      code {
        font-family: 'Fira Code', monospace;
        font-size: 14px;
      }
    }
    
    :deep(code) {
      background: var(--el-fill-color);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'Fira Code', monospace;
    }
    
    :deep(p) {
      margin: 8px 0;
      
      &:first-child {
        margin-top: 0;
      }
      
      &:last-child {
        margin-bottom: 0;
      }
    }
    
    :deep(ul), :deep(ol) {
      margin: 8px 0;
      padding-left: 20px;
    }
    
    :deep(blockquote) {
      border-left: 4px solid var(--el-color-primary);
      padding-left: 12px;
      margin: 8px 0;
      color: var(--el-text-color-secondary);
    }
    
    :deep(table) {
      border-collapse: collapse;
      margin: 8px 0;
      
      th, td {
        border: 1px solid var(--el-border-color);
        padding: 8px 12px;
      }
      
      th {
        background: var(--el-fill-color-light);
      }
    }
    
    :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
      margin: 16px 0 8px;
      font-weight: 600;
      
      &:first-child {
        margin-top: 0;
      }
    }
    
    :deep(h1) { font-size: 24px; }
    :deep(h2) { font-size: 20px; }
    :deep(h3) { font-size: 18px; }
    :deep(h4) { font-size: 16px; }
    :deep(h5) { font-size: 14px; }
    :deep(h6) { font-size: 13px; }
  }

  // 思考过程样式
  .thinking-block {
    margin-bottom: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    overflow: hidden;
    
    .thinking-header {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      cursor: pointer;
      user-select: none;
      
      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }
      
      .thinking-icon {
        color: #6366f1;
        
        &.is-loading {
          animation: spin 1s linear infinite;
        }
      }
      
      span {
        font-size: 13px;
        font-weight: 500;
        color: #4b5563;
      }
      
      .expand-icon {
        margin-left: auto;
        color: #9ca3af;
        transition: transform 0.2s;
        
        &.expanded {
          transform: rotate(180deg);
        }
      }
    }
    
    .thinking-content {
      padding: 12px 14px;
      font-size: 13px;
      color: #6b7280;
      line-height: 1.6;
      border-top: 1px solid #e5e7eb;
      background: white;
      white-space: pre-wrap;
    }
  }
  
  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
  
  .message-actions {
    display: flex;
    gap: 8px;
    margin-top: 8px;
    
    .el-icon {
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      color: var(--el-text-color-secondary);
      
      &:hover {
        background: var(--el-fill-color);
        color: var(--el-color-primary);
      }
    }
  }
  
  .message-citations {
    margin-top: 12px;
    
    .citations-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--el-text-color-secondary);
    }
  }

  // 简洁的引用来源样式
  .simple-citations {
    margin-bottom: 12px;
    padding: 10px 14px;
    background: #f0fdf4;
    border: 1px solid #a7f3d0;
    border-radius: 8px;

    .citations-header {
      font-size: 13px;
      font-weight: 500;
      color: #065f46;
      margin-bottom: 8px;
    }

    .citations-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .citation-tag {
      display: inline-block;
      padding: 4px 10px;
      background: white;
      border: 1px solid #a7f3d0;
      border-radius: 4px;
      font-size: 12px;
      color: #059669;
      cursor: pointer;

      &:hover {
        background: #ecfdf5;
        border-color: #10b981;
      }
    }
  }
}

.input-area {
  padding: 16px;
  border-top: 1px solid var(--border-color, #e0e0e0);
  
  .attachments-preview {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
    
    .attachment-preview-item {
      position: relative;
      
      .preview-content {
        .preview-image {
          width: 80px;
          height: 80px;
          object-fit: cover;
          border-radius: 8px;
        }
        
        .preview-file {
          display: flex;
          align-items: center;
          gap: 4px;
          padding: 8px 12px;
          background: var(--el-fill-color-light);
          border-radius: 8px;
          
          .file-name {
            max-width: 100px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
      
      .remove-icon {
        position: absolute;
        top: -6px;
        right: -6px;
        width: 20px;
        height: 20px;
        background: var(--el-color-danger);
        color: white;
        border-radius: 50%;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
      }
    }
  }
  
  .input-row {
    display: flex;
    gap: 12px;
    align-items: stretch;
    
    .input-actions {
      display: flex;
      flex-direction: column;
      gap: 6px;
      justify-content: center;
      
      // 确保 tooltip 和 upload 组件正确显示
      :deep(.el-tooltip__trigger) {
        display: block !important;
      }
      
      :deep(.el-upload) {
        display: block !important;
        width: 100%;
        height: auto;
      }
      
      :deep(.el-upload .el-icon) {
        margin: 0;
      }
      
      .action-icon {
        cursor: pointer;
        width: 32px;
        height: 32px;
        padding: 0;
        border-radius: 6px;
        background: #f0f2f5;
        border: 1px solid #e4e7ed;
        color: #606266;
        font-size: 16px;
        display: flex !important;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
        
        &:hover {
          background: #ecf5ff;
          border-color: #409eff;
          color: #409eff;
          transform: scale(1.05);
        }
      }
    }
    
    .el-textarea {
      flex: 1;
    }
    
    .button-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
      justify-content: flex-end;
    }
  }
}

.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  
  .model-name {
    display: flex;
    align-items: center;
    gap: 4px;
    
    &.model-failed {
      color: var(--el-color-danger);
    }
  }
  
  .model-badges {
    display: flex;
    gap: 4px;
    align-items: center;
  }
}

.kb-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  
  .kb-tags {
    display: flex;
    gap: 4px;
  }
}

// 简洁的引用样式 - IMA 风格
:deep(.cite-num) {
  color: #10b981;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  
  &:hover {
    color: #059669;
  }
  
  .cite-popup {
    display: none;
    position: absolute;
    bottom: 100%;
    left: 0;
    width: 280px;
    padding: 10px 12px;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 6px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    font-size: 12px;
    font-weight: normal;
    color: #374151;
    line-height: 1.5;
    z-index: 1000;
    margin-bottom: 6px;

    b {
      color: #1f2937;
      display: block;
      margin-bottom: 6px;
    }
  }

  &:hover .cite-popup {
    display: block;
  }
}

// 简单的上标数字（没有引用数据时）
:deep(.cite-num-simple) {
  color: #10b981;
  font-size: 11px;
  font-weight: 600;
}
</style>
