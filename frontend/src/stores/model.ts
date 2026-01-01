import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ModelProvider } from '@/types'
import { modelApi } from '@/api'

export const useModelStore = defineStore('model', () => {
  const providers = ref<ModelProvider[]>([])
  const activeModelId = ref<string | null>(null)
  const activeModelName = ref<string | null>(null)
  const loading = ref(false)

  const activeProvider = computed(() => 
    providers.value.find(p => p.id === activeModelId.value)
  )

  const activeProviders = computed(() => 
    providers.value.filter(p => p.isActive)
  )

  async function fetchProviders() {
    loading.value = true
    try {
      providers.value = await modelApi.getAll()
      // Set default active model
      if (!activeModelId.value && providers.value.length > 0) {
        const active = providers.value.find(p => p.isActive)
        if (active) {
          activeModelId.value = active.id
          activeModelName.value = active.modelName || (active.models?.[0]) || null
        }
      }
    } finally {
      loading.value = false
    }
  }

  async function createProvider(provider: Partial<ModelProvider>) {
    const created = await modelApi.create(provider)
    providers.value.push(created)
    return created
  }

  async function updateProvider(id: string, data: Partial<ModelProvider>) {
    const updated = await modelApi.update(id, data)
    const index = providers.value.findIndex(p => p.id === id)
    if (index !== -1) {
      providers.value[index] = updated
    }
    return updated
  }

  async function deleteProvider(id: string) {
    await modelApi.delete(id)
    providers.value = providers.value.filter(p => p.id !== id)
    if (activeModelId.value === id) {
      activeModelId.value = providers.value[0]?.id || null
      activeModelName.value = null
    }
  }

  async function testConnection(id: string) {
    return await modelApi.testConnection(id)
  }

  function setActiveModel(id: string) {
    activeModelId.value = id
    localStorage.setItem('activeModelId', id)
  }

  function setActiveModelName(name: string) {
    activeModelName.value = name
    localStorage.setItem('activeModelName', name)
  }

  function loadActiveModel() {
    const savedId = localStorage.getItem('activeModelId')
    const savedName = localStorage.getItem('activeModelName')
    
    if (savedId && providers.value.find(p => p.id === savedId)) {
      activeModelId.value = savedId
      activeModelName.value = savedName
    }
  }

  // 智能选择模型
  function selectSmartModel(options: {
    hasImage?: boolean
    hasFile?: boolean
    fileType?: string
    textLength?: number
  }): { providerId: string; modelId: string; modelName: string } | null {
    const { hasImage, hasFile, fileType, textLength = 0 } = options
    
    // 获取所有可用的模型
    const allModels: Array<{
      providerId: string
      modelId: string
      modelName: string
      model: any
      provider: ModelProvider
    }> = []
    
    activeProviders.value.forEach(provider => {
      if (provider.modelInfos && provider.modelInfos.length > 0) {
        provider.modelInfos.forEach(model => {
          // 使用模型的实际名称，而不是ID
          const actualModelName = model.id || model.name
          allModels.push({
            providerId: provider.id,
            modelId: `${provider.id}:${actualModelName}`,
            modelName: model.name,
            model,
            provider
          })
        })
      }
    })
    
    if (allModels.length === 0) return null
    
    // 智能选择逻辑
    let candidates = allModels
    
    // 1. 如果有图片，筛选支持视觉的模型
    if (hasImage) {
      const visionModels = candidates.filter(m => m.model.supportVision)
      if (visionModels.length > 0) {
        candidates = visionModels
      }
    }
    
    // 2. 如果有文件或代码，优先选择支持工具的模型
    if (hasFile) {
      const toolModels = candidates.filter(m => m.model.supportTools)
      if (toolModels.length > 0) {
        candidates = toolModels
      }
    }
    
    // 3. 如果文本很长，优先选择长上下文模型
    if (textLength > 10000) {
      const longContextModels = candidates.filter(m => 
        m.model.contextLength && m.model.contextLength >= 100000
      )
      if (longContextModels.length > 0) {
        candidates = longContextModels
      }
    }
    
    // 4. 过滤掉失败的模型
    const healthyModels = candidates.filter(m => !m.model.isFailed)
    if (healthyModels.length > 0) {
      candidates = healthyModels
    }
    
    // 5. 优先级排序
    candidates.sort((a, b) => {
      // 如果有图片，优先选择高质量视觉模型
      if (hasImage) {
        // GPT-4 Vision 系列最优
        const aIsGPT4Vision = a.modelName.toLowerCase().includes('gpt-4') && a.model.supportVision
        const bIsGPT4Vision = b.modelName.toLowerCase().includes('gpt-4') && b.model.supportVision
        if (aIsGPT4Vision && !bIsGPT4Vision) return -1
        if (!aIsGPT4Vision && bIsGPT4Vision) return 1
        
        // Claude 3 系列次优
        const aIsClaude3 = a.modelName.toLowerCase().includes('claude-3') && a.model.supportVision
        const bIsClaude3 = b.modelName.toLowerCase().includes('claude-3') && b.model.supportVision
        if (aIsClaude3 && !bIsClaude3) return -1
        if (!aIsClaude3 && bIsClaude3) return 1
        
        // Gemini Vision 第三
        const aIsGemini = a.modelName.toLowerCase().includes('gemini') && a.model.supportVision
        const bIsGemini = b.modelName.toLowerCase().includes('gemini') && b.model.supportVision
        if (aIsGemini && !bIsGemini) return -1
        if (!aIsGemini && bIsGemini) return 1
        
        // DeepSeek 第四
        const aIsDeepSeek = a.modelName.toLowerCase().includes('deepseek') && a.model.supportVision
        const bIsDeepSeek = b.modelName.toLowerCase().includes('deepseek') && b.model.supportVision
        if (aIsDeepSeek && !bIsDeepSeek) return -1
        if (!aIsDeepSeek && bIsDeepSeek) return 1
        
        // 其他 llama 等模型排最后
      }
      
      // 普通文本对话的优先级
      // 优先选择 DeepSeek 系列（性价比高）
      if (a.model.group === 'deepseek' && b.model.group !== 'deepseek') return -1
      if (a.model.group !== 'deepseek' && b.model.group === 'deepseek') return 1
      
      // 其次选择 GPT-4 系列
      if (a.model.group === 'gpt' && b.model.group !== 'gpt') return -1
      if (a.model.group !== 'gpt' && b.model.group === 'gpt') return 1
      
      // 再次选择 Claude 系列
      if (a.model.group === 'claude' && b.model.group !== 'claude') return -1
      if (a.model.group !== 'claude' && b.model.group === 'claude') return 1
      
      // 按上下文长度排序
      const aContext = a.model.contextLength || 0
      const bContext = b.model.contextLength || 0
      return bContext - aContext
    })
    
    // 返回最佳候选
    const best = candidates[0]
    return {
      providerId: best.providerId,
      modelId: best.modelId,
      modelName: best.modelName
    }
  }

  return {
    providers,
    activeModelId,
    activeModelName,
    activeProvider,
    activeProviders,
    loading,
    fetchProviders,
    createProvider,
    updateProvider,
    deleteProvider,
    testConnection,
    setActiveModel,
    setActiveModelName,
    loadActiveModel,
    selectSmartModel
  }
})
