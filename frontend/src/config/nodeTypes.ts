import { NodeType, NodeTypeConfig, NodeGroup } from '@/types/workflow'

// 节点颜色配置
export const NODE_COLORS = {
  [NodeType.START]: '#2DD4BF',      // 青绿色
  [NodeType.LLM]: '#3B82F6',        // 蓝色
  [NodeType.KNOWLEDGE_BASE]: '#F59E0B', // 橙色
  [NodeType.QUESTION_CLASSIFIER]: '#8B5CF6', // 紫色
  [NodeType.CONDITION]: '#EF4444',  // 红色
  [NodeType.HTTP]: '#6B7280',       // 灰色
  [NodeType.CODE]: '#10B981',       // 绿色
  [NodeType.END]: '#EC4899'         // 粉色
}

// 节点类型配置
export const NODE_TYPE_CONFIGS: Record<NodeType, NodeTypeConfig> = {
  [NodeType.START]: {
    type: NodeType.START,
    label: 'START',
    icon: '🚀',
    color: NODE_COLORS[NodeType.START],
    description: '工作流起点，定义输入变量',
    defaultConfig: {
      fields: [
        { name: 'input', type: 'string', description: '输入内容', required: true }
      ]
    }
  },
  [NodeType.LLM]: {
    type: NodeType.LLM,
    label: 'LLM',
    icon: '🤖',
    color: NODE_COLORS[NodeType.LLM],
    description: '调用大语言模型',
    defaultConfig: {
      modelId: 'gpt-3.5-turbo',
      systemPrompt: '',
      userPromptTemplate: '',
      temperature: 0.7,
      maxTokens: 2000,
      visionEnabled: false,
      inputVariables: []
    }
  },
  [NodeType.KNOWLEDGE_BASE]: {
    type: NodeType.KNOWLEDGE_BASE,
    label: '知识库',
    icon: '📚',
    color: NODE_COLORS[NodeType.KNOWLEDGE_BASE],
    description: '检索知识库内容',
    defaultConfig: {
      knowledgeBaseId: '',
      queryVariable: 'input',
      topK: 3,
      similarityThreshold: 0.7,
      reranking: false
    }
  },
  [NodeType.QUESTION_CLASSIFIER]: {
    type: NodeType.QUESTION_CLASSIFIER,
    label: '问题分类器',
    icon: '🔀',
    color: NODE_COLORS[NodeType.QUESTION_CLASSIFIER],
    description: '智能分类问题类型',
    defaultConfig: {
      modelId: 'gpt-3.5-turbo',
      inputVariable: 'input',
      classes: [
        { id: 'class1', name: '类别1', description: '描述类别1' },
        { id: 'class2', name: '类别2', description: '描述类别2' }
      ],
      instruction: ''
    }
  },
  [NodeType.CONDITION]: {
    type: NodeType.CONDITION,
    label: '条件',
    icon: '❓',
    color: NODE_COLORS[NodeType.CONDITION],
    description: '条件分支判断',
    defaultConfig: {
      conditions: [
        { id: 'cond1', name: '条件1', expression: '' }
      ],
      defaultBranch: 'default'
    }
  },
  [NodeType.HTTP]: {
    type: NodeType.HTTP,
    label: 'HTTP',
    icon: '🌐',
    color: NODE_COLORS[NodeType.HTTP],
    description: '调用外部 API',
    defaultConfig: {
      method: 'GET',
      url: '',
      headers: '{}',
      body: ''
    }
  },
  [NodeType.CODE]: {
    type: NodeType.CODE,
    label: '代码',
    icon: '📝',
    color: NODE_COLORS[NodeType.CODE],
    description: '执行自定义代码',
    defaultConfig: {
      language: 'javascript',
      code: '',
      inputVariables: []
    }
  },
  [NodeType.END]: {
    type: NodeType.END,
    label: 'END',
    icon: '🏁',
    color: NODE_COLORS[NodeType.END],
    description: '工作流终点',
    defaultConfig: {
      outputMapping: '{}'
    }
  }
}

