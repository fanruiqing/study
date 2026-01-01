<template>
  <div class="condition-config">
    <el-form-item label="条件规则">
      <div class="condition-list">
        <div
          v-for="(condition, index) in localConfig.conditions"
          :key="condition.id"
          class="condition-item"
        >
          <div class="condition-header">
            <el-input
              v-model="condition.name"
              placeholder="条件名称"
              size="small"
              @change="emitUpdate"
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              @click="removeCondition(index)"
            />
          </div>
          <el-input
            v-model="condition.expression"
            type="textarea"
            :rows="2"
            placeholder="例如: {{input.score}} > 80"
            size="small"
            @change="emitUpdate"
          />
        </div>
      </div>
      <el-button
        type="primary"
        size="small"
        @click="addCondition"
        style="width: 100%; margin-top: 8px"
      >
        + 添加条件
      </el-button>
    </el-form-item>

    <el-form-item label="默认分支">
      <el-input
        v-model="localConfig.defaultBranch"
        placeholder="默认分支名称"
        @change="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import type { ConditionNodeConfig } from '@/types/workflow'

interface Props {
  config: ConditionNodeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: ConditionNodeConfig]
}>()

const localConfig = ref<ConditionNodeConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const addCondition = () => {
  if (!localConfig.value.conditions) {
    localConfig.value.conditions = []
  }
  localConfig.value.conditions.push({
    id: `cond_${Date.now()}`,
    name: '',
    expression: ''
  })
  emitUpdate()
}

const removeCondition = (index: number) => {
  localConfig.value.conditions?.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>

<style scoped>
.condition-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.condition-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.condition-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.condition-header .el-input {
  flex: 1;
}
</style>
