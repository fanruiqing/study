<template>
  <div class="top-toolbar">
    <div class="toolbar-left">
      <el-input
        v-model="localWorkflowName"
        placeholder="工作流名称"
        class="workflow-name-input"
        @change="$emit('update:workflowName', localWorkflowName)"
      />
      <el-tag v-if="isDirty" type="warning" size="small">
        <el-icon><Warning /></el-icon>
        未保存
      </el-tag>
      <el-tag v-else type="success" size="small">
        <el-icon><Check /></el-icon>
        已保存
      </el-tag>
    </div>

    <div class="toolbar-right">
      <!-- 撤销/重做按钮 - 与其他按钮风格统一 -->
      <el-button size="small" :disabled="!canUndo" @click="$emit('undo')" title="撤销 (Ctrl+Z)">
        <el-icon><RefreshLeft /></el-icon>
        撤销
      </el-button>
      <el-button size="small" :disabled="!canRedo" @click="$emit('redo')" title="重做 (Ctrl+Y)">
        <el-icon><RefreshRight /></el-icon>
        重做
      </el-button>
      
      <el-divider direction="vertical" />
      
      <el-button size="small" @click="$emit('preview')">
        <el-icon><View /></el-icon>
        预览
      </el-button>
      <el-button size="small" @click="$emit('features')">
        <el-icon><Setting /></el-icon>
        功能特性
      </el-button>
      <el-button type="primary" size="small" @click="$emit('save')" :loading="isSaving">
        <el-icon><DocumentChecked /></el-icon>
        保存
      </el-button>
      <el-button type="success" size="small" @click="$emit('publish')">
        <el-icon><Upload /></el-icon>
        发布
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  Check,
  Warning,
  RefreshLeft,
  RefreshRight,
  View,
  Setting,
  DocumentChecked,
  Upload
} from '@element-plus/icons-vue'

interface Props {
  workflowName: string
  isDirty?: boolean
  isSaving?: boolean
  canUndo?: boolean
  canRedo?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isDirty: false,
  isSaving: false,
  canUndo: false,
  canRedo: false
})

defineEmits<{
  'update:workflowName': [name: string]
  undo: []
  redo: []
  preview: []
  features: []
  save: []
  publish: []
}>()

const localWorkflowName = ref(props.workflowName)

watch(
  () => props.workflowName,
  (newName) => {
    localWorkflowName.value = newName
  }
)
</script>

<style scoped>
.top-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 56px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-left {
  flex: 1;
}

.toolbar-right {
  flex: 0;
}

.workflow-name-input {
  width: 200px;
}

.workflow-name-input :deep(.el-input__wrapper) {
  font-size: 15px;
  font-weight: 600;
}

.el-divider--vertical {
  margin: 0 8px;
  height: 20px;
}
</style>
