<template>
  <div class="workflow-builder">
    <!-- 顶部工具栏 -->
    <TopToolbar
      v-model:workflow-name="workflowName"
      :is-dirty="isDirty"
      :is-saving="isSaving"
      :can-undo="canUndo"
      :can-redo="canRedo"
      @undo="undo"
      @redo="redo"
      @preview="previewWorkflow"
      @features="showFeaturesDialog"
      @save="saveWorkflow"
      @publish="publishWorkflow"
    />

    <div class="main-content">
      <!-- 左侧节点工具栏 -->
      <VerticalToolbar @add-node="addNodeFromToolbar" />

      <!-- 中间画布区域 -->
      <WorkflowCanvas
        :nodes="nodes"
        :connections="connections"
        :selected-node-id="selectedNodeId"
        :node-statuses="nodeStatuses"
        v-model:scale="canvasScale"
        v-model:offset="canvasOffset"
        @select-node="selectNode"
        @delete-node="deleteNode"
        @delete-connection="deleteConnection"
        @update-node="updateNode"
        @add-connection="addConnection"
        @add-node="addNode"
      />

      <!-- 右侧配置面板 -->
      <ConfigPanel
        v-if="selectedNode"
        :node="selectedNode"
        :execution-result="getNodeExecutionResult(selectedNodeId)"
        @close="selectedNodeId = null"
        @update="updateNode"
      />
    </div>

    <!-- 运行对话框 -->
    <el-dialog v-model="showRunDialog" title="运行工作流" width="500px">
      <el-form label-position="top">
        <el-form-item v-for="field in inputFields" :key="field.name" :label="field.name">
          <el-input v-model="runInputs[field.name]" :placeholder="`输入 ${field.name}`" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRunDialog = false">取消</el-button>
        <el-button type="primary" @click="executeWorkflow" :loading="isRunning">执行</el-button>
      </template>
    </el-dialog>

    <!-- 执行结果抽屉 -->
    <el-drawer
      v-model="showExecutionDrawer"
      title="执行结果"
      direction="rtl"
      size="400px"
    >
      <div class="execution-drawer-content">
        <!-- 执行状态 -->
        <div class="execution-status">
          <el-icon v-if="isRunning" class="is-loading"><Loading /></el-icon>
          <el-tag v-else-if="executionStatus === 'COMPLETED'" type="success" size="large">
            <el-icon><Check /></el-icon>
            执行成功
          </el-tag>
          <el-tag v-else-if="executionStatus === 'FAILED'" type="danger" size="large">
            <el-icon><Close /></el-icon>
            执行失败
          </el-tag>
          <span v-if="isRunning" class="status-text">正在执行...</span>
        </div>

        <!-- 执行时间 -->
        <div v-if="executionDuration" class="execution-time">
          <el-icon><Timer /></el-icon>
          耗时: {{ executionDuration }}ms
        </div>

        <!-- 节点执行列表 -->
        <div class="node-executions">
          <div class="section-title">节点执行详情</div>
          <el-timeline>
            <el-timeline-item
              v-for="nodeExec in nodeExecutions"
              :key="nodeExec.nodeId"
              :type="getNodeStatusType(nodeExec.status)"
              :timestamp="nodeExec.startTime ? formatTime(nodeExec.startTime) : ''"
            >
              <div class="node-exec-item">
                <div class="node-exec-header">
                  <span class="node-name">{{ nodeExec.nodeName }}</span>
                  <el-tag :type="getNodeStatusType(nodeExec.status)" size="small">
                    {{ getNodeStatusText(nodeExec.status) }}
                  </el-tag>
                </div>
                
                <!-- 知识库节点特殊显示 -->
                <div v-if="nodeExec.nodeType === 'knowledge_base' && nodeExec.output" class="kb-output">
                  <div v-if="getKBStats(nodeExec.output)" class="kb-stats">
                    <div class="stats-row">
                      <span class="stats-label">检索结果:</span>
                      <span class="stats-value">{{ getKBStats(nodeExec.output).totalRetrieved }} 条</span>
                    </div>
                    <div class="stats-row" v-if="getKBStats(nodeExec.output).filteredOut > 0">
                      <span class="stats-label">过滤掉:</span>
                      <span class="stats-value warning">{{ getKBStats(nodeExec.output).filteredOut }} 条 (阈值: {{ getKBStats(nodeExec.output).threshold }})</span>
                    </div>
                    <div class="stats-row">
                      <span class="stats-label">最终使用:</span>
                      <span class="stats-value success">{{ getKBStats(nodeExec.output).afterFilter }} 条</span>
                    </div>
                  </div>
                  <!-- 被过滤的结果提示 -->
                  <div v-if="getFilteredOut(nodeExec.output).length > 0" class="filtered-warning">
                    <el-collapse>
                      <el-collapse-item>
                        <template #title>
                          <el-icon><Warning /></el-icon>
                          <span>{{ getFilteredOut(nodeExec.output).length }} 条结果因相似度不足被过滤</span>
                        </template>
                        <div v-for="(item, idx) in getFilteredOut(nodeExec.output)" :key="idx" class="filtered-item">
                          <div class="filtered-doc">{{ item.documentName }}</div>
                          <div class="filtered-reason">{{ item.reason }}</div>
                        </div>
                      </el-collapse-item>
                    </el-collapse>
                  </div>
                  <!-- 检索结果 -->
                  <div v-if="getKBResults(nodeExec.output).length > 0" class="kb-results">
                    <div class="output-label">检索到的内容:</div>
                    <div v-for="(result, idx) in getKBResults(nodeExec.output)" :key="idx" class="kb-result-item">
                      <div class="result-header">
                        <span class="doc-name">{{ result.documentName }}</span>
                        <el-tag size="small" type="success">{{ (result.score * 100).toFixed(1) }}%</el-tag>
                      </div>
                      <div class="result-content">{{ truncateText(result.content, 200) }}</div>
                    </div>
                  </div>
                  <div v-else class="no-results">
                    <el-icon><InfoFilled /></el-icon>
                    <span>未检索到相关内容</span>
                  </div>
                </div>
                
                <!-- LLM 流式输出 -->
                <div v-else-if="nodeExec.nodeType === 'llm' && nodeExec.output" class="llm-output">
                  <div class="output-label">
                    <el-icon v-if="nodeExec.status === 'RUNNING'" class="is-loading"><Loading /></el-icon>
                    AI 输出:
                  </div>
                  <div class="llm-content">{{ nodeExec.output }}</div>
                </div>
                <!-- 普通节点输出 -->
                <div v-else-if="nodeExec.output" class="node-exec-output">
                  <div class="output-label">输出:</div>
                  <pre class="output-content">{{ formatOutput(nodeExec.output) }}</pre>
                </div>
                <div v-if="nodeExec.error" class="node-exec-error">
                  {{ nodeExec.error }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- 最终输出 -->
        <div v-if="finalOutput" class="final-output">
          <div class="section-title">最终输出</div>
          <pre class="output-content">{{ formatOutput(finalOutput) }}</pre>
        </div>
      </div>
    </el-drawer>

    <!-- 功能特性对话框 -->
    <el-dialog v-model="showFeaturesDialogVisible" title="工作流功能特性" width="600px">
      <el-form label-position="top">
        <el-form-item label="描述">
          <el-input
            v-model="workflowDescription"
            type="textarea"
            :rows="3"
            placeholder="输入工作流描述"
          />
        </el-form-item>
        <el-form-item label="最大并行度">
          <el-input-number v-model="workflowSettings.maxParallelism" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="超时时间 (秒)">
          <el-input-number v-model="workflowSettings.timeout" :min="10" :max="3600" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFeaturesDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveFeaturesSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, Check, Close, Timer, Warning, InfoFilled } from '@element-plus/icons-vue'
