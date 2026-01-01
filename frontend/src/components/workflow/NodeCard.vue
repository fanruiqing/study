<template>
  <div
    class="node-card"
    :class="[`status-${status}`, { selected }]"
    :style="cardStyle"
    @mousedown.stop="$emit('mousedown', $event)"
    @click.stop="$emit('select')"
  >
    <!-- 节点头部 -->
    <div class="node-header" :style="{ backgroundColor: color }">
      <span class="node-icon">{{ icon }}</span>
      <span class="node-title">{{ label }}</span>
      <el-icon class="node-delete" @click.stop="$emit('delete')">
        <Close />
      </el-icon>
    </div>

    <!-- 节点主体 -->
    <div class="node-body">
      <!-- 输入变量标签 -->
      <div v-if="inputs && inputs.length > 0" class="variable-section">
        <div class="variable-label">输入</div>
        <div class="variable-chips">
          <span
            v-for="input in inputs"
            :key="input.name"
            class="variable-chip input-chip"
            :title="input.description"
          >
            {{ input.name }}
          </span>
        </div>
      </div>

      <!-- 输出变量标签 -->
      <div v-if="outputs && outputs.length > 0" class="variable-section">
        <div class="variable-label">输出</div>
        <div class="variable-chips">
          <span
            v-for="output in outputs"
            :key="output.name"
            class="variable-chip output-chip"
            :title="output.description"
          >
            {{ output.name }}
          </span>
        </div>
      </div>

      <!-- 节点描述 -->
      <div v-if="description" class="node-description">
        {{ description }}
      </div>
    </div>

    <!-- 状态指示器 -->
    <div v-if="status !== 'idle'" class="status-indicator">
      <span v-if="status === 'running'" class="status-icon running">⏳</span>
      <span v-else-if="status === 'completed'" class="status-icon completed">✓</span>
      <span v-else-if="status === 'error'" class="status-icon error">✗</span>
    </div>

    <!-- 输入端口 -->
    <div
      v-if="showInputPort"
      class="connection-port input-port"
      @mousedown.stop="$emit('portMousedown', $event, 'input')"
      @mouseup.stop="$emit('portMouseup', 'input')"
    ></div>

    <!-- 输出端口 -->
    <div
      v-if="showOutputPort"
      class="connection-port output-port"
      @mousedown.stop="$emit('portMousedown', $event, 'output')"
      @mouseup.stop="$emit('portMouseup', 'output')"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Close } from '@element-plus/icons-vue'
import type { NodeStatus, VariableDefinition } from '@/types/workflow'

interface Props {
  label: string
  icon: string
  color: string
  status?: NodeStatus
  selected?: boolean
  x: number
  y: number
  scale?: number
  inputs?: VariableDefinition[]
  outputs?: VariableDefinition[]
  description?: string
  showInputPort?: boolean
  showOutputPort?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  status: 'idle',
  selected: false,
  scale: 1,
  showInputPort: true,
  showOutputPort: true
})

defineEmits<{
  mousedown: [event: MouseEvent]
  select: []
  delete: []
  portMousedown: [event: MouseEvent, portType: 'input' | 'output']
  portMouseup: [portType: 'input' | 'output']
}>()

const cardStyle = computed(() => ({
  left: `${props.x}px`,
  top: `${props.y}px`
}))
</script>

<style scoped>
.node-card {
  position: absolute;
  width: 220px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: move;
  transform-origin: top left;
  transition: box-shadow 0.2s, transform 0.1s;
  user-select: none;
}

.node-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.node-card.selected {
  box-shadow: 0 0 0 2px #409eff, 0 4px 16px rgba(64, 158, 255, 0.3);
}

.node-card.status-running {
  box-shadow: 0 0 0 2px #e6a23c, 0 4px 16px rgba(230, 162, 60, 0.3);
  animation: pulse 1.5s ease-in-out infinite;
}

.node-card.status-completed {
  box-shadow: 0 0 0 2px #67c23a, 0 4px 16px rgba(103, 194, 58, 0.3);
}

.node-card.status-error {
  box-shadow: 0 0 0 2px #f56c6c, 0 4px 16px rgba(245, 108, 108, 0.3);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px 12px 0 0;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.node-icon {
  font-size: 18px;
}

.node-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-delete {
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s;
  font-size: 16px;
}

.node-delete:hover {
  opacity: 1;
}

.node-body {
  padding: 12px 14px;
  min-height: 60px;
}

.variable-section {
  margin-bottom: 10px;
}

.variable-section:last-child {
  margin-bottom: 0;
}

.variable-label {
  font-size: 11px;
  font-weight: 600;
  color: #909399;
  text-transform: uppercase;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.variable-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.variable-chip {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.input-chip {
  background: #e6f0ff;
  color: #409eff;
  border: 1px solid #b3d8ff;
}

.output-chip {
  background: #e1f3d8;
  color: #67c23a;
  border: 1px solid #b3e19d;
}

.node-description {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 8px;
}

.status-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.status-icon.running {
  animation: spin 2s linear infinite;
}

.status-icon.completed {
  color: #67c23a;
}

.status-icon.error {
  color: #f56c6c;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.connection-port {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #fff;
  border: 2px solid #409eff;
  border-radius: 50%;
  cursor: crosshair;
  transition: all 0.2s;
  z-index: 10;
}

.connection-port:hover {
  transform: scale(1.4);
  background: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.5);
}

.input-port {
  left: -7px;
  top: 50px;
  transform: translateY(-50%);
}

.output-port {
  right: -7px;
  top: 50px;
  transform: translateY(-50%);
}

.input-port:hover,
.output-port:hover {
  transform: translateY(-50%) scale(1.4);
}
</style>
