<template>
  <div class="kb-node-config">
    <el-form-item label="知识库">
      <el-select
        v-model="localConfig.knowledgeBaseId"
        placeholder="选择知识库"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb.id"
          :label="kb.name"
          :value="kb.id"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="查询变量">
      <el-input
        v-model="localConfig.queryVariable"
        placeholder="输入查询变量名，如: query"
        @change="emitUpdate"
      />
      <div class="hint">使用 <code>{{query}}</code> 引用上游节点的输出</div>
    </el-form-item>

    <el-form-item label="检索数量 (Top K)">
      <el-input-number
        v-model="localConfig.topK"
        :min="1"
        :max="20"
        style="width: 100%"
        @change="emitUpdate"
      />
      <div class="hint">返回最相关的前 K 条结果</div>
    </el-form-item>

    <el-form-item label="相似度阈值">
      <el-slider
        v-model="localConfig.similarityThreshold"
        :min="0"
        :max="1"
        :step="0.05"
        show-input
        @change="emitUpdate"
      />
      <div class="hint">只返回相似度高于此阈值的结果</div>
    </el-form-item>

    <el-form-item label="重排序">
      <el-switch
        v-model="localConfig.reranking"
        active-text="启用"
        inactive-text="禁用"
        @change="emitUpdate"
      />
      <div class="hint">使用重排序模型优化检索结果排序</div>
    </el-form-item>

    <div class="output-info">
      <div class="output-title">
        <el-icon><InfoFilled /></el-icon>
        输出变量说明
      </div>
      <div class="output-list">
        <div class="output-item">
          <code>results</code>
          <span>检索结果数组，包含文档片段和相似度分数</span>
        </div>
        <div class="output-item">
          <code>context</code>
          <span>拼接后的上下文文本，可直接用于LLM提示词</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import type { KnowledgeBaseNodeConfig } from '@/types/workflow'
import api from '@/api'

interface Props {
  config: KnowledgeBaseNodeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: KnowledgeBaseNodeConfig]
}>()

const localConfig = ref<KnowledgeBaseNodeConfig>({ ...props.config })
const knowledgeBases = ref<Array<{ id: string; name: string }>>([])

watch(
  () => props.config,
  (newConfig) => {
    localConfig.value = { ...newConfig }
  },
  { deep: true }
)

const loadKnowledgeBases = async () => {
  try {
    const response = await api.get('/knowledge-bases')
    knowledgeBases.value = response.data
  } catch (error) {
    console.error('加载知识库失败:', error)
  }
}

const emitUpdate = () => {
  emit('update', localConfig.value)
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.hint code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', monospace;
  color: #409eff;
}

.output-info {
  margin-top: 16px;
  padding: 12px;
  background: #fdf6ec;
  border-radius: 6px;
  border: 1px solid #faecd8;
}

.output-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 10px;
}

.output-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.output-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
}

.output-item code {
  background: #fff;
  padding: 2px 8px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', monospace;
  color: #e6a23c;
  flex-shrink: 0;
}

.output-item span {
  color: #606266;
  line-height: 1.5;
}
</style>
