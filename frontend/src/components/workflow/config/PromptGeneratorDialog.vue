<template>
  <el-dialog
    v-model="visible"
    title="✨ AI 提示词助手"
    width="560px"
    :close-on-click-modal="false"
    class="prompt-generator-dialog"
  >
    <div class="generator-content">
      <!-- 步骤1: 描述需求 -->
      <div v-if="step === 1" class="step-content">
        <div class="step-header">
          <span class="step-number">1</span>
          <span class="step-title">描述你想要AI做什么</span>
        </div>
        
        <el-input
          v-model="userDescription"
          type="textarea"
          :rows="4"
          placeholder="例如：帮我总结用户上传的文档内容，提取关键信息并生成摘要"
          class="description-input"
        />
        
        <div class="quick-templates">
          <div class="templates-label">快速选择场景：</div>
          <div class="template-tags">
            <el-tag
              v-for="template in quickTemplates"
              :key="template.id"
              class="template-tag"
              effect="plain"
              @click="selectTemplate(template)"
            >
              {{ template.icon }} {{ template.name }}
            </el-tag>
          </div>
        </div>

        <div class="context-info">
          <el-icon><Connection /></el-icon>
          <span>将根据上游节点的输出变量自动生成变量引用</span>
        </div>
      </div>

      <!-- 步骤2: 预览结果 -->
      <div v-if="step === 2" class="step-content">
        <div class="step-header">
          <span class="step-number">2</span>
          <span class="step-title">预览生成的提示词</span>
        </div>

        <div v-if="generating" class="generating-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>AI 正在生成提示词...</span>
        </div>

        <div v-else class="preview-section">
          <div class="preview-block">
            <div class="preview-label">
              <el-icon><User /></el-icon>
              系统提示词
            </div>
            <el-input
              v-model="generatedSystemPrompt"
              type="textarea"
              :rows="3"
              class="preview-textarea"
            />
          </div>

          <div class="preview-block">
            <div class="preview-label">
              <el-icon><ChatDotRound /></el-icon>
              用户提示词模板
            </div>
            <el-input
              v-model="generatedUserPrompt"
              type="textarea"
              :rows="5"
              class="preview-textarea"
            />
          </div>

          <div class="regenerate-hint">
            <el-button link type="primary" @click="regenerate">
              <el-icon><Refresh /></el-icon>
              不满意？重新生成
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="step === 2" @click="step = 1">
          <el-icon><Back /></el-icon>
          返回修改
        </el-button>
        <el-button
          v-if="step === 1"
          type="primary"
          :disabled="!userDescription.trim()"
          @click="generatePrompt"
        >
          <el-icon><MagicStick /></el-icon>
          生成提示词
        </el-button>
        <el-button
          v-if="step === 2 && !generating"
          type="primary"
          @click="applyPrompt"
        >
          <el-icon><Check /></el-icon>
          应用到配置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { 
  MagicStick, Check, Back, Refresh, Loading, 
  Connection, User, ChatDotRound 
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

interface Props {
  modelValue: boolean
  availableVariables?: string[]
  currentSystemPrompt?: string
  currentUserPrompt?: string
}

interface QuickTemplate {
  id: string
  name: string
  icon: string
  description: string
}

const props = withDefaults(defineProps<Props>(), {
  availableVariables: () => [],
  currentSystemPrompt: '',
  currentUserPrompt: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'apply': [systemPrompt: string, userPrompt: string]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const step = ref(1)
const userDescription = ref('')
const generating = ref(false)
const generatedSystemPrompt = ref('')
const generatedUserPrompt = ref('')

const quickTemplates: QuickTemplate[] = [
  { id: 'summarize', name: '内容总结', icon: '📝', description: '总结文档或对话内容' },
  { id: 'qa', name: '问答助手', icon: '💬', description: '基于知识库回答问题' },
  { id: 'translate', name: '翻译助手', icon: '🌐', description: '多语言翻译' },
  { id: 'analyze', name: '数据分析', icon: '📊', description: '分析数据并给出洞察' },
  { id: 'creative', name: '创意写作', icon: '✍️', description: '生成创意内容' },
  { id: 'code', name: '代码助手', icon: '💻', description: '代码生成与解释' }
]

const selectTemplate = (template: QuickTemplate) => {
  const descriptions: Record<string, string> = {
    summarize: '帮我总结输入的内容，提取关键信息，生成简洁的摘要',
    qa: '作为智能问答助手，根据提供的知识库内容准确回答用户问题',
    translate: '将输入的内容翻译成目标语言，保持原意的同时使译文流畅自然',
    analyze: '分析输入的数据，找出关键趋势和洞察，给出专业的分析报告',
    creative: '根据用户的要求进行创意写作，生成有吸引力的内容',
    code: '作为编程助手，帮助用户编写、解释或优化代码'
  }
  userDescription.value = descriptions[template.id] || template.description
}

const generatePrompt = async () => {
  step.value = 2
  generating.value = true

  try {
    // 模拟AI生成（实际项目中应调用后端API）
    await simulateGeneration()
  } catch (error) {
    ElMessage.error('生成失败，请重试')
    step.value = 1
  } finally {
    generating.value = false
  }
}

const simulateGeneration = async () => {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1500))

  const variables = props.availableVariables
  const hasQuery = variables.includes('query')
  const hasContext = variables.includes('context')
  const hasResults = variables.includes('results')

  // 根据用户描述和可用变量智能生成提示词
  const desc = userDescription.value.toLowerCase()
  
  if (desc.includes('总结') || desc.includes('摘要')) {
    generatedSystemPrompt.value = '你是一个专业的内容分析师，擅长提取关键信息并生成简洁准确的摘要。请保持客观中立，突出重点内容。'
    generatedUserPrompt.value = hasContext 
      ? '请仔细阅读以下内容，提取关键信息并生成摘要：\n\n{{context}}\n\n' + (hasQuery ? '用户特别关注：{{query}}' : '')
      : '请总结以下内容：\n\n{{query}}'
  } else if (desc.includes('问答') || desc.includes('回答')) {
    generatedSystemPrompt.value = '你是一个知识渊博的AI助手。请基于提供的参考资料准确回答问题。如果资料中没有相关信息，请诚实说明。回答要简洁明了，条理清晰。'
    generatedUserPrompt.value = hasContext
      ? '参考资料：\n{{context}}\n\n用户问题：{{query}}\n\n请根据参考资料回答上述问题。'
      : '问题：{{query}}\n\n请回答上述问题。'
  } else if (desc.includes('翻译')) {
    generatedSystemPrompt.value = '你是一个专业的翻译专家，精通多种语言。翻译时请保持原文的语气和风格，确保译文流畅自然、准确达意。'
    generatedUserPrompt.value = '请将以下内容翻译成目标语言：\n\n{{query}}'
  } else if (desc.includes('分析') || desc.includes('数据')) {
    generatedSystemPrompt.value = '你是一个资深的数据分析师，擅长从数据中发现规律和洞察。请提供专业、客观的分析，并给出可行的建议。'
    generatedUserPrompt.value = hasContext
      ? '请分析以下数据：\n\n{{context}}\n\n' + (hasQuery ? '分析重点：{{query}}' : '请给出关键发现和建议。')
      : '请分析以下内容并给出洞察：\n\n{{query}}'
  } else if (desc.includes('创意') || desc.includes('写作')) {
    generatedSystemPrompt.value = '你是一个富有创意的写作助手，擅长各种文体的创作。请发挥想象力，创作出有吸引力、有感染力的内容。'
    generatedUserPrompt.value = '创作要求：{{query}}\n\n请根据要求进行创作。'
  } else if (desc.includes('代码') || desc.includes('编程')) {
    generatedSystemPrompt.value = '你是一个经验丰富的软件工程师，精通多种编程语言和技术栈。请提供清晰、高效、可维护的代码，并附上必要的注释和说明。'
    generatedUserPrompt.value = '编程需求：{{query}}\n\n请提供代码实现和解释。'
  } else {
    // 通用模板
    generatedSystemPrompt.value = '你是一个专业、友好的AI助手。请根据用户的需求提供准确、有帮助的回答。'
    generatedUserPrompt.value = hasContext
      ? '背景信息：\n{{context}}\n\n用户请求：{{query}}\n\n请根据以上信息完成任务。'
      : '{{query}}'
  }
}

const regenerate = () => {
  generatePrompt()
}

const applyPrompt = () => {
  emit('apply', generatedSystemPrompt.value, generatedUserPrompt.value)
  ElMessage.success('提示词已应用')
  handleClose()
}

const handleClose = () => {
  visible.value = false
  // 重置状态
  setTimeout(() => {
    step.value = 1
    userDescription.value = ''
    generatedSystemPrompt.value = ''
    generatedUserPrompt.value = ''
  }, 300)
}

watch(visible, (val) => {
  if (val) {
    step.value = 1
  }
})
</script>

<style scoped>
.prompt-generator-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 16px;
}

.prompt-generator-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

.generator-content {
  min-height: 300px;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.step-number {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.step-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.description-input {
  margin-bottom: 16px;
}

.description-input :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.6;
}

.quick-templates {
  margin-bottom: 16px;
}

.templates-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.template-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-tag {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 16px;
  padding: 6px 12px;
}

.template-tag:hover {
  background: #ecf5ff;
  border-color: #409eff;
  color: #409eff;
}

.context-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: #f4f4f5;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
}

.generating-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #409eff;
}

.generating-state .el-icon {
  font-size: 32px;
  margin-bottom: 12px;
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-block {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
}

.preview-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}

.preview-textarea :deep(.el-textarea__inner) {
  background: white;
  font-size: 13px;
  line-height: 1.6;
}

.regenerate-hint {
  text-align: center;
  margin-top: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
