<template>
  <div class="llm-node-config">
    <el-form-item label="模型">
      <el-select 
        v-model="localConfig.modelId" 
        placeholder="选择模型" 
        style="width: 100%" 
        filterable
        @change="emitUpdate"
      >
        <el-option-group
          v-for="provider in activeProviders"
          :key="provider.id"
          :label="provider.name"
        >
          <el-option
            v-for="model in getProviderModels(provider)"
            :key="model.id"
            :label="model.name"
            :value="`${provider.id}:${model.id}`"
          >
            <div class="model-option">
              <span>{{ model.name }}</span>
              <span class="model-tags">
                <el-tag v-if="model.supportVision" size="small" type="success">视觉</el-tag>
                <el-tag v-if="model.supportTools" size="small" type="warning">工具</el-tag>
              </span>
            </div>
          </el-option>
        </el-option-group>
      </el-select>
    </el-form-item>

    <!-- AI 提示词助手入口 -->
    <div class="ai-assistant-entry">
      <el-button 
        type="primary" 
        plain 
        class="ai-generate-btn"
        @click="showPromptGenerator = true"
      >
        <el-icon><MagicStick /></el-icon>
        AI 一键生成提示词
      </el-button>
      <span class="ai-hint">不知道怎么写？让 AI 帮你生成</span>
    </div>

    <el-form-item label="系统提示词">
      <el-input
        v-model="localConfig.systemPrompt"
        type="textarea"
        :rows="3"
        placeholder="定义AI助手的角色和行为规则"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="用户提示词模板">
      <el-input
        v-model="localConfig.userPromptTemplate"
        type="textarea"
        :rows="6"
        placeholder="使用 {{变量名}} 引用上游节点的输出变量"
        @change="emitUpdate"
      />
      <div class="variable-hint">
        <el-icon><InfoFilled /></el-icon>
        <div class="hint-content">
          <div>可用变量引用示例：</div>
          <ul>
            <li><code>{{query}}</code> - 用户问题</li>
            <li><code>{{context}}</code> - 知识库检索的上下文</li>
            <li><code>{{results}}</code> - 知识库检索结果数组</li>
          </ul>
          <div class="example">
            示例模板：<br/>
            <code>根据以下参考资料回答问题：<br/>
参考资料：{{context}}<br/>
问题：{{query}}</code>
          </div>
        </div>
      </div>
    </el-form-item>

    <el-form-item label="温度">
      <el-slider
        v-model="localConfig.temperature"
        :min="0"
        :max="2"
        :step="0.1"
        show-input
        @change="emitUpdate"
      />
      <div class="param-hint">值越低回答越确定，值越高回答越有创意</div>
    </el-form-item>

    <el-form-item label="最大令牌数">
      <el-input-number
        v-model="localConfig.maxTokens"
        :min="1"
        :max="32000"
        :step="100"
        style="width: 100%"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="视觉模式">
      <el-switch
        v-model="localConfig.visionEnabled"
        active-text="启用"
        inactive-text="禁用"
        @change="emitUpdate"
      />
      <div v-if="localConfig.visionEnabled" class="vision-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>视觉模式允许模型处理图像输入</span>
      </div>
    </el-form-item>

    <!-- AI 提示词生成对话框 -->
    <PromptGeneratorDialog
      v-model="showPromptGenerator"
      :available-variables="availableVariables"
      :current-system-prompt="localConfig.systemPrompt"
      :current-user-prompt="localConfig.userPromptTemplate"
      @apply="handleApplyPrompt"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { InfoFilled, MagicStick } from '@element-plus/icons-vue'
import type { LLMNodeConfig } from '@/types/workflow'
import type { ModelProvider } from '@/types'
import { useModelStore } from '@/stores/model'
import PromptGeneratorDialog from './PromptGeneratorDialog.vue'

interface Props {
  config: LLMNodeConfig
}

const props = defineProps<Props>()

const emit = defineEmits<{
  update: [config: LLMNodeConfig]
}>()

const modelStore = useModelStore()
const localConfig = ref<LLMNodeConfig>({ ...props.config })
const showPromptGenerator = ref(false)

// 可用的上游变量（实际项目中应从工作流上下文获取）
const availableVariables = ref(['query', 'context', 'results'])

// 获取活跃的模型提供商
const activeProviders = computed(() => modelStore.activeProviders)

// 获取提供商的模型列表
const getProviderModels = (provider: ModelProvider) => {
  if (provider.modelInfos && provider.modelInfos.length > 0) {
    return provider.modelInfos.filter(m => !m.isFailed)
  }
  // 如果没有 modelInfos，使用 models 数组
  if (provider.models && provider.models.length > 0) {
    return provider.models.map(m => ({ id: m, name: m }))
  }
  return []
}

watch(
  () => props.config,
  (newConfig) => {
    localConfig.value = { ...newConfig }
  },
  { deep: true }
)

const emitUpdate = () => {
  emit('update', localConfig.value)
}

const handleApplyPrompt = (systemPrompt: string, userPrompt: string) => {
  localConfig.value.systemPrompt = systemPrompt
  localConfig.value.userPromptTemplate = userPrompt
  emitUpdate()
}

// 加载模型列表
onMounted(async () => {
  if (modelStore.providers.length === 0) {
    await modelStore.fetchProviders()
  }
})
</script>

<style scoped>
.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.model-tags {
  display: flex;
  gap: 4px;
}

.model-tags .el-tag {
  font-size: 10px;
  padding: 0 4px;
  height: 18px;
  line-height: 16px;
}

.ai-assistant-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4fd 100%);
  border-radius: 8px;
  border: 1px dashed #c6e2ff;
}

.ai-generate-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  font-weight: 500;
}

.ai-generate-btn:hover {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
  color: white;
}

.ai-hint {
  font-size: 12px;
  color: #909399;
}

.variable-hint {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 6px;
  font-size: 12px;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.variable-hint .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.hint-content {
  flex: 1;
}

.hint-content ul {
  margin: 6px 0;
  padding-left: 16px;
}

.hint-content li {
  margin: 4px 0;
}

.hint-content code {
  background: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', monospace;
  color: #409eff;
}

.example {
  margin-top: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  line-height: 1.6;
}

.example code {
  display: block;
  white-space: pre-wrap;
  background: transparent;
  padding: 0;
}

.param-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.vision-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px;
  background: #e6f0ff;
  border-radius: 4px;
  font-size: 12px;
  color: #409eff;
}
</style>
