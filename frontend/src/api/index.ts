import axios from 'axios'
import type { Session, Message, ModelProvider, PromptTemplate, ExportData, ModelInfo } from '@/types'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// Session API
export const sessionApi = {
  getAll: () => api.get<Session[]>('/sessions').then(r => r.data),
  create: () => api.post<Session>('/sessions').then(r => r.data),
  get: (id: string) => api.get<Session>(`/sessions/${id}`).then(r => r.data),
  update: (id: string, data: Partial<Session>) => api.put<Session>(`/sessions/${id}`, data).then(r => r.data),
  delete: (id: string) => api.delete(`/sessions/${id}`),
  search: (query: string) => api.get<Session[]>('/sessions/search', { params: { query } }).then(r => r.data)
}

// Chat API
export const chatApi = {
  getMessages: (sessionId: string) => api.get<Message[]>(`/chat/messages/${sessionId}`).then(r => r.data),
  saveMessage: (message: Partial<Message>) => api.post<Message>('/chat/messages', message).then(r => r.data),
  deleteMessage: (messageId: string) => api.delete(`/chat/messages/${messageId}`),
  rateMessage: (messageId: string, rating: number) => api.post(`/chat/messages/${messageId}/rate`, { rating }),
  
  // 上传图片
  uploadImage: async (file: File): Promise<string> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<{ success: string; base64: string; message: string }>('/chat/upload-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.base64
  },
  
  // 上传文件
  uploadFile: async (file: File): Promise<string> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<{ success: string; filename: string; message: string }>('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return response.data.filename
  },
  
  // 上传文档并提取内容
  uploadDocument: async (file: File): Promise<{ filename: string; content: string }> => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await api.post<{ success: string; filename: string; content: string; message: string }>('/chat/upload-document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    return { filename: response.data.filename, content: response.data.content }
  },
  
  streamChat: (sessionId: string, content: string, modelId: string): EventSource => {
    const params = new URLSearchParams({ sessionId, content, modelId })
    return new EventSource(`/api/chat/stream?${params}`)
  },
  
  // 带知识库的流式聊天
  streamChatWithKnowledge: (sessionId: string, content: string, modelId: string): EventSource => {
    const params = new URLSearchParams({ sessionId, content, modelId })
    return new EventSource(`/api/chat/with-knowledge?${params}`)
  },
  
  // 带附件的流式聊天
  streamChatWithAttachments: (sessionId: string, content: string, modelId: string, images: File[]): EventSource => {
    const formData = new FormData()
    formData.append('sessionId', sessionId)
    formData.append('content', content)
    formData.append('modelId', modelId)
    images.forEach(image => formData.append('images', image))
    
    // 注意：EventSource 不支持 POST，需要使用其他方式
    // 这里我们先上传图片，然后使用普通的 stream 接口
    // 实际使用时需要在前端处理
    const params = new URLSearchParams({ sessionId, content, modelId })
    return new EventSource(`/api/chat/stream?${params}`)
  },
  
  regenerate: (messageId: string): EventSource => {
    return new EventSource(`/api/chat/regenerate/${messageId}`)
  },
  
  // 获取消息的引用来源
  getCitations: (messageId: string) => api.get<Array<{
    documentId: string
    documentName: string
    chunkId: string
    content: string
    similarity: number
  }>>(`/chat/${messageId}/citations`).then(r => r.data)
}

// Model Provider API
export const modelApi = {
  getAll: () => api.get<ModelProvider[]>('/models').then(r => r.data),
  getActive: () => api.get<ModelProvider[]>('/models/active').then(r => r.data),
  get: (id: string) => api.get<ModelProvider>(`/models/${id}`).then(r => r.data),
  create: (provider: Partial<ModelProvider>) => api.post<ModelProvider>('/models', provider).then(r => r.data),
  update: (id: string, data: Partial<ModelProvider>) => api.put<ModelProvider>(`/models/${id}`, data).then(r => r.data),
  delete: (id: string) => api.delete(`/models/${id}`),
  
  // 测试已保存的提供商连接
  testConnection: (id: string) => 
    api.post<{ success: boolean; message: string; latency?: number; modelCount?: number }>(`/model-providers/${id}/test`).then(r => r.data),
  
  // 测试单个模型
  testSingleModel: (providerId: string, modelId: string) => 
    api.post<{ success: boolean; message: string; latency?: number }>(`/model-providers/${providerId}/test-model`, { modelId }).then(r => r.data),
  
  // 清除模型失败标记
  clearModelFailedMark: (providerId: string, modelId: string) => 
    api.post(`/model-providers/${providerId}/clear-failed-mark`, { modelId }),
  
  // 测试连接（通过 URL 和 API Key）
  testConnectionWithUrl: (baseUrl: string, apiKey: string) => 
    api.post<{ success: boolean; message: string; latency?: number; modelCount?: number }>('/model-providers/test-connection', { baseUrl, apiKey }).then(r => r.data),
  
  // 获取模型列表（带详细信息）
  fetchModels: (baseUrl: string, apiKey: string) => 
    api.post<{ success: boolean; models?: ModelInfo[]; message?: string }>('/models/fetch-models', { baseUrl, apiKey }).then(r => r.data),
  
  // 获取分组后的模型列表
  fetchModelsGrouped: (baseUrl: string, apiKey: string) => 
    api.post<{ success: boolean; groups?: Record<string, ModelInfo[]>; message?: string }>('/models/fetch-models-grouped', { baseUrl, apiKey }).then(r => r.data),
  
  // 刷新模型列表
  refreshModels: (id: string) => api.post<ModelProvider>(`/models/${id}/refresh-models`).then(r => r.data)
}