import TopToolbar from '@/components/workflow/TopToolbar.vue'
import VerticalToolbar from '@/components/workflow/VerticalToolbar.vue'
import WorkflowCanvas from '@/components/workflow/WorkflowCanvas.vue'
import ConfigPanel from '@/components/workflow/ConfigPanel.vue'
import { createNode } from '@/config/nodeTypes'
import { workflowApi, type NodeExecution } from '@/api'
import type {
  WorkflowNode,
  Connection,
  NodeType,
  NodeTypeConfig,
  ExecutionResult,
  WorkflowSettings
} from '@/types/workflow'

// 状态
const workflowName = ref('新建工作流')
const workflowDescription = ref('')
const nodes = ref<WorkflowNode[]>([])
const connections = ref<Connection[]>([])
const selectedNodeId = ref<string | null>(null)
const canvasScale = ref(1)
const canvasOffset = ref({ x: 0, y: 0 })

// 撤销/重做
const history = ref<Array<{ nodes: WorkflowNode[]; connections: Connection[] }>>([])
const historyIndex = ref(-1)
const isDirty = ref(false)
const isSaving = ref(false)

// 执行相关
const isRunning = ref(false)
const showRunDialog = ref(false)
const runInputs = ref<Record<string, string>>({})
const executionResults = ref<Map<string, ExecutionResult>>(new Map())
const showExecutionDrawer = ref(false)
const executionStatus = ref<'RUNNING' | 'COMPLETED' | 'FAILED' | null>(null)
const executionDuration = ref<number | null>(null)
const nodeExecutions = ref<any[]>([])
const finalOutput = ref<string | null>(null)
const nodeStatuses = ref<Map<string, string>>(new Map())
const llmStreamingContent = ref<Map<string, string>>(new Map())
const executionStartTime = ref<number | null>(null)

