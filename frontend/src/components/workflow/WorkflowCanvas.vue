<template>
  <div
    ref="canvasContainer"
    class="workflow-canvas"
    @drop="onDrop"
    @dragover.prevent
    @mousedown="onCanvasMouseDown"
    @mousemove="onCanvasMouseMove"
    @mouseup="onCanvasMouseUp"
    @wheel.prevent="onCanvasWheel"
  >
    <!-- SVG 层用于绘制连接线 -->
    <svg class="connections-layer" :style="svgStyle">
      <defs>
        <marker
          id="arrowhead"
          markerWidth="10"
          markerHeight="10"
          refX="9"
          refY="3"
          orient="auto"
        >
          <polygon points="0 0, 10 3, 0 6" fill="#409eff" />
        </marker>
        <marker
          id="arrowhead-temp"
          markerWidth="10"
          markerHeight="10"
          refX="9"
          refY="3"
          orient="auto"
        >
          <polygon points="0 0, 10 3, 0 6" fill="#67c23a" />
        </marker>
      </defs>

      <!-- 已有连接 -->
      <g v-for="conn in connections" :key="conn.id">
        <path
          :d="getConnectionPath(conn)"
          :class="['connection-line', { animated: conn.animated }]"
          @mouseenter="hoveredConnection = conn.id"
          @mouseleave="hoveredConnection = null"
          @click="onConnectionClick(conn)"
        />
        <circle
          v-if="hoveredConnection === conn.id"
          :cx="getConnectionMidpoint(conn).x"
          :cy="getConnectionMidpoint(conn).y"
          r="12"
          class="connection-delete-btn"
          @click.stop="$emit('deleteConnection', conn.id)"
        />
      </g>

      <!-- 临时连接线 -->
      <path
        v-if="tempConnection"
        :d="tempConnection.path"
        class="connection-line temp-connection"
      />
    </svg>

    <!-- 网格背景 -->
    <div class="grid-background" :style="gridStyle"></div>

    <!-- 节点层 -->
    <div class="nodes-layer" :style="nodesLayerStyle">
      <NodeCard
        v-for="node in nodes"
        :key="node.id"
        :label="node.label"
        :icon="getNodeIcon(node.type)"
        :color="getNodeColor(node.type)"
        :status="nodeStatuses.get(node.id) || node.status"
        :selected="selectedNodeId === node.id"
        :x="node.x"
        :y="node.y"
        :scale="scale"
        :inputs="node.inputs"
        :outputs="node.outputs"
        :description="getNodeDescription(node)"
        :show-input-port="node.type !== 'start'"
        :show-output-port="node.type !== 'end'"
        @mousedown="onNodeMouseDown($event, node)"
        @select="$emit('selectNode', node.id)"
        @delete="$emit('deleteNode', node.id)"
        @port-mousedown="(event, portType) => onPortMouseDown(event, node, portType)"
        @port-mouseup="(portType) => onPortMouseUp(node, portType)"
      />
    </div>

    <!-- 缩放比例显示 -->
    <div class="zoom-indicator">
      {{ Math.round(scale * 100) }}%
    </div>

    <!-- 小地图 -->
    <Minimap
      :nodes="nodes"
      :connections="connections"
      :canvas-width="canvasWidth"
      :canvas-height="canvasHeight"
      :viewport-x="-offset.x / scale"
      :viewport-y="-offset.y / scale"
      :viewport-width="containerWidth / scale"
      :viewport-height="containerHeight / scale"
      :scale="scale"
      @navigate="navigateToPosition"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import NodeCard from './NodeCard.vue'
import Minimap from './Minimap.vue'
import { getNodeConfig, getNodeColor } from '@/config/nodeTypes'
import type { WorkflowNode, Connection, NodeType } from '@/types/workflow'

interface Props {
  nodes: WorkflowNode[]
  connections: Connection[]
  selectedNodeId: string | null
  scale?: number
  offset?: { x: number; y: number }
  nodeStatuses?: Map<string, string>
}