// Prompt API
export const promptApi = {
  getAll: () => api.get<PromptTemplate[]>('/prompts').then(r => r.data),
  getGrouped: () => api.get<Record<string, PromptTemplate[]>>('/prompts/grouped').then(r => r.data),
  getCategories: () => api.get<string[]>('/prompts/categories').then(r => r.data),
  get: (id: string) => api.get<PromptTemplate>(`/prompts/${id}`).then(r => r.data),
  create: (template: Partial<PromptTemplate>) => api.post<PromptTemplate>('/prompts', template).then(r => r.data),
  update: (id: string, data: Partial<PromptTemplate>) => api.put<PromptTemplate>(`/prompts/${id}`, data).then(r => r.data),
  delete: (id: string) => api.delete(`/prompts/${id}`),
  search: (query: string) => api.get<PromptTemplate[]>('/prompts/search', { params: { query } }).then(r => r.data)
}

// Export/Import API
export const dataApi = {
  export: () => api.get<ExportData>('/data/export').then(r => r.data),
  exportJson: () => api.get('/data/export/json', { responseType: 'blob' }).then(r => r.data),
  exportMarkdown: () => api.get('/data/export/markdown', { responseType: 'blob' }).then(r => r.data),
  import: (data: string) => api.post('/data/import', data, { headers: { 'Content-Type': 'application/json' } })
}

// Knowledge Base API
export const knowledgeBaseApi = {
  // 获取所有知识库
  getAll: () => api.get<any[]>('/knowledge-bases').then(r => r.data),
  
  // 获取会话关联的知识库
  getBySession: (sessionId: string) => api.get<any[]>(`/knowledge-bases/session/${sessionId}`).then(r => r.data),
  
  // 批量关联知识库到会话（替换现有关联）
  associateToSession: (sessionId: string, knowledgeBaseIds: string[]) => 
    api.post(`/knowledge-bases/${knowledgeBaseIds[0] || 'dummy'}/associate-session`, { 
      sessionId, 
      knowledgeBaseIds 
    })
}

// Workflow API
export interface WorkflowExecution {
  id: string
  workflowId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  input: string
  output?: string
  error?: string
  startTime: number
  endTime?: number
}

export interface NodeExecution {
  id: string
  executionId: string
  nodeId: string
  nodeName: string
  nodeType: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  input?: string
  output?: string
  error?: string
  startTime: number
  endTime?: number
}

export const workflowApi = {
  // 保存工作流
  save: (workflow: any) => api.post('/workflows', workflow).then(r => r.data),
  
  // 更新工作流
  update: (id: string, workflow: any) => api.put(`/workflows/${id}`, workflow).then(r => r.data),
  
  // 获取工作流列表
  list: () => api.get('/workflows').then(r => r.data),
  
  // 获取单个工作流
  get: (id: string) => api.get(`/workflows/${id}`).then(r => r.data),
  
  // 删除工作流
  delete: (id: string) => api.delete(`/workflows/${id}`),
  
  // 发布工作流
  publish: (id: string) => api.post(`/workflows/${id}/publish`).then(r => r.data),
  
  // 取消发布
  unpublish: (id: string) => api.post(`/workflows/${id}/unpublish`),
  
  // 验证工作流
  validate: (id: string) => api.post(`/workflows/${id}/validate`).then(r => r.data),
  
  // 执行已保存的工作流
  execute: (id: string, input?: Record<string, any>) => 
    api.post<WorkflowExecution>(`/workflows/${id}/execute`, input || {}).then(r => r.data),
  
  // 预览执行（无需保存）
  executePreview: (definition: string, input?: Record<string, any>) => 
    api.post<WorkflowExecution>('/workflows/execute-preview', { definition, input: input || {} }).then(r => r.data),
  
  // 流式执行（SSE）
  executeStream: (definition: string, input?: Record<string, any>): EventSource => {
    const params = new URLSearchParams({
      definition,
      input: JSON.stringify(input || {})
    })
    return new EventSource(`/api/workflows/execute-stream?${params}`)
  },
  
  // 获取执行记录
  getExecution: (executionId: string) => 
    api.get<WorkflowExecution>(`/workflows/executions/${executionId}`).then(r => r.data),
  
  // 获取节点执行记录
  getNodeExecutions: (executionId: string) => 
    api.get<NodeExecution[]>(`/workflows/executions/${executionId}/nodes`).then(r => r.data)
}

export default api
