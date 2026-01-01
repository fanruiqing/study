<template>
  <div class="code-node-config">
    <el-form-item label="编程语言">
      <el-select
        v-model="localConfig.language"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option label="JavaScript" value="javascript" />
        <el-option label="Python" value="python" />
      </el-select>
    </el-form-item>

    <el-form-item label="输入变量">
      <el-select
        v-model="localConfig.inputVariables"
        multiple
        placeholder="选择输入变量"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option
          v-for="variable in availableVariables"
          :key="variable"
          :label="variable"
          :value="variable"
        />
      </el-select>
    </el-form-item>

    <el-form-item label="代码">
      <el-input
        v-model="localConfig.code"
        type="textarea"
        :rows="10"
        placeholder="输入代码..."
        @change="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { CodeNodeConfig } from '@/types/workflow'

interface Props {
  config: CodeNodeConfig
  availableVariables?: string[]
}

const props = withDefaults(defineProps<Props>(), {
  availableVariables: () => []
})

const emit = defineEmits<{
  update: [config: CodeNodeConfig]
}>()

const localConfig = ref<CodeNodeConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>
