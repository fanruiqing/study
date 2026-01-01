// 工作流类型定义

// 节点类型枚举
export enum NodeType {
  START = 'start',
  LLM = 'llm',
  KNOWLEDGE_BASE = 'knowledge_base',
  QUESTION_CLASSIFIER = 'question_classifier',
  CONDITION = 'condition',
  HTTP = 'http',
  CODE = 'code',
  END = 'end'
}

// 变量类型
export type VariableType = 'string' | 'number' | 'boolean' | 'array' | 'object' | 'file'

// 变量定义
export interface VariableDefinition {
  name: string
  type: VariableType
  description?: string
  required?: boolean
}

// 节点配置基类
export interface NodeConfig {
  [key: string]: any
}

// LLM 节点配置
export interface LLMNodeConfig extends NodeConfig {
  modelId: string
  systemPrompt: string
  userPromptTemplate: string
  temperature: number
  maxTokens: number
  visionEnabled: boolean
  inputVariables: string[]
}

// 问题分类器配置
export interface QuestionClassifierConfig extends NodeConfig {
  modelId: string
  inputVariable: string
  classes: ClassDefinition[]
  instruction?: string
}

export interface ClassDefinition {
  id: string
  name: string
  description: string
}

// 知识库节点配置
export interface KnowledgeBaseNodeConfig extends NodeConfig {
  knowledgeBaseId: string
  queryVariable: string
  topK: number
  similarityThreshold: number
  reranking: boolean
}

// 条件节点配置
export interface ConditionNodeConfig extends NodeConfig {
  conditions: ConditionRule[]
  defaultBranch: string
}

export interface ConditionRule {
  id: string
  name: string
  expression: string
}

// HTTP 节点配置
export interface HTTPNodeConfig extends NodeConfig {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  url: string
  headers: string
  body: string
}

// 代码节点配置
export interface CodeNodeConfig extends NodeConfig {
  language: 'javascript' | 'python'
  code: string
  inputVariables: string[]
}

// START 节点配置
export interface StartNodeConfig extends NodeConfig {
  fields: VariableDefinition[]
}

// END 节点配置
export interface EndNodeConfig extends NodeConfig {
  outputMapping: string
}

// 工作流节点
export interface WorkflowNode {
  id: string
  type: NodeType
  label: string
  x: number
  y: number
  config: NodeConfig
  inputs?: VariableDefinition[]
  outputs?: VariableDefinition[]
}

// 连接
export interface Connection {
  id: string
  sourceNodeId: string
  sourcePort: string
  targetNodeId: string
  targetPort: string
  animated?: boolean
}

// 连接样式
export interface ConnectionStyle {
  strokeColor: string
  strokeWidth: number
  animated: boolean
  dashArray?: string
}

// 执行结果
export interface ExecutionResult {
  nodeId: string
  status: 'success' | 'error'
  inputs: Record<string, any>
  outputs: Record<string, any>
  duration: number
  error?: string
  timestamp: Date
}

// 工作流设置
export interface WorkflowSettings {
  maxParallelism: number
  timeout: number
  retryPolicy: RetryPolicy
}

export interface RetryPolicy {
  maxRetries: number
  retryDelay: number
  backoffMultiplier: number
}

// 全局变量
export interface GlobalVariable {
  name: string
  type: VariableType
  value: any
}

// 工作流
export interface Workflow {
  id: string
  name: string
  description?: string
  nodes: WorkflowNode[]
  connections: Connection[]
  variables: GlobalVariable[]
  settings: WorkflowSettings
  createdAt: Date
  updatedAt: Date
  version: number
}

// 节点状态
export type NodeStatus = 'idle' | 'running' | 'completed' | 'error' | 'waiting'

// 节点分组
export interface NodeGroup {
  name: string
  icon: string
  nodes: NodeTypeConfig[]
}

// 节点类型配置
export interface NodeTypeConfig {
  type: NodeType
  label: string
  icon: string
  color: string
  description: string
  defaultConfig: NodeConfig
}
