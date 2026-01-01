<template>
  <div class="knowledge-base-view">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <div class="icon-wrapper">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
          </div>
          <div>
            <h1>知识库</h1>
            <p class="subtitle">管理和组织您的文档知识库</p>
          </div>
        </div>
        <button @click="showCreateDialog = true" class="btn-create">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          创建知识库
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="knowledgeBases.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h3>还没有知识库</h3>
        <p>创建您的第一个知识库来存储和管理文档</p>
        <button @click="showCreateDialog = true" class="btn-create-empty">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          创建知识库
        </button>
      </div>

      <!-- 知识库列表 -->
      <div v-else class="kb-grid">
        <div
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="kb-card"
          @click="selectKnowledgeBase(kb)"
        >
          <div class="kb-card-header">
            <div class="kb-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
            </div>
            <div class="kb-actions">
              <button @click.stop="editKnowledgeBase(kb)" class="action-btn" title="编辑">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
              <button @click.stop="deleteKnowledgeBase(kb.id)" class="action-btn danger" title="删除">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            </div>
          </div>
          
          <h3 class="kb-title">{{ kb.name }}</h3>
          <p class="kb-description">{{ kb.description || '暂无描述' }}</p>
          
          <div class="kb-stats">
            <div class="stat-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
              <span>{{ kb.documentCount || 0 }} 文档</span>
            </div>
            <div class="stat-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              <span>{{ formatDate(kb.updatedAt) }}</span>
            </div>
          </div>
          
          <div class="kb-footer">
            <div class="kb-model">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
              </svg>
              <span>{{ getModelDisplayName(kb) }}</span>
            </div>
            <div class="kb-config">
              <span class="config-badge">{{ kb.chunkSize }}</span>
              <span class="config-badge">{{ kb.chunkOverlap }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <transition name="modal">
      <div v-if="showCreateDialog" class="modal-overlay" @click.self="closeDialog">
        <div class="modal-container">
          <div class="modal-header">
            <h2>{{ editingKb ? '编辑知识库' : '创建知识库' }}</h2>
            <button @click="closeDialog" class="close-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          
          <form @submit.prevent="saveKnowledgeBase" class="modal-body">
            <div class="form-section">
              <div class="form-group">
                <label class="form-label">
                  <span>知识库名称</span>
                  <span class="required">*</span>
                </label>
                <input 
                  v-model="formData.name" 
                  required 
                  placeholder="例如：产品文档、技术手册" 
                  class="form-input"
                  maxlength="50"
                />
              </div>
              
              <div class="form-group">
                <label class="form-label">描述</label>
                <textarea 
                  v-model="formData.description" 
                  placeholder="简要描述这个知识库的用途和内容" 
                  rows="3"
                  class="form-textarea"
                  maxlength="200"
                ></textarea>
              </div>
            </div>

            <div class="form-section">
              <h3 class="section-title">模型配置</h3>
              
              <div class="form-group">
                <label class="form-label">
                  <span>嵌入模型</span>
                  <span class="required">*</span>
                </label>
                <select 
                  v-model="formData.embeddingModel" 
                  @focus="loadEmbeddingModels"
                  class="form-select"
                  required
                >
                  <option value="" disabled>请选择嵌入模型</option>
                  <option 
                    v-for="model in embeddingModels" 
                    :key="model.id" 
                    :value="model.id"
                  >
                    {{ model.name }} · {{ model.provider }}
                  </option>
                </select>
                <p class="form-hint" v-if="embeddingModels.length === 0">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="12" y1="8" x2="12" y2="12"></line>
                    <line x1="12" y1="16" x2="12.01" y2="16"></line>
                  </svg>
                  请先在设置页面配置模型提供商
                </p>
              </div>
            </div>

            <div class="form-section">
              <h3 class="section-title">分块配置</h3>
              
              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">
                    <span>分块大小</span>
                    <span class="label-hint">字符数</span>
                  </label>
                  <input 
                    v-model.number="formData.chunkSize" 
                    type="number" 
                    min="100" 
                    max="2000"
                    class="form-input"
                  />
                  <p class="form-hint">建议 300-1000 字符</p>
                </div>
                
                <div class="form-group">
                  <label class="form-label">
                    <span>分块重叠</span>
                    <span class="label-hint">字符数</span>
                  </label>
                  <input 
                    v-model.number="formData.chunkOverlap" 
                    type="number" 
                    min="0" 
                    max="500"
                    class="form-input"
                  />
                  <p class="form-hint">建议 10-100 字符</p>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" @click="closeDialog" class="btn-cancel">
                取消
              </button>
              <button type="submit" class="btn-submit">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
                {{ editingKb ? '保存更改' : '创建知识库' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()

interface KnowledgeBase {
  id: string
  name: string
  description?: string
  embeddingModel: string
  chunkSize: number
  chunkOverlap: number
  documentCount?: number
  createdAt: number
  updatedAt: number
}

const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(false)
const showCreateDialog = ref(false)
const editingKb = ref<KnowledgeBase | null>(null)
const embeddingModels = ref<any[]>([])

const formData = ref({
  name: '',
  description: '',
  embeddingModel: '',
  chunkSize: 500,
  chunkOverlap: 50
})

const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const response = await api.get('/knowledge-bases')
    knowledgeBases.value = response.data
  } catch (error) {
    console.error('加载知识库失败:', error)
    alert('加载知识库失败')
  } finally {
    loading.value = false
  }
}

const loadEmbeddingModels = async () => {
  // 移除缓存检查,每次都重新加载以确保获取最新数据
  // if (embeddingModels.value.length > 0) return
  
  console.log('开始加载嵌入模型...')
  
  try {
    // 获取所有模型提供商
    const providersResponse = await api.get('/model-providers')
    const providers = providersResponse.data
    const models = []
    
    console.log('获取到的提供商数量:', providers.length)
    
    // 常见的嵌入模型关键词和前缀
    const embeddingKeywords = [
      'embedding',
      'embed',
      'bge',           // BAAI General Embedding
      'gte',           // General Text Embedding
      'm3e',           // M3E embedding
      'text2vec',      // text2vec 系列
      'sentence',      // sentence-transformers
      'e5',            // E5 embedding models
      'instructor',    // instructor embedding
      'multilingual-e5' // multilingual embedding
    ]
    
    // 判断是否为嵌入模型
    const isEmbeddingModel = (model) => {
      const modelId = (model.id || '').toLowerCase()
      const modelName = (model.name || '').toLowerCase()
      
      // 检查是否包含嵌入模型关键词
      return embeddingKeywords.some(keyword => 
        modelId.includes(keyword) || modelName.includes(keyword)
      )
    }
    
    // 为每个活跃的提供商获取模型列表
    for (const provider of providers) {
      if (provider.isActive) {
        try {
          const modelsResponse = await api.get(`/model-providers/${provider.id}/models`)
          const providerModels = modelsResponse.data
          
          // 筛选出嵌入模型
          const embeddingModelList = providerModels.filter(isEmbeddingModel)
          
          embeddingModelList.forEach(model => {
            // 为每个模型生成唯一ID: providerId::modelId
            const uniqueId = `${provider.id}::${model.id}`
            const modelData = {
              id: uniqueId,  // 使用唯一ID
              originalId: model.id,  // 保存原始模型ID
              name: model.name || model.id,
              provider: provider.name,
              providerId: provider.id,
              displayName: `${model.name || model.id} (${provider.name})`  // 显示名称包含提供商
            }
            console.log('添加嵌入模型:', modelData)
            models.push(modelData)
          })
        } catch (error) {
          console.warn(`获取提供商 ${provider.name} 的模型失败:`, error)
        }
      }
    }
    
    // 如果没有找到嵌入模型，添加默认选项
    if (models.length === 0) {
      models.push(
        { id: 'default::text-embedding-ada-002', originalId: 'text-embedding-ada-002', name: 'text-embedding-ada-002', provider: 'OpenAI', displayName: 'text-embedding-ada-002 (OpenAI)' },
        { id: 'default::text-embedding-3-small', originalId: 'text-embedding-3-small', name: 'text-embedding-3-small', provider: 'OpenAI', displayName: 'text-embedding-3-small (OpenAI)' },
        { id: 'default::text-embedding-3-large', originalId: 'text-embedding-3-large', name: 'text-embedding-3-large', provider: 'OpenAI', displayName: 'text-embedding-3-large (OpenAI)' }
      )
    }
    
    embeddingModels.value = models
    
    console.log('最终加载的嵌入模型数量:', models.length)
    console.log('嵌入模型列表:', models)
    
    // 如果表单中没有选择模型，默认选择第一个
    if (!formData.value.embeddingModel && models.length > 0) {
      formData.value.embeddingModel = models[0].id
    }
  } catch (error) {
    console.error('加载嵌入模型失败:', error)
    // 使用默认模型
    embeddingModels.value = [
      { id: 'default::text-embedding-ada-002', originalId: 'text-embedding-ada-002', name: 'text-embedding-ada-002', provider: 'OpenAI', displayName: 'text-embedding-ada-002 (OpenAI)' },
      { id: 'default::text-embedding-3-small', originalId: 'text-embedding-3-small', name: 'text-embedding-3-small', provider: 'OpenAI', displayName: 'text-embedding-3-small (OpenAI)' },
      { id: 'default::text-embedding-3-large', originalId: 'text-embedding-3-large', name: 'text-embedding-3-large', provider: 'OpenAI', displayName: 'text-embedding-3-large (OpenAI)' }
    ]
    if (!formData.value.embeddingModel) {
      formData.value.embeddingModel = 'default::text-embedding-ada-002'
    }
  }
}

const saveKnowledgeBase = async () => {
  try {
    console.log('保存知识库, 原始 formData:', formData.value)
    
    // 准备保存的数据
    const saveData = { ...formData.value }
    
    console.log('embeddingModel 值:', saveData.embeddingModel)
    console.log('是否包含 "::":', saveData.embeddingModel && saveData.embeddingModel.includes('::'))
    
    // 如果embeddingModel是新格式的唯一ID,提取原始模型ID和提供商信息
    if (saveData.embeddingModel && saveData.embeddingModel.includes('::')) {
      const parts = saveData.embeddingModel.split('::')
      const providerId = parts[0]
      const originalId = parts[1]
      
      // 查找提供商信息
      const model = embeddingModels.value.find(m => m.id === saveData.embeddingModel)
      
      console.log('提取原始模型ID:', originalId)
      console.log('提供商ID:', providerId)
      console.log('找到的模型信息:', model)
      
      saveData.embeddingModel = originalId  // 使用原始模型ID
      saveData.providerId = providerId
      saveData.providerName = model ? model.provider : ''
    }
    
    console.log('最终保存的数据:', saveData)
    
    if (editingKb.value) {
      await api.put(`/knowledge-bases/${editingKb.value.id}`, saveData)
    } else {
      await api.post('/knowledge-bases', saveData)
    }
    showCreateDialog.value = false
    resetForm()
    await loadKnowledgeBases()
  } catch (error) {
    console.error('保存知识库失败:', error)
    alert('保存知识库失败')
  }
}

const editKnowledgeBase = async (kb: KnowledgeBase) => {
  editingKb.value = kb
  
  // 确保加载了嵌入模型列表
  await loadEmbeddingModels()
  
  // 尝试找到匹配的唯一ID
  let embeddingModelId = kb.embeddingModel
  const matchingModel = embeddingModels.value.find(m => m.originalId === kb.embeddingModel)
  if (matchingModel) {
    embeddingModelId = matchingModel.id  // 使用唯一ID
  }
  
  formData.value = {
    name: kb.name,
    description: kb.description || '',
    embeddingModel: embeddingModelId,
    chunkSize: kb.chunkSize,
    chunkOverlap: kb.chunkOverlap
  }
  showCreateDialog.value = true
}

const deleteKnowledgeBase = async (id: string) => {
  if (!confirm('确定要删除这个知识库吗？这将删除所有相关文档。')) {
    return
  }
  try {
    await api.delete(`/knowledge-bases/${id}`)
    await loadKnowledgeBases()
  } catch (error) {
    console.error('删除知识库失败:', error)
    alert('删除知识库失败')
  }
}

const selectKnowledgeBase = (kb: KnowledgeBase) => {
  router.push(`/knowledge-base/${kb.id}`)
}

const resetForm = () => {
  editingKb.value = null
  formData.value = {
    name: '',
    description: '',
    embeddingModel: embeddingModels.value.length > 0 ? embeddingModels.value[0].id : '',
    chunkSize: 500,
    chunkOverlap: 50
  }
}

const closeDialog = () => {
  showCreateDialog.value = false
  resetForm()
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  if (days < 30) return `${Math.floor(days / 7)} 周前`
  if (days < 365) return `${Math.floor(days / 30)} 月前`
  return `${Math.floor(days / 365)} 年前`
}

const getModelDisplayName = (kb: KnowledgeBase) => {
  const modelId = kb.embeddingModel
  if (!modelId) return '未设置'
  
  // 如果有提供商名称,显示"模型名 (提供商)"
  if (kb.providerName) {
    const parts = modelId.split('/')
    const modelName = parts[parts.length - 1]
    return `${modelName} (${kb.providerName})`
  }
  
  // 否则只显示模型名
  const parts = modelId.split('/')
  return parts[parts.length - 1]
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.knowledge-base-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
}

/* 页面头部 */
.page-header {
  background: white;
  border-bottom: 1px solid #e8eaed;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.title-section h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.02em;
}

.subtitle {
  margin: 0.25rem 0 0 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.btn-create {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-create:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-create:active {
  transform: translateY(0);
}

/* 内容区域 */
.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rem 2rem;
  color: #6b7280;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 6rem 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 1.5rem;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.empty-state h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
}

.empty-state p {
  margin: 0 0 2rem 0;
  color: #6b7280;
  font-size: 0.9375rem;
}

.btn-create-empty {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.875rem 2rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-create-empty:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 知识库网格 */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 1.5rem;
}

/* 知识库卡片 */
.kb-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e8eaed;
  position: relative;
  overflow: hidden;
}

.kb-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.kb-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #667eea;
}