// 对话框
const showFeaturesDialogVisible = ref(false)
const workflowSettings = ref<WorkflowSettings>({
  maxParallelism: 3,
  timeout: 300,
  retryPolicy: {
    maxRetries: 3,
    retryDelay: 1000,
    backoffMultiplier: 2
  }
})

// 计算属性
const selectedNode = computed(() => {
  return nodes.value.find(n => n.id === selectedNodeId.value) || null
})

const inputFields = computed(() => {
  const startNode = nodes.value.find(n => n.type === 'start')
  return startNode?.config.fields || []
})

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

// 初始化 - 自动创建 START 节点
const initializeWorkflow = () => {
  if (nodes.value.length === 0) {
    const startNode = createNode('start' as NodeType, 100, 200)
    nodes.value.push(startNode)
    saveToHistory()
  }
}

// 历史记录
const saveToHistory = () => {
  history.value = history.value.slice(0, historyIndex.value + 1)
  history.value.push({
    nodes: JSON.parse(JSON.stringify(nodes.value)),
    connections: JSON.parse(JSON.stringify(connections.value))
  })
  historyIndex.value++
  isDirty.value = true
  if (history.value.length > 50) {
    history.value.shift()
    historyIndex.value--
  }
}

const undo = () => {
  if (canUndo.value) {
    historyIndex.value--
    const state = history.value[historyIndex.value]
    nodes.value = JSON.parse(JSON.stringify(state.nodes))
    connections.value = JSON.parse(JSON.stringify(state.connections))
    ElMessage.info('已撤销')
  }
}

const redo = () => {
  if (canRedo.value) {
    historyIndex.value++
    const state = history.value[historyIndex.value]
    nodes.value = JSON.parse(JSON.stringify(state.nodes))
    connections.value = JSON.parse(JSON.stringify(state.connections))
    ElMessage.info('已重做')
  }
}

