# AI Assistant Toolbox

类似 Cherry Studio 的智能助手工具箱，支持多模型对话、会话管理、提示词模板等功能。

## 技术栈

- **前端**: Vue 3 + TypeScript + Vite + Pinia + Element Plus
- **后端**: Java 17 + Spring Boot 3 + Spring AI + Maven + MyBatis-Plus + Liquibase
- **数据库**: MySQL 8.0+

## 快速开始

### 数据库配置

1. 创建 MySQL 数据库（或让应用自动创建）
2. 复制配置文件：
   ```bash
   cd backend/src/main/resources
   cp application-example.yml application.yml
   ```
3. 编辑 `application.yml`，填入你的数据库连接信息

> **注意**: 数据库表会通过 Liquibase 自动创建和迁移，无需手动执行 SQL 脚本。

### 后端启动

```bash
cd backend

# 安装依赖并启动
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

首次启动时，Liquibase 会自动创建所有必要的数据库表。

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 http://localhost:5173 启动

## 配置模型

API Key 通过应用内的「设置」页面管理，无需在配置文件中设置。

1. 启动应用后，进入「设置」页面
2. 点击「添加模型」
3. 填写模型信息：
   - 名称：如 "GPT-4"
   - 类型：选择 OpenAI/Claude/通义千问/自定义
   - API Key：你的 API 密钥
   - Base URL：API 地址（可选，默认 OpenAI 官方地址）
   - 模型名称：如 "gpt-4", "gpt-3.5-turbo"

## 功能特性

### 💬 智能对话
- 多模型支持（OpenAI、Claude、通义千问、硅基流动等）
- 流式响应（SSE 实时输出）
- Markdown 渲染与代码高亮
- 消息操作（复制、重新生成、删除）
- 会话管理（创建、删除、重命名、搜索）

### 📚 知识库（RAG）
- 创建和管理多个知识库
- 支持多种文档格式（PDF、Word、TXT、Markdown 等）
- 智能文档切片（段落切片、固定大小切片）
- 向量化存储与语义检索
- 会话关联知识库，实现基于私有数据的问答
- 引用来源展示，显示回答依据的文档片段

### 🔧 可视化工作流编排
- 拖拽式工作流设计器
- 多种节点类型：
  - 开始/结束节点
  - LLM 节点（调用大语言模型）
  - 知识库节点（RAG 检索）
  - 条件分支节点
  - HTTP 请求节点
  - 代码执行节点
- 工作流保存、加载与执行
- 执行结果可视化

### 📝 提示词模板
- 创建和管理提示词模板
- 模板分类管理
- 快速应用到对话

### ⚙️ 系统设置
- 模型提供商配置（API Key、Base URL）
- 数据导入导出
- 亮色/暗色主题切换

## 项目结构

```
├── backend/                          # 后端 Spring Boot 项目
│   ├── src/main/java/com/ai/assistant/
│   │   ├── config/                   # 配置类
│   │   │   ├── CorsConfig.java       # 跨域配置
│   │   │   ├── MyBatisPlusConfig.java # MyBatis-Plus 配置
│   │   │   ├── VectorStoreConfig.java # 向量存储配置
│   │   │   └── ...
│   │   ├── controller/               # REST API 控制器
│   │   │   ├── ChatController.java   # 聊天接口
│   │   │   ├── SessionController.java # 会话管理
│   │   │   ├── KnowledgeBaseController.java # 知识库接口
│   │   │   ├── DocumentController.java # 文档管理
│   │   │   ├── WorkflowController.java # 工作流接口
│   │   │   ├── ModelProviderController.java # 模型提供商
│   │   │   └── PromptController.java # 提示词模板
│   │   ├── service/                  # 业务逻辑层
│   │   │   ├── ChatService.java      # 聊天服务
│   │   │   ├── KnowledgeBaseService.java # 知识库服务
│   │   │   ├── RAGService.java       # RAG 检索增强生成
│   │   │   ├── DocumentService.java  # 文档处理
│   │   │   ├── TextChunkingService.java # 文本切片
│   │   │   ├── EmbeddingService.java # 向量嵌入
│   │   │   ├── WorkflowService.java  # 工作流服务
│   │   │   └── ...
│   │   ├── entity/                   # 数据库实体
│   │   │   ├── Session.java          # 会话
│   │   │   ├── Message.java          # 消息
│   │   │   ├── KnowledgeBase.java    # 知识库
│   │   │   ├── Document.java         # 文档
│   │   │   ├── Workflow.java         # 工作流
│   │   │   └── ...
│   │   ├── mapper/                   # MyBatis 数据访问层
│   │   ├── dto/                      # 数据传输对象
│   │   ├── workflow/                 # 工作流引擎
│   │   │   ├── engine/               # 执行引擎
│   │   │   ├── node/impl/            # 节点实现（LLM、条件、知识库等）
│   │   │   ├── model/                # 工作流模型定义
│   │   │   └── validator/            # 工作流验证器
│   │   └── exception/                # 全局异常处理
│   ├── src/main/resources/
│   │   ├── application-example.yml   # 配置文件示例
│   │   └── db/changelog/             # Liquibase 数据库迁移脚本
│   └── pom.xml                       # Maven 依赖配置
│
├── frontend/                         # 前端 Vue 3 项目
│   ├── src/
│   │   ├── views/                    # 页面组件
│   │   │   ├── ChatView.vue          # 聊天页面
│   │   │   ├── SettingsView.vue      # 设置页面
│   │   │   ├── PromptsView.vue       # 提示词管理
│   │   │   ├── KnowledgeBaseView.vue # 知识库管理
│   │   │   ├── DocumentManageView.vue # 文档管理
│   │   │   └── WorkflowBuilderView.vue # 工作流编排器
│   │   ├── components/               # 可复用组件
│   │   │   └── workflow/             # 工作流相关组件
│   │   │       ├── WorkflowCanvas.vue # 画布
│   │   │       ├── NodeCard.vue      # 节点卡片
│   │   │       ├── ConfigPanel.vue   # 配置面板
│   │   │       └── config/           # 各类节点配置组件
│   │   ├── stores/                   # Pinia 状态管理
│   │   │   ├── chat.ts               # 聊天状态
│   │   │   ├── session.ts            # 会话状态
│   │   │   └── model.ts              # 模型状态
│   │   ├── api/                      # API 接口封装
│   │   │   └── index.ts              # 统一 API 导出
│   │   ├── types/                    # TypeScript 类型定义
│   │   ├── router/                   # Vue Router 路由配置
│   │   └── styles/                   # 全局样式
│   ├── package.json                  # npm 依赖配置
│   └── vite.config.ts                # Vite 构建配置
│
├── CONTRIBUTING.md                   # 贡献指南
└── README.md                         # 项目说明
```

## 数据库表结构

项目使用 Liquibase 自动管理数据库表，主要包含：

| 表名 | 说明 |
|------|------|
| session | 会话信息 |
| message | 聊天消息 |
| model_provider | 模型提供商配置 |
| prompt_template | 提示词模板 |
| knowledge_base | 知识库 |
| document | 知识库文档 |
| document_chunk | 文档切片 |
| workflow | 工作流定义 |
| workflow_execution | 工作流执行记录 |
