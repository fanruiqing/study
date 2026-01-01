<template>
  <div class="classifier-config">
    <el-form-item label="模型">
      <el-select
        v-model="localConfig.modelId"
        placeholder="选择模型"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option label="GPT-4" value="gpt-4" />
        <el-option label="GPT-3.5 Turbo" value="gpt-3.5-turbo" />
        <el-option label="Claude 3" value="claude-3" />
      </el-select>
    </el-form-item>

    <el-form-item label="输入变量">
      <el-input
        v-model="localConfig.inputVariable"
        placeholder="输入变量名"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="分类指令">
      <el-input
        v-model="localConfig.instruction"
        type="textarea"
        :rows="3"
        placeholder="输入分类指令（可选）"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="分类类别">
      <div class="class-list">
        <div
          v-for="(cls, index) in localConfig.classes"
          :key="cls.id"
          class="class-item"
        >
          <div class="class-header">
            <span class="class-number">{{ index + 1 }}</span>
            <el-input
              v-model="cls.name"
              placeholder="类别名称"
              size="small"
              @change="emitUpdate"
            />
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              @click="removeClass(index)"
            />
          </div>
          <el-input
            v-model="cls.description"
            placeholder="类别描述"
            size="small"
            @change="emitUpdate"
          />
        </div>
      </div>
      <el-button
        type="primary"
        size="small"
        @click="addClass"
        style="width: 100%; margin-top: 8px"
      >
        + 添加类别
      </el-button>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import type { QuestionClassifierConfig } from '@/types/workflow'

interface Props {
  config: QuestionClassifierConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: QuestionClassifierConfig]
}>()

const localConfig = ref<QuestionClassifierConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const addClass = () => {
  if (!localConfig.value.classes) {
    localConfig.value.classes = []
  }
  localConfig.value.classes.push({
    id: `class_${Date.now()}`,
    name: '',
    description: ''
  })
  emitUpdate()
}

const removeClass = (index: number) => {
  localConfig.value.classes?.splice(index, 1)
  emitUpdate()
}

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>

<style scoped>
.class-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.class-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.class-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.class-header .el-input {
  flex: 1;
}
</style>