// 键盘快捷键处理
const handleKeyDown = (event: KeyboardEvent) => {
  // Ctrl+Z 撤销
  if (event.ctrlKey && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    undo()
  }
  // Ctrl+Y 或 Ctrl+Shift+Z 重做
  if ((event.ctrlKey && event.key === 'y') || (event.ctrlKey && event.shiftKey && event.key === 'z')) {
    event.preventDefault()
    redo()
  }
  // Delete 删除选中节点
  if (event.key === 'Delete' && selectedNodeId.value) {
    deleteNode(selectedNodeId.value)
  }
  // Ctrl+S 保存
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    saveWorkflow()
  }
}

// 节点操作
const addNodeFromToolbar = (nodeConfig: NodeTypeConfig, position?: { x: number; y: number }) => {
  const x = position?.x || 400
  const y = position?.y || 200
  addNode(nodeConfig.type, x, y)
}

const addNode = (type: NodeType, x: number, y: number) => {
  const node = createNode(type, x, y)
  nodes.value.push(node)
  selectedNodeId.value = node.id
  saveToHistory()
}

const deleteNode = (nodeId: string) => {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node?.type === 'start') {
    ElMessage.warning('START 节点不能删除')
    return
  }
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  connections.value = connections.value.filter(
    c => c.sourceNodeId !== nodeId && c.targetNodeId !== nodeId
  )
  if (selectedNodeId.value === nodeId) {
    selectedNodeId.value = null
  }
  saveToHistory()
}

const updateNode = (updatedNode: WorkflowNode) => {
  const index = nodes.value.findIndex(n => n.id === updatedNode.id)
  if (index !== -1) {
    nodes.value[index] = updatedNode
    saveToHistory()
  }
}

const selectNode = (nodeId: string) => {
  selectedNodeId.value = nodeId || null
}

// 连接操作
const addConnection = (connection: Omit<Connection, 'id'>) => {
  const newConnection: Connection = {
    id: `conn_${Date.now()}`,
    ...connection
  }
  connections.value.push(newConnection)
  saveToHistory()
}

const deleteConnection = (connectionId: string) => {
  connections.value = connections.value.filter(c => c.id !== connectionId)
  saveToHistory()
}

// 工作流操作
const saveWorkflow = async () => {
  if (nodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }
  isSaving.value = true
  try {
    const workflow = {
      name: workflowName.value,
      description: workflowDescription.value,
      nodes: nodes.value,
      connections: connections.value,
      settings: workflowSettings.value
    }
    console.log('保存工作流:', workflow)
    await new Promise(resolve => setTimeout(resolve, 500))
    isDirty.value = false
    ElMessage.success('工作流已保存')
  } catch (error) {
    ElMessage.error('保存失败')
    console.error(error)
  } finally {
    isSaving.value = false
  }
}

const previewWorkflow = () => {
  if (nodes.value.length === 0) {
    ElMessage.warning('请先添加节点')
    return
  }
  const startNode = nodes.value.find(n => n.type === 'start')
  if (startNode && startNode.config.fields && startNode.config.fields.length > 0) {
    showRunDialog.value = true
    runInputs.value = {}
    startNode.config.fields.forEach((field: any) => {
      runInputs.value[field.name] = ''
    })
  } else {
    executeWorkflow()
  }
}

const publishWorkflow = async () => {
  if (isDirty.value) {
    ElMessage.warning('请先保存工作流')
    return
  }
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('工作流已发布')
  } catch (error) {
    ElMessage.error('发布失败')
    console.error(error)
  }
}