const props = withDefaults(defineProps<Props>(), {
  scale: 1,
  offset: () => ({ x: 0, y: 0 }),
  nodeStatuses: () => new Map()
})

const emit = defineEmits<{
  'update:scale': [scale: number]
  'update:offset': [offset: { x: number; y: number }]
  selectNode: [nodeId: string]
  deleteNode: [nodeId: string]
  deleteConnection: [connectionId: string]
  updateNode: [node: WorkflowNode]
  addConnection: [connection: Omit<Connection, 'id'>]
  addNode: [type: NodeType, x: number, y: number]
}>()

const canvasContainer = ref<HTMLDivElement | null>(null)
const containerWidth = ref(0)
const containerHeight = ref(0)
const canvasWidth = ref(5000)
const canvasHeight = ref(5000)

const isDraggingNode = ref(false)
const draggedNode = ref<WorkflowNode | null>(null)
const dragOffset = ref({ x: 0, y: 0 })

const isPanning = ref(false)
const panStart = ref({ x: 0, y: 0 })

const isConnecting = ref(false)
const connectionStart = ref<{ node: WorkflowNode; port: 'input' | 'output' } | null>(null)
const tempConnectionEnd = ref({ x: 0, y: 0 })

const hoveredConnection = ref<string | null>(null)

const svgStyle = computed(() => ({
  width: '100%',
  height: '100%'
}))

const gridStyle = computed(() => ({
  backgroundSize: `${20 * props.scale}px ${20 * props.scale}px`,
  backgroundPosition: `${props.offset.x}px ${props.offset.y}px`
}))

const nodesLayerStyle = computed(() => ({
  transform: `translate(${props.offset.x}px, ${props.offset.y}px) scale(${props.scale})`,
  transformOrigin: '0 0'
}))

const tempConnection = computed(() => {
  if (!isConnecting.value || !connectionStart.value) return null

  const node = connectionStart.value.node
  const isOutput = connectionStart.value.port === 'output'
  
  // 计算起点位置（在节点层坐标系中）
  const nodeX = isOutput ? node.x + NODE_WIDTH : node.x
  const nodeY = node.y + NODE_PORT_Y_OFFSET
  
  // 转换到屏幕坐标
  const startX = nodeX * props.scale + props.offset.x
  const startY = nodeY * props.scale + props.offset.y

  return {
    path: createBezierPath(startX, startY, tempConnectionEnd.value.x, tempConnectionEnd.value.y)
  }
})

const getNodeIcon = (type: NodeType) => {
  return getNodeConfig(type)?.icon || '📦'
}

// 获取节点状态（优先使用外部传入的状态）
const nodeStatuses = computed(() => props.nodeStatuses)

const getNodeDescription = (node: WorkflowNode) => {
  // 根据节点类型返回描述
  return ''
}

const createBezierPath = (x1: number, y1: number, x2: number, y2: number) => {
  const controlPointOffset = Math.min(Math.abs(x2 - x1) / 2, 150)
  return `M ${x1} ${y1} C ${x1 + controlPointOffset} ${y1}, ${x2 - controlPointOffset} ${y2}, ${x2} ${y2}`
}

// 节点卡片宽度和端口位置
const NODE_WIDTH = 220
const NODE_PORT_Y_OFFSET = 50 // 端口在节点顶部往下50px的位置

const getConnectionPath = (conn: Connection) => {
  const sourceNode = props.nodes.find(n => n.id === conn.sourceNodeId)
  const targetNode = props.nodes.find(n => n.id === conn.targetNodeId)
  
  if (!sourceNode || !targetNode) return ''

  console.log('Drawing connection:', {
    source: { id: conn.sourceNodeId, label: sourceNode.label, x: sourceNode.x },
    target: { id: conn.targetNodeId, label: targetNode.label, x: targetNode.x }
  })

  // 源节点右侧端口位置（输出端口）- 在节点层坐标系中
  const sourceX = sourceNode.x + NODE_WIDTH
  const sourceY = sourceNode.y + NODE_PORT_Y_OFFSET
  
  // 目标节点左侧端口位置（输入端口）- 在节点层坐标系中
  const targetX = targetNode.x
  const targetY = targetNode.y + NODE_PORT_Y_OFFSET

  // 转换到屏幕坐标（考虑缩放和偏移）
  const startX = sourceX * props.scale + props.offset.x
  const startY = sourceY * props.scale + props.offset.y
  const endX = targetX * props.scale + props.offset.x
  const endY = targetY * props.scale + props.offset.y

  console.log('Connection path:', { startX, startY, endX, endY })

  return createBezierPath(startX, startY, endX, endY)
}

