<template>
  <div class="start-node-config">
    <el-form-item label="输入变量">
      <div class="variable-list">
        <div
          v-for="(field, index) in localConfig.fields"
          :key="index"
          class="variable-item"
        >
          <el-input
            v-model="field.name"
            placeholder="变量名"
            size="small"
            @change="emitUpdate"
          />
          <el-select
            v-model="field.type"
            placeholder="类型"
            size="small"
            style="width: 100px"
            @change="emitUpdate"
          >
            <el-option label="文本" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
            <el-option label="文件" value="file" />
            <el-option label="数组" value="array" />
            <el-option label="对象" value="object" />
          </el-select>
          <el-input
            v-model="field.description"
            placeholder="描述"
            size="small"
            @change="emitUpdate"
          />
          <el-button
            type="danger"
            :icon="Delete"
            circle
            size="small"
            @click="removeField(index)"
          />
        </div>
      </div>
      <el-button type="primary" size="small" @click="addField" style="width: 100%; margin-top: 8px">
        + 添加变量
      </el-button>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import type { StartNodeConfig } from '@/types/workflow'

interface Props {
  config: StartNodeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: StartNodeConfig]
}>()

const localConfig = ref<StartNodeConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const addField = () => {
  if (!localConfig.value.fields) {
    localConfig.value.fields = []
  }
  localConfig.value.fields.push({
    name: '',
    type: 'string',
    description: '',
    required: true
  })
  emitUpdate()
}

const removeField = (index: number) => {
  localConfig.value.fields?.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>

<style scoped>
.variable-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.variable-item {
  display: flex;
  gap: 6px;
  align-items: center;
}

.variable-item .el-input {
  flex: 1;
}
</style>
