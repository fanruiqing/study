<template>
  <div class="settings-view">
    <div class="settings-header">
      <h2>设置</h2>
    </div>
    
    <el-tabs v-model="activeTab">
      <el-tab-pane label="模型配置" name="models">
        <div class="models-layout">
          <!-- 左侧：服务商列表 -->
          <div class="providers-sidebar">
            <div class="sidebar-header">
              <h3>服务商</h3>
              <el-button type="primary" size="small" :icon="Plus" @click="showAddProvider">
                添加
              </el-button>
            </div>
            <div class="providers-list">
              <div 
                v-for="provider in modelStore.providers" 
                :key="provider.id"
                :class="['provider-item', { active: selectedProviderId === provider.id }]"
                @click="selectProvider(provider.id)"
              >
                <div class="provider-item-header">
                  <span class="provider-item-name">{{ provider.name }}</span>
                  <el-icon 
                    v-if="provider.isActive" 
                    :size="14" 
                    color="#67c23a"
                    class="status-icon"
                  >
                    <CircleCheck />
                  </el-icon>
                </div>
                <div class="provider-item-info">
                  <span class="model-count">{{ provider.modelInfos?.length || 0 }} 个模型</span>
                </div>
              </div>
              <el-empty v-if="modelStore.providers.length === 0" description="暂无服务商" />
            </div>
          </div>
          
          <!-- 右侧：模型详情 -->
          <div class="models-content">
            <div v-if="selectedProvider" class="provider-detail">
              <div class="detail-header">
                <div class="detail-title">
                  <h3>{{ selectedProvider.name }}</h3>
                  <el-tag size="small" :type="selectedProvider.isActive ? 'success' : 'info'">
                    {{ selectedProvider.isActive ? '已启用' : '已禁用' }}
                  </el-tag>
                </div>
                <div class="detail-actions">
                  <el-button 
                    size="small" 
                    :loading="testingConnection[selectedProvider.id]"
                    @click="testConnection(selectedProvider.id)"
                  >
                    <el-icon><Connection /></el-icon>
                    测试连接
                  </el-button>
                  <el-button size="small" @click="refreshModels(selectedProvider.id)">
                    <el-icon><Refresh /></el-icon>
                    刷新模型
                  </el-button>
                  <el-button size="small" @click="editProvider(selectedProvider)">
                    <el-icon><Edit /></el-icon>
                    编辑
                  </el-button>
                  <el-button size="small" type="danger" @click="deleteProvider(selectedProvider.id)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </div>
              
              <div class="detail-info">
                <div class="info-item">
                  <span class="info-label">Base URL:</span>
                  <span class="info-value">{{ selectedProvider.baseUrl }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">API Key:</span>
                  <span class="info-value">{{ selectedProvider.apiKey }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">默认模型:</span>
                  <span class="info-value">{{ selectedProvider.modelName || '未设置' }}</span>
                </div>
              </div>
              
              <!-- 分组显示模型 -->
              <div v-if="groupedModels && Object.keys(groupedModels).length > 0" class="models-groups">
                <div v-for="(models, group) in groupedModels" :key="group" class="model-group">
                  <div class="group-header">
                    <span class="group-name">{{ getGroupDisplayName(group) }}</span>
                    <span class="group-count">{{ models.length }}</span>
                  </div>
                  <div class="group-models">
                    <div 
                      v-for="model in models" 
                      :key="model.id"
                      :class="['model-item', { selected: selectedProvider.modelName === model.id }]"
                    >
                      <div class="model-item-content" @click="selectModel(selectedProvider.id, model.id)">
                        <div class="model-item-main">
                          <span class="model-name" :class="{ 'model-failed': model.isFailed }">
                            {{ model.name }}
                            <el-icon v-if="model.isFailed" color="#f56c6c" :size="14" title="该模型调用失败过">
                              <WarningFilled />
                            </el-icon>
                          </span>
                          <div class="model-badges">
                            <el-icon 
                              v-if="model.supportTools" 
                              :size="16" 
                              color="#409eff"
                              title="支持工具调用/MCP"
                            >
                              <Tools />
                            </el-icon>
                            <el-icon 
                              v-if="model.supportVision" 
                              :size="16" 
                              color="#67c23a"
                              title="支持视觉"
                            >
                              <View />
                            </el-icon>
                            <el-tag 
                              v-if="model.type === 'embedding'" 
                              size="small" 
                              type="warning"
                            >
                              嵌入
                            </el-tag>
                          </div>
                        </div>
                        <div v-if="model.contextLength" class="model-context">
                          {{ formatContextLength(model.contextLength) }}
                        </div>
                      </div>
                      <div class="model-item-actions">
                        <el-button 
                          v-if="model.isFailed"
                          size="small" 
                          text
                          type="warning"
                          @click.stop="clearFailedMark(selectedProvider.id, model.id)"
                          title="清除失败标记"
                        >
                          <el-icon><CircleClose /></el-icon>
                        </el-button>
                        <el-button 
                          size="small" 
                          text
                          :loading="testingModel[model.id]"
                          @click.stop="testSingleModel(selectedProvider.id, model.id)"
                        >
                          <el-icon><Connection /></el-icon>
                          测试
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无模型" />
            </div>
            <el-empty v-else description="请选择一个服务商" />
          </div>
        </div>
      </el-tab-pane>
      
      <el-tab-pane label="数据管理" name="data">
        <div class="section">
          <h3>导出数据</h3>
          <p class="section-desc">导出所有对话记录和设置</p>
          <div class="button-group">
            <el-button @click="exportJson">导出 JSON</el-button>
            <el-button @click="exportMarkdown">导出 Markdown</el-button>
          </div>
        </div>
        
        <div class="section">
          <h3>导入数据</h3>
          <p class="section-desc">从 JSON 文件导入对话记录</p>
          <el-upload
            action=""
            :auto-upload="false"
            :show-file-list="false"
            accept=".json"
            @change="handleImport"
          >
            <el-button>选择文件导入</el-button>
          </el-upload>
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <!-- Add/Edit Provider Dialog -->
    <el-dialog
      v-model="providerDialogVisible"
      :title="editingProvider ? '编辑服务商' : '添加服务商'"
      width="600px"
    >
      <el-form :model="providerForm" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="providerForm.name" placeholder="例如: OpenAI, 英伟达" />
        </el-form-item>
        <el-form-item label="API Key" required>
          <el-input v-model="providerForm.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="Base URL" required>
          <el-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com" />
          <div class="form-tip">输入 API 地址后点击"获取模型"自动获取可用模型列表</div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            :loading="fetchingModels"
            @click="fetchAvailableModels"
          >
            获取模型列表
          </el-button>
        </el-form-item>
        
        <!-- 模型列表选择 -->
        <el-form-item v-if="availableModels.length > 0" label="选择模型">
          <div class="available-models">
            <el-checkbox-group v-model="selectedModels">
              <el-checkbox 
                v-for="model in availableModels" 
                :key="model.id" 
                :label="model.id"
              >
                {{ model.name }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="model-actions">
            <el-button text size="small" @click="selectAllModels">全选</el-button>
            <el-button text size="small" @click="selectedModels = []">清空</el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="默认模型">
          <el-select v-model="providerForm.modelName" placeholder="选择默认使用的模型" filterable>
            <el-option 
              v-for="model in selectedModels" 
              :key="model" 
              :label="model" 
              :value="model" 
            />
          </el-select>
        </el-form-item>
        
        <el-divider>高级设置</el-divider>
        
        <el-form-item label="Temperature">
          <el-slider v-model="providerForm.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="providerForm.maxTokens" :min="100" :max="128000" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="providerForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="providerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProvider">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useModelStore } from '@/stores/model'
import { modelApi, dataApi } from '@/api'
import type { ModelProvider, ModelInfo } from '@/types'
import { 
  Plus, 
  CircleCheck, 
  Connection, 
  Refresh, 
  Edit, 
  Delete,
  Tools,
  View,
  WarningFilled,
  CircleClose
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const modelStore = useModelStore()

const activeTab = ref('models')
const providerDialogVisible = ref(false)
const editingProvider = ref<ModelProvider | null>(null)
const fetchingModels = ref(false)
const availableModels = ref<ModelInfo[]>([])
const selectedModels = ref<string[]>([])
const selectedProviderId = ref<string | null>(null)
const testingConnection = ref<Record<string, boolean>>({})
const testingModel = ref<Record<string, boolean>>({})

const providerForm = reactive({
  name: '',
  type: 'openai',
  apiKey: '',
  baseUrl: '',
  modelName: '',
  temperature: 0.7,
  maxTokens: 2048,
  isActive: true
})

// 选中的服务商
const selectedProvider = computed(() => {
  if (!selectedProviderId.value) return null
  return modelStore.providers.find(p => p.id === selectedProviderId.value)
})

// 分组的模型
const groupedModels = computed(() => {
  if (!selectedProvider.value?.modelInfos) return {}
  
  const groups: Record<string, ModelInfo[]> = {}
  selectedProvider.value.modelInfos.forEach(model => {
    const group = model.group || 'other'
    if (!groups[group]) {
      groups[group] = []
    }
    groups[group].push(model)
  })
  
  // 排序分组 - 添加更多常见的分组
  const sortedGroups: Record<string, ModelInfo[]> = {}
  const groupOrder = ['deepseek', 'gpt-4', 'gpt-3.5', 'openai', 'claude', 'qwen', 'gemini', 'llama', 'mistral', 'nvidia', 'embedding', 'other']
  groupOrder.forEach(key => {
    if (groups[key]) {
      sortedGroups[key] = groups[key]
    }
  })
  
  // 添加不在groupOrder中的分组（按字母顺序）
  Object.keys(groups).sort().forEach(key => {
    if (!sortedGroups[key]) {
      sortedGroups[key] = groups[key]
    }
  })
  
  return sortedGroups
})

const selectProvider = (id: string) => {
  selectedProviderId.value = id
}

const getGroupDisplayName = (group: string): string => {
  const names: Record<string, string> = {
    'deepseek': 'DeepSeek',
    'gpt-4': 'GPT-4',
    'gpt-3.5': 'GPT-3.5',
    'claude': 'Claude',
    'qwen': 'Qwen',
    'gemini': 'Gemini',
    'llama': 'LLaMA',
    'mistral': 'Mistral',
    'nvidia': 'NVIDIA',
    'embedding': '嵌入模型',
    'whisper': 'Whisper',
    'image': '图像生成',
    'speech': '语音合成',
    'other': '其他'
  }
  return names[group] || group
}

const formatContextLength = (length: number): string => {
  if (length >= 1000000) {
    return `${(length / 1000000).toFixed(1)}M`
  }
  if (length >= 1000) {
    return `${(length / 1000).toFixed(0)}K`
  }
  return `${length}`
}

const testConnection = async (id: string) => {
  testingConnection.value[id] = true
  try {
    const result = await modelApi.testConnection(id)
    if (result.success) {
      const provider = modelStore.providers.find(p => p.id === id)
      ElMessage.success({
        message: `${provider?.name || '服务商'} 连接成功！\n延迟: ${result.latency}ms | 可用模型: ${result.modelCount} 个`,
        duration: 4000,
        showClose: true,
        dangerouslyUseHTMLString: true
      })
    } else {
      ElMessage.error({
        message: `连接失败: ${result.message || '未知错误'}`,
        duration: 4000,
        showClose: true
      })
    }
  } catch (error) {
    ElMessage.error('连接测试失败，请检查网络或配置')
  } finally {
    testingConnection.value[id] = false
  }
}

const testSingleModel = async (providerId: string, modelId: string) => {
  testingModel.value[modelId] = true
  try {
    const result = await modelApi.testSingleModel(providerId, modelId)
    if (result.success) {
      ElMessage.success({
        message: `模型 ${modelId} 测试成功！\n延迟: ${result.latency}ms`,
        duration: 3000,
        showClose: true
      })
      // 测试成功后刷新模型列表，以更新失败标记
      await modelStore.fetchProviders()
    } else {
      ElMessage.error({
        message: `模型测试失败: ${result.message || '未知错误'}`,
        duration: 4000,
        showClose: true
      })
    }
  } catch (error: any) {
    ElMessage.error({
      message: `模型测试失败: ${error.message || '未知错误'}`,
      duration: 4000,
      showClose: true
    })
  } finally {
    testingModel.value[modelId] = false
  }
}

const clearFailedMark = async (providerId: string, modelId: string) => {
  try {
    await modelApi.clearModelFailedMark(providerId, modelId)
    ElMessage.success('已清除失败标记')
    // 刷新模型列表
    await modelStore.fetchProviders()
  } catch (error) {
    ElMessage.error('清除失败标记失败')
  }
}

const resetForm = () => {
  providerForm.name = ''
  providerForm.type = 'openai'
  providerForm.apiKey = ''
  providerForm.baseUrl = ''
  providerForm.modelName = ''
  providerForm.temperature = 0.7
  providerForm.maxTokens = 2048
  providerForm.isActive = true
  availableModels.value = []
  selectedModels.value = []
}

const showAddProvider = () => {
  editingProvider.value = null
  resetForm()
  providerDialogVisible.value = true
}

const editProvider = (provider: ModelProvider) => {
  editingProvider.value = provider
  Object.assign(providerForm, {
    name: provider.name,
    type: provider.type,
    apiKey: '',
    baseUrl: provider.baseUrl || '',
    modelName: provider.modelName || '',
    temperature: provider.temperature || 0.7,
    maxTokens: provider.maxTokens || 2048,
    isActive: provider.isActive
  })
  // 设置已有的模型列表
  if (provider.modelInfos) {
    availableModels.value = provider.modelInfos
    selectedModels.value = provider.modelInfos.map(m => m.id)
  }
  providerDialogVisible.value = true
}

const fetchAvailableModels = async () => {
  if (!providerForm.baseUrl || !providerForm.apiKey) {
    ElMessage.warning('请先填写 Base URL 和 API Key')
    return
  }
  
  fetchingModels.value = true
  try {
    const result = await modelApi.fetchModels(providerForm.baseUrl, providerForm.apiKey)
    if (result.success && result.models) {
      availableModels.value = result.models
      selectedModels.value = result.models.map(m => m.id)
      ElMessage.success(`获取到 ${result.models.length} 个模型`)
    } else {
      ElMessage.error(result.message || '获取模型列表失败')
    }
  } catch (error) {
    ElMessage.error('获取模型列表失败')
  } finally {
    fetchingModels.value = false
  }
}

const selectAllModels = () => {
  selectedModels.value = availableModels.value.map(m => m.id)
}

const saveProvider = async () => {
  if (!providerForm.name || !providerForm.apiKey || !providerForm.baseUrl) {
    ElMessage.warning('请填写名称、API Key 和 Base URL')
    return
  }
  
  try {
    // 获取选中的完整模型信息
    const selectedModelInfos = availableModels.value.filter(m => 
      selectedModels.value.includes(m.id)
    )
    
    const data = {
      ...providerForm,
      models: selectedModels.value,  // 保留模型ID列表（兼容）
      modelInfos: selectedModelInfos  // 传递完整的模型信息
    }
    
    if (editingProvider.value) {
      await modelStore.updateProvider(editingProvider.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await modelStore.createProvider(data)
      ElMessage.success('添加成功')
    }
    providerDialogVisible.value = false
    await modelStore.fetchProviders()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteProvider = async (id: string) => {
  await ElMessageBox.confirm('确定要删除这个服务商配置吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await modelStore.deleteProvider(id)
  ElMessage.success('删除成功')
  // 如果删除的是当前选中的，清空选择
  if (selectedProviderId.value === id) {
    selectedProviderId.value = modelStore.providers.length > 0 ? modelStore.providers[0].id : null
  }
}

const refreshModels = async (id: string) => {
  try {
    await modelApi.refreshModels(id)
    await modelStore.fetchProviders()
    ElMessage.success('模型列表已刷新')
  } catch (error) {
    ElMessage.error('刷新失败')
  }
}

const selectModel = async (providerId: string, modelName: string) => {
  try {
    await modelStore.updateProvider(providerId, { modelName })
    const provider = modelStore.providers.find(p => p.id === providerId)
    ElMessage.success({
      message: `已选择模型: ${modelName}`,
      duration: 3000,
      showClose: true
    })
    // 刷新数据以更新UI
    await modelStore.fetchProviders()
  } catch (error) {
    ElMessage.error('选择模型失败')
  }
}

const exportJson = async () => {
  try {
    const blob = await dataApi.exportJson()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ai-assistant-export.json'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const exportMarkdown = async () => {
  try {
    const blob = await dataApi.exportMarkdown()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ai-assistant-export.md'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const handleImport = async (file: any) => {
  try {
    const content = await file.raw.text()
    await dataApi.import(content)
    ElMessage.success('导入成功')
  } catch (error) {
    ElMessage.error('导入失败')
  }
}

onMounted(async () => {
  await modelStore.fetchProviders()
  // 默认选中第一个服务商
  if (modelStore.providers.length > 0) {
    selectedProviderId.value = modelStore.providers[0].id
  }
})
</script>

<style lang="scss" scoped>
.settings-view {
  padding: 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.settings-header {
  margin-bottom: 24px;
  flex-shrink: 0;
  
  h2 {
    font-size: 24px;
    font-weight: 600;
  }
}

:deep(.el-tabs) {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

:deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
}

:deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
}

.models-layout {
  display: flex;
  gap: 16px;
  height: 100%;
  overflow: hidden;
}

.providers-sidebar {
  width: 240px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  
  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #e4e7ed;
    
    h3 {
      font-size: 16px;
      font-weight: 500;
      margin: 0;
    }
  }
  
  .providers-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }
  
  .provider-item {
    padding: 12px;
    border-radius: 6px;
    cursor: pointer;
    margin-bottom: 4px;
    transition: all 0.2s;
    
    &:hover {
      background: #f5f7fa;
    }
    
    &.active {
      background: #ecf5ff;
      border: 1px solid #409eff;
    }
    
    .provider-item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }
    
    .provider-item-name {
      font-weight: 500;
      font-size: 14px;
    }
    
    .provider-item-info {
      font-size: 12px;
      color: #909399;
    }
  }
}

.models-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 16px;
  height: 100%;
}

.provider-detail {
  .detail-header {
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
    
    .detail-title {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;
      
      h3 {
        font-size: 20px;
        font-weight: 600;
        margin: 0;
      }
    }
    
    .detail-actions {
      display: flex;
      gap: 8px;
    }
  }
  
  .detail-info {
    margin-bottom: 24px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 6px;
    
    .info-item {
      display: flex;
      margin-bottom: 8px;
      font-size: 14px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .info-label {
        width: 100px;
        color: #606266;
        font-weight: 500;
      }
      
      .info-value {
        flex: 1;
        color: #303133;
      }
    }
  }
}

.models-groups {
  .model-group {
    margin-bottom: 24px;
    
    .group-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 4px;
      margin-bottom: 8px;
      
      .group-name {
        font-weight: 500;
        font-size: 14px;
        color: #303133;
      }
      
      .group-count {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .group-models {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .model-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      transition: all 0.2s;
      gap: 12px;
      
      &:hover {
        background: #f5f7fa;
        border-color: #c6e2ff;
        
        .model-item-actions {
          opacity: 1;
        }
      }
      
      &.selected {
        background: #ecf5ff;
        border-color: #409eff;
      }
      
      .model-item-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex: 1;
        cursor: pointer;
      }
      
      .model-item-main {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
      }
      
      .model-name {
        font-size: 14px;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 4px;
        
        &.model-failed {
          color: #f56c6c;
          opacity: 0.7;
        }
      }
      
      .model-badges {
        display: flex;
        align-items: center;
        gap: 6px;
      }
      
      .model-context {
        font-size: 12px;
        color: #909399;
        padding: 2px 8px;
        background: #f0f2f5;
        border-radius: 3px;
      }
      
      .model-item-actions {
        opacity: 0;
        transition: opacity 0.2s;
        display: flex;
        gap: 4px;
      }
    }
  }
}

.section {
  margin-bottom: 32px;
  
  h3 {
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 8px;
  }
}

.section-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
}

.button-group {
  display: flex;
  gap: 12px;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.available-models {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 12px;
  
  .el-checkbox {
    display: block;
    margin: 4px 0;
  }
}

.model-actions {
  margin-top: 8px;
}
</style>