const getConnectionMidpoint = (conn: Connection) => {
  const sourceNode = props.nodes.find(n => n.id === conn.sourceNodeId)
  const targetNode = props.nodes.find(n => n.id === conn.targetNodeId)
  
  if (!sourceNode || !targetNode) return { x: 0, y: 0 }

  const sourceX = sourceNode.x + NODE_WIDTH
  const sourceY = sourceNode.y + NODE_PORT_Y_OFFSET
  const targetX = targetNode.x
  const targetY = targetNode.y + NODE_PORT_Y_OFFSET

  const startX = sourceX * props.scale + props.offset.x
  const startY = sourceY * props.scale + props.offset.y
  const endX = targetX * props.scale + props.offset.x
  const endY = targetY * props.scale + props.offset.y

  return {
    x: (startX + endX) / 2,
    y: (startY + endY) / 2
  }
}

const onDrop = (event: DragEvent) => {
  event.preventDefault()
  const data = event.dataTransfer?.getData('application/json')
  if (!data) return

  const nodeConfig = JSON.parse(data)
  const rect = canvasContainer.value?.getBoundingClientRect()
  if (!rect) return

  const x = (event.clientX - rect.left - props.offset.x) / props.scale
  const y = (event.clientY - rect.top - props.offset.y) / props.scale

  emit('addNode', nodeConfig.type, x, y)
}

const onNodeMouseDown = (event: MouseEvent, node: WorkflowNode) => {
  isDraggingNode.value = true
  draggedNode.value = node
  dragOffset.value = {
    x: event.clientX - (node.x * props.scale + props.offset.x),
    y: event.clientY - (node.y * props.scale + props.offset.y)
  }
}

const onPortMouseDown = (event: MouseEvent, node: WorkflowNode, port: 'input' | 'output') => {
  isConnecting.value = true
  connectionStart.value = { node, port }
  
  const rect = canvasContainer.value?.getBoundingClientRect()
  if (rect) {
    tempConnectionEnd.value = {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    }
  }
}

const onPortMouseUp = (targetNode: WorkflowNode, port: 'input' | 'output') => {
  if (!isConnecting.value || !connectionStart.value) return

  const sourceNode = connectionStart.value.node
  const sourcePort = connectionStart.value.port

  console.log('Connection attempt:', {
    from: { node: sourceNode.label, port: sourcePort },
    to: { node: targetNode.label, port: port }
  })

  // 验证连接
  if (sourceNode.id === targetNode.id) {
    console.log('Rejected: self-connection')
    isConnecting.value = false
    connectionStart.value = null
    return
  }

  if (sourcePort === port) {
    console.log('Rejected: same port type')
    isConnecting.value = false
    connectionStart.value = null
    return
  }

  // 确定源和目标：output -> input
  // 如果从output端口开始拖拽，sourceNode是源，targetNode是目标
  // 如果从input端口开始拖拽，targetNode是源，sourceNode是目标
  const source = sourcePort === 'output' ? sourceNode : targetNode
  const target = sourcePort === 'output' ? targetNode : sourceNode

  console.log('Creating connection:', {
    source: source.label,
    target: target.label
  })

  // 检查是否已存在连接
  const exists = props.connections.some(
    c => c.sourceNodeId === source.id && c.targetNodeId === target.id
  )

  if (!exists) {
    emit('addConnection', {
      sourceNodeId: source.id,
      sourcePort: 'output',
      targetNodeId: target.id,
      targetPort: 'input',
      animated: true
    })
  }

  isConnecting.value = false
  connectionStart.value = null
}