const executeWorkflow = async () => {
  showRunDialog.value = false
  isRunning.value = true
  showExecutionDrawer.value = true
  executionStatus.value = 'RUNNING'
  executionDuration.value = null
  nodeExecutions.value = []
  finalOutput.value = null
  nodeStatuses.value.clear()
  llmStreamingContent.value.clear()
  executionStartTime.value = Date.now()
  
  // 更新所有节点状态为等待
  nodes.value.forEach(node => {
    nodeStatuses.value.set(node.id, 'waiting')
  })
  
  // 构建工作流定义
  const definition = JSON.stringify({
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: n.type,
      name: n.label,
      config: n.config
    })),
    edges: connections.value.map(c => ({
      source: c.sourceNodeId,
      target: c.targetNodeId
    }))
  })
  
  // 使用流式执行
  const eventSource = workflowApi.executeStream(definition, runInputs.value)
  
  eventSource.addEventListener('start', (event: MessageEvent) => {
    console.log('Workflow started:', event.data)
  })
  
  eventSource.addEventListener('node_start', (event: MessageEvent) => {
    const data = JSON.parse(event.data)
    console.log('Node started:', data)
    
    // 更新节点状态
    nodeStatuses.value.set(data.nodeId, 'running')
    
    // 添加到执行列表
    nodeExecutions.value.push({
      nodeId: data.nodeId,
      nodeName: data.nodeName,
      nodeType: data.nodeType,
      status: 'RUNNING',
      startTime: Date.now(),
      output: null,
      error: null
    })
  })
  
  eventSource.addEventListener('node_complete', (event: MessageEvent) => {
    const data = JSON.parse(event.data)
    console.log('Node completed:', data)
    
    // 更新节点状态
    nodeStatuses.value.set(data.nodeId, 'completed')
    
    // 更新执行列表
    const nodeExec = nodeExecutions.value.find(n => n.nodeId === data.nodeId)
    if (nodeExec) {
      nodeExec.status = 'COMPLETED'
      nodeExec.output = JSON.stringify(data.output, null, 2)
      nodeExec.endTime = Date.now()
    }
  })
  
  eventSource.addEventListener('node_error', (event: MessageEvent) => {
    const data = JSON.parse(event.data)
    console.log('Node error:', data)
    
    // 更新节点状态
    nodeStatuses.value.set(data.nodeId, 'error')
    
    // 更新执行列表
    const nodeExec = nodeExecutions.value.find(n => n.nodeId === data.nodeId)
    if (nodeExec) {
      nodeExec.status = 'FAILED'
      nodeExec.error = data.error
      nodeExec.endTime = Date.now()
    }
  })
  
  eventSource.addEventListener('llm_token', (event: MessageEvent) => {
    const data = JSON.parse(event.data)
    
    // 累积LLM输出
    const currentContent = llmStreamingContent.value.get(data.nodeId) || ''
    llmStreamingContent.value.set(data.nodeId, currentContent + data.token)
    
    // 更新执行列表中的输出（实时显示）
    const nodeExec = nodeExecutions.value.find(n => n.nodeId === data.nodeId)
    if (nodeExec) {
      nodeExec.output = llmStreamingContent.value.get(data.nodeId)
    }
  })
  
  eventSource.addEventListener('complete', (event: MessageEvent) => {
    const data = JSON.parse(event.data)
    console.log('Workflow completed:', data)
    
    executionStatus.value = 'COMPLETED'
    finalOutput.value = JSON.stringify(data.output, null, 2)
    executionDuration.value = Date.now() - (executionStartTime.value || Date.now())
    isRunning.value = false
    
    ElMessage.success('工作流执行完成')
    eventSource.close()
  })
  
  eventSource.addEventListener('error', (event: MessageEvent) => {
    if (event.data) {
      const data = JSON.parse(event.data)
      console.error('Workflow error:', data)
      ElMessage.error('执行失败: ' + data.error)
    }
    
    executionStatus.value = 'FAILED'
    executionDuration.value = Date.now() - (executionStartTime.value || Date.now())
    isRunning.value = false
    eventSource.close()
  })
  
  eventSource.onerror = (error) => {
    console.error('SSE error:', error)
    if (isRunning.value) {
      executionStatus.value = 'FAILED'
      isRunning.value = false
      ElMessage.error('连接中断')
    }
    eventSource.close()
  }
}

const showFeaturesDialog = () => {
  showFeaturesDialogVisible.value = true
}

