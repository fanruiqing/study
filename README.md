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
3. 编辑 `application.yml`，填入你的数据库连接信息和 API Key

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

1. 启动应用后，进入「设置」页面
2. 点击「添加模型」
3. 填写模型信息：
   - 名称：如 "GPT-4"
   - 类型：选择 OpenAI/Claude/通义千问/自定义
   - API Key：你的 API 密钥
   - Base URL：API 地址（可选，默认 OpenAI 官方地址）
   - 模型名称：如 "gpt-4", "gpt-3.5-turbo"

## 功能特性

- ✅ 多模型对话（支持 OpenAI、Claude、通义千问等）
- ✅ 流式响应（SSE）
- ✅ 会话管理（创建、删除、重命名、搜索）
- ✅ Markdown 渲染（代码高亮）
- ✅ 提示词模板管理
- ✅ 数据导入导出
- ✅ 亮色/暗色主题切换
- ✅ 消息操作（复制、重新生成、删除）

## 项目结构

```
├── backend/                 # 后端 Spring Boot 项目
│   ├── src/main/java/
│   │   └── com/ai/assistant/
│   │       ├── controller/  # REST 控制器
│   │       ├── service/     # 业务逻辑
│   │       ├── repository/  # 数据访问
│   │       ├── entity/      # 实体类
│   │       ├── dto/         # 数据传输对象
│   │       └── config/      # 配置类
│   └── pom.xml
│
├── frontend/                # 前端 Vue 3 项目
│   ├── src/
│   │   ├── views/          # 页面组件
│   │   ├── stores/         # Pinia 状态管理
│   │   ├── api/            # API 接口
│   │   ├── types/          # TypeScript 类型
│   │   └── styles/         # 样式文件
│   └── package.json
│
└── README.md
```