const onCanvasMouseDown = (event: MouseEvent) => {
  if (event.target === canvasContainer.value || (event.target as HTMLElement).classList.contains('grid-background')) {
    isPanning.value = true
    panStart.value = { x: event.clientX, y: event.clientY }
    emit('selectNode', '')
  }
}

const onCanvasMouseMove = (event: MouseEvent) => {
  if (isDraggingNode.value && draggedNode.value) {
    const newX = (event.clientX - dragOffset.value.x - props.offset.x) / props.scale
    const newY = (event.clientY - dragOffset.value.y - props.offset.y) / props.scale
    
    emit('updateNode', {
      ...draggedNode.value,
      x: newX,
      y: newY
    })
  }

  if (isPanning.value) {
    const newOffset = {
      x: props.offset.x + (event.clientX - panStart.value.x),
      y: props.offset.y + (event.clientY - panStart.value.y)
    }
    emit('update:offset', newOffset)
    panStart.value = { x: event.clientX, y: event.clientY }
  }

  if (isConnecting.value) {
    const rect = canvasContainer.value?.getBoundingClientRect()
    if (rect) {
      tempConnectionEnd.value = {
        x: event.clientX - rect.left,
        y: event.clientY - rect.top
      }
    }
  }
}

const onCanvasMouseUp = () => {
  isDraggingNode.value = false
  draggedNode.value = null
  isPanning.value = false

  if (isConnecting.value) {
    isConnecting.value = false
    connectionStart.value = null
  }
}

const onCanvasWheel = (event: WheelEvent) => {
  const delta = event.deltaY > 0 ? 0.9 : 1.1
  const newScale = Math.max(0.25, Math.min(2, props.scale * delta))

  const rect = canvasContainer.value?.getBoundingClientRect()
  if (rect) {
    const mouseX = event.clientX - rect.left
    const mouseY = event.clientY - rect.top

    const newOffset = {
      x: mouseX - (mouseX - props.offset.x) * (newScale / props.scale),
      y: mouseY - (mouseY - props.offset.y) * (newScale / props.scale)
    }
    emit('update:offset', newOffset)
  }

  emit('update:scale', newScale)
}

const onConnectionClick = (conn: Connection) => {
  // 可以添加连接点击逻辑
}

const navigateToPosition = (x: number, y: number) => {
  const newOffset = {
    x: containerWidth.value / 2 - x * props.scale,
    y: containerHeight.value / 2 - y * props.scale
  }
  emit('update:offset', newOffset)
}

const updateContainerSize = () => {
  if (canvasContainer.value) {
    containerWidth.value = canvasContainer.value.clientWidth
    containerHeight.value = canvasContainer.value.clientHeight
  }
}

onMounted(() => {
  updateContainerSize()
  window.addEventListener('resize', updateContainerSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateContainerSize)
})
</script>

<style scoped>
.workflow-canvas {
  position: relative;
  flex: 1;
  overflow: hidden;
  background: #fafafa;
  user-select: none;
}

.grid-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(to right, #e5e5e5 1px, transparent 1px),
    linear-gradient(to bottom, #e5e5e5 1px, transparent 1px);
  pointer-events: none;
}

.connections-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  transform-origin: 0 0;
}

.connection-line {
  fill: none;
  stroke: #409eff;
  stroke-width: 2;
  pointer-events: stroke;
  cursor: pointer;
  transition: stroke-width 0.2s;
  marker-end: url(#arrowhead);
}

.connection-line:hover {
  stroke-width: 3;
  stroke: #66b1ff;
}

.connection-line.animated {
  stroke-dasharray: 5;
  animation: dash 0.5s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}

.temp-connection {
  stroke: #67c23a;
  stroke-dasharray: 5;
  marker-end: url(#arrowhead-temp);
}

.connection-delete-btn {
  fill: #f56c6c;
  cursor: pointer;
  pointer-events: all;
}

.connection-delete-btn:hover {
  fill: #f78989;
}

.nodes-layer {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: 0 0;
}

.zoom-indicator {
  position: absolute;
  bottom: 20px;
  left: 20px;
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
