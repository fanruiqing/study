<template>
  <div class="end-node-config">
    <el-form-item label="输出映射 (JSON)">
      <el-input
        v-model="localConfig.outputMapping"
        type="textarea"
        :rows="6"
        placeholder='{"result": "{{llm.response}}", "metadata": "{{kb.sources}}"}'
        @change="emitUpdate"
      />
    </el-form-item>
    <div class="hint">
      <el-icon><InfoFilled /></el-icon>
      <span>使用 {{节点ID.输出变量}} 引用上游节点的输出</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'
import type { EndNodeConfig } from '@/types/workflow'

interface Props {
  config: EndNodeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: EndNodeConfig]
}>()

const localConfig = ref<EndNodeConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>

<style scoped>
.hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: #e6f0ff;
  border-radius: 4px;
  font-size: 12px;
  color: #409eff;
  margin-top: 8px;
}
</style>