const saveFeaturesSettings = () => {
  showFeaturesDialogVisible.value = false
  isDirty.value = true
  ElMessage.success('设置已保存')
}

const getNodeExecutionResult = (nodeId: string | null): ExecutionResult | null => {
  if (!nodeId) return null
  return executionResults.value.get(nodeId) || null
}

// 执行结果相关辅助函数
const getNodeStatusType = (status: string) => {
  const map: Record<string, string> = {
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

const getNodeStatusText = (status: string) => {
  const map: Record<string, string> = {
    RUNNING: '执行中',
    COMPLETED: '成功',
    FAILED: '失败'
  }
  return map[status] || status
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN')
}

const formatOutput = (output: string | null) => {
  if (!output) return ''
  try {
    return JSON.stringify(JSON.parse(output), null, 2)
  } catch {
    return output
  }
}

// 知识库节点辅助函数
const getKBStats = (output: string | null) => {
  if (!output) return null
  try {
    const data = typeof output === 'string' ? JSON.parse(output) : output
    return data._stats || null
  } catch {
    return null
  }
}

const getFilteredOut = (output: string | null) => {
  if (!output) return []
  try {
    const data = typeof output === 'string' ? JSON.parse(output) : output
    return data._filteredOut || []
  } catch {
    return []
  }
}

const getKBResults = (output: string | null) => {
  if (!output) return []
  try {
    const data = typeof output === 'string' ? JSON.parse(output) : output
    return data.results || []
  } catch {
    return []
  }
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

onMounted(() => {
  initializeWorkflow()
  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  // 移除键盘事件监听
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.workflow-builder {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 执行结果抽屉样式 */
.execution-drawer-content {
  padding: 16px;
}

.execution-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.execution-status .is-loading {
  font-size: 24px;
  color: #409eff;
}

.status-text {
  font-size: 16px;
  color: #606266;
}

.execution-time {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px;
  background: #ecf5ff;
  border-radius: 6px;
  color: #409eff;
  font-size: 14px;
}

.node-executions {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.node-exec-item {
  padding: 8px 0;
}

.node-exec-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.node-name {
  font-weight: 500;
  color: #303133;
}

.node-exec-output {
  margin-top: 8px;
}

.output-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.output-content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #303133;
  overflow-x: auto;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
}

.node-exec-error {
  margin-top: 8px;
  padding: 8px 12px;
  background: #fef0f0;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 12px;
}

/* 知识库节点输出样式 */
.kb-output {
  margin-top: 8px;
}

.kb-stats {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.stats-label {
  font-size: 12px;
  color: #606266;
}

.stats-value {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
}

.stats-value.warning {
  color: #e6a23c;
}

.stats-value.success {
  color: #67c23a;
}

.filtered-warning {
  margin-bottom: 10px;
}

.filtered-warning :deep(.el-collapse-item__header) {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #e6a23c;
  height: auto;
  line-height: 1.5;
}

.filtered-warning :deep(.el-collapse-item__header .el-icon) {
  margin-right: 6px;
}

.filtered-warning :deep(.el-collapse-item__content) {
  padding: 8px 12px;
  background: #fffbf0;
}

.filtered-item {
  padding: 6px 0;
  border-bottom: 1px dashed #faecd8;
}

.filtered-item:last-child {
  border-bottom: none;
}

.filtered-doc {
  font-size: 12px;
  font-weight: 500;
  color: #303133;
}

.filtered-reason {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.kb-results {
  margin-top: 8px;
}

.kb-result-item {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;
}

.kb-result-item:last-child {
  margin-bottom: 0;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.doc-name {
  font-size: 12px;
  font-weight: 500;
  color: #409eff;
}

.result-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.no-results {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px;
  background: #f4f4f5;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
}

.llm-output {
  margin-top: 8px;
}

.llm-output .output-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #409eff;
  margin-bottom: 6px;
  font-weight: 500;
}

.llm-content {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #bae6fd;
}

.final-output {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