.kb-card:hover::before {
  transform: scaleX(1);
}

.kb-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.kb-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #667eea;
}

.kb-actions {
  display: flex;
  gap: 0.5rem;
  opacity: 0;
  transition: opacity 0.2s;
}

.kb-card:hover .kb-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.action-btn.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.kb-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}

.kb-description {
  margin: 0 0 1rem 0;
  color: #6b7280;
  font-size: 0.875rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.625rem;
}

.kb-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f3f4f6;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #6b7280;
}

.stat-item svg {
  color: #9ca3af;
}

.kb-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kb-model {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #667eea;
  font-weight: 500;
}

.kb-model svg {
  color: #667eea;
}

.kb-config {
  display: flex;
  gap: 0.5rem;
}

.config-badge {
  background: #f3f4f6;
  color: #6b7280;
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.modal-body {
  padding: 2rem;
  overflow-y: auto;
}

.form-section {
  margin-bottom: 2rem;
}

.form-section:last-of-type {
  margin-bottom: 0;
}

.section-title {
  margin: 0 0 1rem 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1a1a1a;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.required {
  color: #dc2626;
}

.label-hint {
  color: #9ca3af;
  font-weight: 400;
  font-size: 0.8125rem;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 0.9375rem;
  color: #1a1a1a;
  transition: all 0.2s;
  font-family: inherit;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-select {
  background-color: white;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1.5L6 6.5L11 1.5' stroke='%236b7280' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  padding-right: 2.5rem;
}

.form-hint {
  margin: 0.5rem 0 0 0;
  font-size: 0.8125rem;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.modal-footer {
  padding: 1.5rem 2rem;
  border-top: 1px solid #e8eaed;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.btn-cancel {
  background: #f3f4f6;
  color: #374151;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #e5e7eb;
}

.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-submit:active {
  transform: translateY(0);
}

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.95) translateY(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .title-section {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .btn-create {
    width: 100%;
    justify-content: center;
  }
  
  .kb-grid {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
