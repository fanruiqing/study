<template>
  <div class="config-panel" v-if="node">
    <div class="panel-header">
      <div class="panel-title">
        <span class="node-icon">{{ getNodeIcon(node.type) }}</span>
        <span>{{ node.label }}</span>
        <el-tag v-if="node.status" :type="getStatusType(node.status)" size="small">
          {{ getStatusText(node.status) }}
        </el-tag>
      </div>
      <el-icon class="close-btn" @click="$emit('close')">
        <Close />
      </el-icon>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 设置标签页 -->
      <el-tab-pane label="设置" name="settings">
        <div class="config-content">
          <el-form label-position="top" size="small">
            <el-form-item label="节点名称">
              <el-input v-model="node.label" placeholder="输入节点名称" />
            </el-form-item>

            <!-- START 节点配置 -->
            <template v-if="node.type === 'start'">
              <StartNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- LLM 节点配置 -->
            <template v-else-if="node.type === 'llm'">
              <LLMNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- 知识库节点配置 -->
            <template v-else-if="node.type === 'knowledge_base'">
              <KnowledgeBaseNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- 问题分类器配置 -->
            <template v-else-if="node.type === 'question_classifier'">
              <QuestionClassifierConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- 条件节点配置 -->
            <template v-else-if="node.type === 'condition'">
              <ConditionNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- HTTP 节点配置 -->
            <template v-else-if="node.type === 'http'">
              <HTTPNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- 代码节点配置 -->
            <template v-else-if="node.type === 'code'">
              <CodeNodeConfig :config="node.config" @update="updateConfig" />
            </template>

            <!-- END 节点配置 -->
            <template v-else-if="node.type === 'end'">
              <EndNodeConfig :config="node.config" @update="updateConfig" />
            </template>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 上次运行标签页 -->
      <el-tab-pane label="上次运行" name="lastRun">
        <div class="config-content">
          <ExecutionResult :result="executionResult" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { getNodeConfig } from '@/config/nodeTypes'
import type { WorkflowNode, NodeStatus, ExecutionResult as ExecResult } from '@/types/workflow'
import StartNodeConfig from './config/StartNodeConfig.vue'
import LLMNodeConfig from './config/LLMNodeConfig.vue'
import KnowledgeBaseNodeConfig from './config/KnowledgeBaseNodeConfig.vue'
import QuestionClassifierConfig from './config/QuestionClassifierConfig.vue'
import ConditionNodeConfig from './config/ConditionNodeConfig.vue'
import HTTPNodeConfig from './config/HTTPNodeConfig.vue'
import CodeNodeConfig from './config/CodeNodeConfig.vue'
import EndNodeConfig from './config/EndNodeConfig.vue'
import ExecutionResult from './ExecutionResult.vue'

interface Props {
  node: WorkflowNode | null
  executionResult?: ExecResult | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: []
  update: [node: WorkflowNode]
}>()

const activeTab = ref('settings')

const getNodeIcon = (type: string) => {
  return getNodeConfig(type as any)?.icon || '📦'
}

const getStatusType = (status: NodeStatus) => {
  const typeMap = {
    idle: 'info',
    running: 'warning',
    completed: 'success',
    error: 'danger',
    waiting: 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status: NodeStatus) => {
  const textMap = {
    idle: '空闲',
    running: '运行中',
    completed: '已完成',
    error: '错误',
    waiting: '等待中'
  }
  return textMap[status] || status
}

const updateConfig = (newConfig: any) => {
  if (props.node) {
    emit('update', {
      ...props.node,
      config: newConfig
    })
  }
}
</script>

<style scoped>
.config-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.node-icon {
  font-size: 20px;
}

.close-btn {
  cursor: pointer;
  color: #909399;
  font-size: 18px;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #303133;
}

.config-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 16px;
}

.config-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
}

.config-content {
  padding: 16px;
}
</style>
