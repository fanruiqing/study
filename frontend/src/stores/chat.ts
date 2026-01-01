import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Message } from '@/types'
import { chatApi } from '@/api'
import { useSessionStore } from './session'
import { useModelStore } from './model'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<Message[]>([])
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const thinkingContent = ref('')  // 思考过程内容
  const isThinking = ref(false)    // 是否正在思考
  const loading = ref(false)
  let updateScheduled = false

  async function fetchMessages(sessionId: string) {
    loading.value = true
    try {
      const loadedMessages = await chatApi.getMessages(sessionId)
      
      // 去重：如果有相同session_id、role、content和相近timestamp的消息，只保留最早的
      const uniqueMessages: Message[] = []
      const seen = new Map<string, Message>()
      
      for (const msg of loadedMessages) {
        const key = `${msg.sessionId}-${msg.role}-${msg.content}`
        const existing = seen.get(key)
        
        if (!existing) {
          seen.set(key, msg)
          uniqueMessages.push(msg)
        } else {
          // 如果时间戳相差很小（比如1秒内），认为是重复的，保留时间戳更小的
          if (Math.abs(msg.timestamp - existing.timestamp) < 1000) {
            if (msg.timestamp < existing.timestamp) {
              // 替换为更早的消息
              const index = uniqueMessages.indexOf(existing)
              if (index !== -1) {
                uniqueMessages[index] = msg
                seen.set(key, msg)
              }
            }
            // 否则忽略这条重复消息
          } else {
            // 时间差较大，不是重复消息
            uniqueMessages.push(msg)
          }
        }
      }
      
      messages.value = uniqueMessages
    } finally {
      loading.value = false
    }
  }

  async function sendMessage(sessionId: string, content: string, modelId: string, useKnowledgeBase = false) {
    isStreaming.value = true
    streamingContent.value = ''
    thinkingContent.value = ''
    isThinking.value = false

    // Add user message immediately
    const userMessage: Message = {
      id: crypto.randomUUID(),
      sessionId,
      role: 'user',
      content,
      timestamp: Date.now(),
      modelId
    }
    messages.value.push(userMessage)

    // Create placeholder for assistant message
    const assistantMessage: Message = {
      id: crypto.randomUUID(),
      sessionId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      modelId,
      thinking: ''  // 添加思考内容字段
    }
    messages.value.push(assistantMessage)

    return new Promise<void>((resolve, reject) => {
      // 根据是否使用知识库选择不同的API
      const eventSource = useKnowledgeBase 
        ? chatApi.streamChatWithKnowledge(sessionId, content, modelId)
        : chatApi.streamChat(sessionId, content, modelId)
      
      // 设置超时定时器 (60秒)
      const timeout = setTimeout(async () => {
        eventSource.close()
        isStreaming.value = false
        isThinking.value = false
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage.role === 'assistant' && !lastMessage.content) {
          const errorContent = '⚠️ 请求超时，请检查网络连接或稍后重试'
          lastMessage.content = errorContent
          // 保存超时消息到数据库
          try {
            await chatApi.saveMessage({
              sessionId,
              role: 'assistant',
              content: errorContent,
              modelId
            })
            // 刷新模型列表以显示健康状态
            const modelStore = useModelStore()
            await modelStore.fetchProviders()
          } catch (err) {
            console.error('保存超时消息失败:', err)
          }
        }
        reject(new Error('请求超时'))
      }, 60000)
      
      // 保存 eventSource 引用，用于手动停止
      ;(window as any).__currentEventSource = eventSource

      eventSource.onmessage = (event) => {
        clearTimeout(timeout) // 收到数据后清除超时
        let chunk = event.data
        
        // 检查是否是思考内容
        if (chunk.startsWith('[[THINKING]]')) {
          isThinking.value = true
          const thinkingChunk = chunk.substring(12) // 移除 [[THINKING]] 前缀
          thinkingContent.value += thinkingChunk
          
          // 更新消息的思考内容
          if (!updateScheduled) {
            updateScheduled = true
            requestAnimationFrame(() => {
              const lastMessage = messages.value[messages.value.length - 1]
              if (lastMessage.role === 'assistant') {
                lastMessage.thinking = thinkingContent.value
              }
              updateScheduled = false
            })
          }
        } else {
          // 正常内容，结束思考状态
          if (isThinking.value) {
            isThinking.value = false
          }
          
          streamingContent.value += chunk
          // 使用 requestAnimationFrame 来批量更新，避免频繁渲染
          if (!updateScheduled) {
            updateScheduled = true
            requestAnimationFrame(() => {
              const lastMessage = messages.value[messages.value.length - 1]
              if (lastMessage.role === 'assistant') {
                lastMessage.content = streamingContent.value
              }
              updateScheduled = false
            })
          }
        }
      }

      eventSource.addEventListener('done', () => {
        clearTimeout(timeout)
        eventSource.close()
        isStreaming.value = false
        isThinking.value = false
        streamingContent.value = ''
        thinkingContent.value = ''
        ;(window as any).__currentEventSource = null
        // Refresh messages to get server-saved version
        fetchMessages(sessionId)
        // Update session timestamp
        const sessionStore = useSessionStore()
        sessionStore.fetchSessions()
        resolve()
      })

      eventSource.addEventListener('error', async () => {
        clearTimeout(timeout)
        eventSource.close()
        isStreaming.value = false
        isThinking.value = false
        streamingContent.value = ''
        thinkingContent.value = ''
        ;(window as any).__currentEventSource = null
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage.role === 'assistant' && !lastMessage.content) {
          const errorContent = '❌ 连接错误，请检查网络或模型配置'
          lastMessage.content = errorContent
          // 保存错误消息到数据库
          try {
            await chatApi.saveMessage({
              sessionId,
              role: 'assistant',
              content: errorContent,
              modelId
            })
            // 刷新模型列表以显示健康状态
            const modelStore = useModelStore()
            await modelStore.fetchProviders()
          } catch (err) {
            console.error('保存错误消息失败:', err)
          }
        }
        reject(new Error('Stream error'))
      })

      eventSource.onerror = () => {
        clearTimeout(timeout)
        eventSource.close()
        isStreaming.value = false
        isThinking.value = false
        streamingContent.value = ''
        thinkingContent.value = ''
        ;(window as any).__currentEventSource = null
        reject(new Error('Connection error'))
      }
    })
  }

  function stopStreaming() {
    const eventSource = (window as any).__currentEventSource
    if (eventSource) {
      eventSource.close()
      ;(window as any).__currentEventSource = null
    }
    isStreaming.value = false
    isThinking.value = false
    streamingContent.value = ''
    thinkingContent.value = ''
  }

  async function regenerateMessage(messageId: string) {
    const message = messages.value.find(m => m.id === messageId)
    if (!message || message.role !== 'assistant') return

    isStreaming.value = true
    streamingContent.value = ''
    message.content = ''

    return new Promise<void>((resolve, reject) => {
      const eventSource = chatApi.regenerate(messageId)

      eventSource.onmessage = (event) => {
        const chunk = event.data
        streamingContent.value += chunk
        message.content = streamingContent.value
      }

      eventSource.addEventListener('done', () => {
        eventSource.close()
        isStreaming.value = false
        streamingContent.value = ''
        resolve()
      })

      eventSource.onerror = () => {
        eventSource.close()
        isStreaming.value = false
        reject(new Error('Regenerate error'))
      }
    })
  }

  async function deleteMessage(messageId: string) {
    await chatApi.deleteMessage(messageId)
    const index = messages.value.findIndex(m => m.id === messageId)
    if (index !== -1) {
      messages.value = messages.value.slice(0, index)
    }
  }

  async function rateMessage(messageId: string, rating: number) {
    await chatApi.rateMessage(messageId, rating)
    const message = messages.value.find(m => m.id === messageId)
    if (message) {
      message.rating = rating
    }
  }

  function copyMessage(content: string) {
    navigator.clipboard.writeText(content)
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    messages,
    isStreaming,
    streamingContent,
    thinkingContent,
    isThinking,
    loading,
    fetchMessages,
    sendMessage,
    regenerateMessage,
    deleteMessage,
    rateMessage,
    copyMessage,
    clearMessages,
    stopStreaming
  }
})
