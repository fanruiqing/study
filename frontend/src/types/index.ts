export interface Message {
  id: string
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
  modelId?: string
  rating?: number
  metadata?: Record<string, any>
  thinking?: string  // 思考过程内容
  attachments?: Array<{
    type: string
    name: string
    url: string
    mimeType?: string
    size?: number
  }>
}

export interface Session {
  id: string
  title: string
  createdAt: number
  updatedAt: number
  modelId?: string
  messageCount: number
}

export interface ModelInfo {
  id: string
  name: string
  group?: string           // 分组：deepseek, gpt, claude, embedding, etc.
  type?: string            // 类型：chat, embedding, vision, tool
  supportTools?: boolean   // 是否支持工具调用/MCP
  supportVision?: boolean  // 是否支持视觉
  contextLength?: number   // 上下文长度
  isFailed?: boolean       // 是否调用失败过
}

export interface ModelProvider {
  id: string
  name: string
  type: 'openai' | 'claude' | 'qwen' | 'custom'
  apiKey: string
  baseUrl?: string
  modelName?: string
  isActive: boolean
  temperature?: number
  maxTokens?: number
  models?: string[]           // 模型ID列表（兼容）
  modelInfos?: ModelInfo[]    // 详细模型信息列表
}

export interface PromptTemplate {
  id: string
  title: string
  content: string
  category?: string
  createdAt: number
  updatedAt: number
}

export interface ExportData {
  version: string
  exportedAt: number
  sessions: Session[]
  messages: Message[]
}