// 节点分组
export const NODE_GROUPS: NodeGroup[] = [
  {
    name: '基础',
    icon: '⬡',
    nodes: [
      NODE_TYPE_CONFIGS[NodeType.START],
      NODE_TYPE_CONFIGS[NodeType.END]
    ]
  },
  {
    name: 'AI',
    icon: '🤖',
    nodes: [
      NODE_TYPE_CONFIGS[NodeType.LLM],
      NODE_TYPE_CONFIGS[NodeType.KNOWLEDGE_BASE],
      NODE_TYPE_CONFIGS[NodeType.QUESTION_CLASSIFIER]
    ]
  },
  {
    name: '逻辑',
    icon: '⚙️',
    nodes: [
      NODE_TYPE_CONFIGS[NodeType.CONDITION]
    ]
  },
  {
    name: '工具',
    icon: '🔧',
    nodes: [
      NODE_TYPE_CONFIGS[NodeType.HTTP],
      NODE_TYPE_CONFIGS[NodeType.CODE]
    ]
  }
]

// 获取节点配置
export function getNodeConfig(type: NodeType): NodeTypeConfig {
  return NODE_TYPE_CONFIGS[type]
}

// 获取节点颜色
export function getNodeColor(type: NodeType): string {
  return NODE_COLORS[type]
}

// 获取节点的默认输入变量
function getDefaultInputs(type: NodeType) {
  switch (type) {
    case NodeType.START:
      return [] // START节点没有输入
    case NodeType.LLM:
      return [
        { name: 'query', type: 'string', description: '用户问题' },
        { name: 'context', type: 'string', description: '上下文信息（可选）' }
      ]
    case NodeType.KNOWLEDGE_BASE:
      return [{ name: 'query', type: 'string', description: '查询内容' }]
    case NodeType.QUESTION_CLASSIFIER:
      return [{ name: 'input', type: 'string', description: '待分类问题' }]
    case NodeType.CONDITION:
      return [{ name: 'input', type: 'object', description: '条件输入' }]
    case NodeType.HTTP:
      return [{ name: 'data', type: 'object', description: '请求数据' }]
    case NodeType.CODE:
      return [{ name: 'input', type: 'object', description: '代码输入' }]
    case NodeType.END:
      return [{ name: 'result', type: 'object', description: '最终结果' }]
    default:
      return []
  }
}

// 获取节点的默认输出变量
function getDefaultOutputs(type: NodeType) {
  switch (type) {
    case NodeType.START:
      return [{ name: 'input', type: 'string', description: '用户输入' }]
    case NodeType.LLM:
      return [{ name: 'response', type: 'string', description: 'LLM响应' }]
    case NodeType.KNOWLEDGE_BASE:
      return [
        { name: 'results', type: 'array', description: '检索结果' },
        { name: 'context', type: 'string', description: '上下文文本' }
      ]
    case NodeType.QUESTION_CLASSIFIER:
      return [{ name: 'category', type: 'string', description: '分类结果' }]
    case NodeType.CONDITION:
      return [{ name: 'branch', type: 'string', description: '分支结果' }]
    case NodeType.HTTP:
      return [{ name: 'response', type: 'object', description: 'HTTP响应' }]
    case NodeType.CODE:
      return [{ name: 'output', type: 'object', description: '代码输出' }]
    case NodeType.END:
      return [] // END节点没有输出
    default:
      return []
  }
}

// 创建新节点
export function createNode(type: NodeType, x: number, y: number) {
  const config = getNodeConfig(type)
  return {
    id: `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    type,
    label: config.label,
    x,
    y,
    config: JSON.parse(JSON.stringify(config.defaultConfig)),
    inputs: getDefaultInputs(type),
    outputs: getDefaultOutputs(type)
  }
}
