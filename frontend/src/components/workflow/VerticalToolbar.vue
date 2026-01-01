<template>
  <div class="vertical-toolbar">
    <div v-for="group in nodeGroups" :key="group.name" class="node-group">
      <div class="group-header">
        <span class="group-icon">{{ group.icon }}</span>
        <span class="group-name">{{ group.name }}</span>
      </div>
      <div class="group-nodes">
        <div
          v-for="nodeConfig in group.nodes"
          :key="nodeConfig.type"
          class="toolbar-node"
          :title="nodeConfig.description"
          draggable="true"
          @dragstart="onDragStart($event, nodeConfig)"
          @click="onClick(nodeConfig)"
        >
          <span class="node-icon">{{ nodeConfig.icon }}</span>
          <span class="node-label">{{ nodeConfig.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NODE_GROUPS } from '@/config/nodeTypes'
import type { NodeTypeConfig } from '@/types/workflow'

const nodeGroups = NODE_GROUPS

const emit = defineEmits<{
  addNode: [nodeConfig: NodeTypeConfig, position?: { x: number; y: number }]
}>()

const onDragStart = (event: DragEvent, nodeConfig: NodeTypeConfig) => {
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('application/json', JSON.stringify(nodeConfig))
  }
}

const onClick = (nodeConfig: NodeTypeConfig) => {
  // 点击时在画布中央添加节点
  emit('addNode', nodeConfig)
}
</script>

<style scoped>
.vertical-toolbar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  padding: 16px 8px;
}

.node-group {
  margin-bottom: 20px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.group-icon {
  font-size: 16px;
}

.group-name {
  flex: 1;
}

.group-nodes {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.toolbar-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  font-size: 13px;
  user-select: none;
}

.toolbar-node:hover {
  background: #e6f0ff;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.toolbar-node:active {
  cursor: grabbing;
}

.node-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.node-label {
  flex: 1;
  font-weight: 500;
  color: #303133;
}
</style>
