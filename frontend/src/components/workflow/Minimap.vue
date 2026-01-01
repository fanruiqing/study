<template>
  <div class="minimap" :style="minimapStyle">
    <canvas ref="minimapCanvas" @click="onMinimapClick"></canvas>
    <div class="viewport-indicator" :style="viewportStyle"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { WorkflowNode, Connection } from '@/types/workflow'

interface Props {
  nodes: WorkflowNode[]
  connections: Connection[]
  canvasWidth: number
  canvasHeight: number
  viewportX: number
  viewportY: number
  viewportWidth: number
  viewportHeight: number
  scale: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  navigate: [x: number, y: number]
}>()

const minimapCanvas = ref<HTMLCanvasElement | null>(null)
const minimapWidth = 200
const minimapHeight = 150
const minimapScale = ref(1)

const minimapStyle = computed(() => ({
  width: `${minimapWidth}px`,
  height: `${minimapHeight}px`
}))

const viewportStyle = computed(() => {
  const scaleX = minimapWidth / props.canvasWidth
  const scaleY = minimapHeight / props.canvasHeight
  
  return {
    left: `${props.viewportX * scaleX}px`,
    top: `${props.viewportY * scaleY}px`,
    width: `${props.viewportWidth * scaleX}px`,
    height: `${props.viewportHeight * scaleY}px`
  }
})

const drawMinimap = () => {
  const canvas = minimapCanvas.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = minimapWidth
  canvas.height = minimapHeight

  // 清空画布
  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, minimapWidth, minimapHeight)

  // 计算缩放比例
  const scaleX = minimapWidth / props.canvasWidth
  const scaleY = minimapHeight / props.canvasHeight
  minimapScale.value = Math.min(scaleX, scaleY)

  // 绘制连接线
  ctx.strokeStyle = '#d0d0d0'
  ctx.lineWidth = 1
  props.connections.forEach(conn => {
    const sourceNode = props.nodes.find(n => n.id === conn.sourceNodeId)
    const targetNode = props.nodes.find(n => n.id === conn.targetNodeId)
    
    if (sourceNode && targetNode) {
      ctx.beginPath()
      ctx.moveTo(sourceNode.x * scaleX, sourceNode.y * scaleY)
      ctx.lineTo(targetNode.x * scaleX, targetNode.y * scaleY)
      ctx.stroke()
    }
  })

  // 绘制节点
  props.nodes.forEach(node => {
    ctx.fillStyle = '#409eff'
    ctx.fillRect(
      node.x * scaleX - 2,
      node.y * scaleY - 2,
      4,
      4
    )
  })
}

const onMinimapClick = (event: MouseEvent) => {
  const canvas = minimapCanvas.value
  if (!canvas) return

  const rect = canvas.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const scaleX = props.canvasWidth / minimapWidth
  const scaleY = props.canvasHeight / minimapHeight

  emit('navigate', x * scaleX, y * scaleY)
}

watch(() => [props.nodes, props.connections], () => {
  drawMinimap()
}, { deep: true })

onMounted(() => {
  drawMinimap()
})
</script>

<style scoped>
.minimap {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  cursor: pointer;
  z-index: 100;
}

.minimap canvas {
  display: block;
}

.viewport-indicator {
  position: absolute;
  border: 2px solid #409eff;
  background: rgba(64, 158, 255, 0.1);
  pointer-events: none;
}
</style>
