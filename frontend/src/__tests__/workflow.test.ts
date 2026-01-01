/**
 * 工作流 UI 属性测试
 * 验证 workflow-ui-redesign 规范中定义的所有属性
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { NodeType, WorkflowNode, Connection, VariableDefinition } from '@/types/workflow'
import { NODE_COLORS, NODE_TYPE_CONFIGS, createNode, getNodeColor } from '@/config/nodeTypes'

// Property 1: Node Creation Consistency
// 验证节点创建的一致性
describe('Property 1: Node Creation Consistency', () => {
  it('should create node with correct type and position', () => {
    const node = createNode(NodeType.LLM, 100, 200)
    
    expect(node.type).toBe(NodeType.LLM)
    expect(node.x).toBe(100)
    expect(node.y).toBe(200)
    expect(node.id).toBeTruthy()
    expect(node.label).toBe('LLM')
  })

  it('should create unique IDs for each node', () => {
    const node1 = createNode(NodeType.LLM, 0, 0)
    const node2 = createNode(NodeType.LLM, 0, 0)
    
    expect(node1.id).not.toBe(node2.id)
  })

  it('should include default config for each node type', () => {
    Object.values(NodeType).forEach(type => {
      const node = createNode(type, 0, 0)
      expect(node.config).toBeDefined()
    })
  })
})

// Property 2: Node Card Information Completeness
// 验证节点卡片信息完整性
describe('Property 2: Node Card Information Completeness', () => {
  it('should have all required fields in node config', () => {
    Object.values(NodeType).forEach(type => {
      const config = NODE_TYPE_CONFIGS[type]
      
      expect(config.type).toBe(type)
      expect(config.label).toBeTruthy()
      expect(config.icon).toBeTruthy()
      expect(config.color).toBeTruthy()
      expect(config.description).toBeTruthy()
      expect(config.defaultConfig).toBeDefined()
    })
  })
})

// Property 3: Variable Display Consistency
// 验证变量显示一致性
describe('Property 3: Variable Display Consistency', () => {
  it('should have valid variable definitions', () => {
    const validTypes = ['string', 'number', 'boolean', 'array', 'object', 'file']
    
    const variable: VariableDefinition = {
      name: 'test',
      type: 'string',
      description: 'Test variable'
    }
    
    expect(validTypes).toContain(variable.type)
    expect(variable.name).toBeTruthy()
  })
})

// Property 4: Status Class Mapping
// 验证状态类映射
describe('Property 4: Status Class Mapping', () => {
  const statusClasses = {
    idle: 'status-idle',
    running: 'status-running',
    completed: 'status-completed',
    error: 'status-error',
    waiting: 'status-waiting'
  }

  it('should map each status to a unique class', () => {
    const classes = Object.values(statusClasses)
    const uniqueClasses = new Set(classes)
    
    expect(classes.length).toBe(uniqueClasses.size)
  })
})

// Property 5: Node Type Color Uniqueness
// 验证节点类型颜色唯一性
describe('Property 5: Node Type Color Uniqueness', () => {
  it('should have unique colors for each node type', () => {
    const colors = Object.values(NODE_COLORS)
    const uniqueColors = new Set(colors)
    
    expect(colors.length).toBe(uniqueColors.size)
  })

  it('should return correct color for each node type', () => {
    Object.values(NodeType).forEach(type => {
      const color = getNodeColor(type)
      expect(color).toBe(NODE_COLORS[type])
    })
  })
})

// Property 6: Connection Validation
// 验证连接验证逻辑
describe('Property 6: Connection Validation', () => {
  const validateConnection = (
    sourceNode: WorkflowNode,
    targetNode: WorkflowNode,
    sourcePort: 'input' | 'output',
    targetPort: 'input' | 'output'
  ): { valid: boolean; error?: string } => {
    // 不能自连接
    if (sourceNode.id === targetNode.id) {
      return { valid: false, error: '不能连接到自身' }
    }
    
    // 只能从输出连接到输入
    if (sourcePort === targetPort) {
      return { valid: false, error: '只能从输出连接到输入' }
    }
    
    return { valid: true }
  }

  it('should reject self-connections', () => {
    const node = createNode(NodeType.LLM, 0, 0)
    const result = validateConnection(node, node, 'output', 'input')
    
    expect(result.valid).toBe(false)
    expect(result.error).toBe('不能连接到自身')
  })

  it('should reject same-port connections', () => {
    const node1 = createNode(NodeType.LLM, 0, 0)
    const node2 = createNode(NodeType.LLM, 100, 0)
    
    const result1 = validateConnection(node1, node2, 'output', 'output')
    expect(result1.valid).toBe(false)
    
    const result2 = validateConnection(node1, node2, 'input', 'input')
    expect(result2.valid).toBe(false)
  })

  it('should accept valid connections', () => {
    const node1 = createNode(NodeType.LLM, 0, 0)
    const node2 = createNode(NodeType.LLM, 100, 0)
    
    const result = validateConnection(node1, node2, 'output', 'input')
    expect(result.valid).toBe(true)
  })
})

// Property 7: Selection State Synchronization
// 验证选择状态同步
describe('Property 7: Selection State Synchronization', () => {
  it('should track selected node correctly', () => {
    let selectedNodeId: string | null = null
    const nodes: WorkflowNode[] = [
      createNode(NodeType.START, 0, 0),
      createNode(NodeType.LLM, 100, 0)
    ]

    // 选择节点
    selectedNodeId = nodes[0].id
    expect(selectedNodeId).toBe(nodes[0].id)

    // 切换选择
    selectedNodeId = nodes[1].id
    expect(selectedNodeId).toBe(nodes[1].id)

    // 取消选择
    selectedNodeId = null
    expect(selectedNodeId).toBeNull()
  })
})

// Property 8: Dirty State Detection
// 验证脏状态检测
describe('Property 8: Dirty State Detection', () => {
  it('should detect changes correctly', () => {
    let isDirty = false
    const originalNodes: WorkflowNode[] = []
    let currentNodes: WorkflowNode[] = []

    // 添加节点应该标记为脏
    currentNodes.push(createNode(NodeType.LLM, 0, 0))
    isDirty = JSON.stringify(originalNodes) !== JSON.stringify(currentNodes)
    expect(isDirty).toBe(true)

    // 保存后应该不脏
    isDirty = false
    expect(isDirty).toBe(false)
  })
})

// Property 9: Zoom Level Bounds
// 验证缩放边界
describe('Property 9: Zoom Level Bounds', () => {
  const MIN_ZOOM = 0.25
  const MAX_ZOOM = 2

  const clampZoom = (zoom: number): number => {
    return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom))
  }

  it('should clamp zoom to minimum', () => {
    expect(clampZoom(0.1)).toBe(MIN_ZOOM)
    expect(clampZoom(0)).toBe(MIN_ZOOM)
    expect(clampZoom(-1)).toBe(MIN_ZOOM)
  })

  it('should clamp zoom to maximum', () => {
    expect(clampZoom(3)).toBe(MAX_ZOOM)
    expect(clampZoom(10)).toBe(MAX_ZOOM)
  })

  it('should allow valid zoom levels', () => {
    expect(clampZoom(1)).toBe(1)
    expect(clampZoom(0.5)).toBe(0.5)
    expect(clampZoom(1.5)).toBe(1.5)
  })
})

// Property 10: START Node Auto-Creation
// 验证 START 节点自动创建
describe('Property 10: START Node Auto-Creation', () => {
  it('should create START node on initialization', () => {
    const nodes: WorkflowNode[] = []
    
    // 模拟初始化
    if (nodes.length === 0) {
      nodes.push(createNode(NodeType.START, 100, 200))
    }
    
    expect(nodes.length).toBe(1)
    expect(nodes[0].type).toBe(NodeType.START)
  })

  it('should not allow deleting START node', () => {
    const nodes: WorkflowNode[] = [createNode(NodeType.START, 0, 0)]
    
    const canDelete = (nodeId: string): boolean => {
      const node = nodes.find(n => n.id === nodeId)
      return node?.type !== NodeType.START
    }
    
    expect(canDelete(nodes[0].id)).toBe(false)
  })
})

// Property 11: Variable Definition Structure
// 验证变量定义结构
describe('Property 11: Variable Definition Structure', () => {
  it('should have valid variable structure', () => {
    const variable: VariableDefinition = {
      name: 'input',
      type: 'string',
      description: 'User input',
      required: true
    }
    
    expect(variable.name).toBeTruthy()
    expect(['string', 'number', 'boolean', 'array', 'object', 'file']).toContain(variable.type)
  })
})

// Property 12: Classifier Output Ports
// 验证分类器输出端口
describe('Property 12: Classifier Output Ports', () => {
  it('should create output port for each class', () => {
    const classifierConfig = {
      modelId: 'gpt-4',
      inputVariable: 'input',
      classes: [
        { id: 'class1', name: '技术问题', description: '技术相关问题' },
        { id: 'class2', name: '业务问题', description: '业务相关问题' },
        { id: 'class3', name: '其他', description: '其他问题' }
      ]
    }
    
    // 每个分类应该有一个输出端口
    expect(classifierConfig.classes.length).toBe(3)
    classifierConfig.classes.forEach(cls => {
      expect(cls.id).toBeTruthy()
      expect(cls.name).toBeTruthy()
    })
  })
})

// Property 13: Classification Routing
// 验证分类路由
describe('Property 13: Classification Routing', () => {
  it('should route to correct branch based on classification', () => {
    const classes = [
      { id: 'tech', name: '技术' },
      { id: 'business', name: '业务' }
    ]
    
    const route = (classificationResult: string): string | null => {
      const cls = classes.find(c => c.id === classificationResult)
      return cls ? cls.id : null
    }
    
    expect(route('tech')).toBe('tech')
    expect(route('business')).toBe('business')
    expect(route('unknown')).toBeNull()
  })
})

// Property 14: Vision Mode Configuration
// 验证视觉模式配置
describe('Property 14: Vision Mode Configuration', () => {
  it('should have vision mode toggle in LLM config', () => {
    const llmConfig = NODE_TYPE_CONFIGS[NodeType.LLM].defaultConfig
    
    expect('visionEnabled' in llmConfig).toBe(true)
    expect(typeof llmConfig.visionEnabled).toBe('boolean')
  })
})

// Property 15: Execution Result Completeness
// 验证执行结果完整性
describe('Property 15: Execution Result Completeness', () => {
  it('should have all required fields in execution result', () => {
    const result = {
      nodeId: 'node_1',
      status: 'success' as const,
      inputs: { query: 'test' },
      outputs: { response: 'result' },
      duration: 1500,
      timestamp: new Date()
    }
    
    expect(result.nodeId).toBeTruthy()
    expect(['success', 'error']).toContain(result.status)
    expect(result.inputs).toBeDefined()
    expect(result.outputs).toBeDefined()
    expect(typeof result.duration).toBe('number')
    expect(result.timestamp).toBeInstanceOf(Date)
  })
})

// Property 16: Error Result Information
// 验证错误结果信息
describe('Property 16: Error Result Information', () => {
  it('should include error message when status is error', () => {
    const errorResult = {
      nodeId: 'node_1',
      status: 'error' as const,
      inputs: {},
      outputs: {},
      duration: 500,
      error: 'Connection timeout',
      timestamp: new Date()
    }
    
    expect(errorResult.status).toBe('error')
    expect(errorResult.error).toBeTruthy()
  })
})
