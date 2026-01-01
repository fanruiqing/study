# AI 助手工具箱 - 知识库和智能体编排器

## 🎯 项目概述

本项目为 AI 助手系统添加了两个重要模块：

1. **知识库模块** ✅ - RAG（检索增强生成）功能，已实现85%
2. **可视化智能体编排器** 📋 - 拖拽式工作流设计器，已完成规范设计

## 📦 交付内容

### 1. 完整的技术规范

```
.kiro/specs/
├── knowledge-base/
│   ├── requirements.md    # 8个需求，40+验收标准
│   ├── design.md          # 完整架构设计，19个正确性属性
│   └── tasks.md           # 24个实施任务
└── visual-agent-builder/
    ├── requirements.md    # 12个需求，60+验收标准
    ├── design.md          # 完整架构设计，31个正确性属性
    └── tasks.md           # 36个实施任务
```

### 2. 知识库模块实现

#### 后端代码（85%完成）
```
backend/src/main/java/com/ai/assistant/
├── entity/                    # 5个实体类
│   ├── KnowledgeBase.java
│   ├── Document.java
│   ├── DocumentChunk.java
│   └── SessionKnowledgeBase.java
├── mapper/                    # 5个Mapper
├── service/                   # 9个服务类
│   ├── KnowledgeBaseService.java
│   ├── DocumentService.java
│   ├── DocumentProcessorService.java
│   ├── TextChunkingService.java
│   ├── VectorStoreService.java
│   ├── EmbeddingService.java
│   ├── DocumentVectorizationService.java
│   ├── RAGService.java
│   └── KnowledgeBaseChatService.java
├── controller/                # 2个控制器
│   ├── KnowledgeBaseController.java
│   └── DocumentController.java
└── config/                    # 2个配置类
    ├── VectorStoreConfig.java
    └── AsyncConfig.java
```

#### 前端代码（80%完成）
```
frontend/src/
├── views/
│   ├── KnowledgeBaseView.vue      # 知识库管理页面
│   └── DocumentManageView.vue     # 文档管理页面
└── router/
    └── index.ts                    # 路由配置
```

### 3. 文档和指南

- ✅ `知识库模块实现进度.md` - 详细的实现进度
- ✅ `知识库模块快速启动指南.md` - 快速开始指南
- ✅ `完整实施总结.md` - 完整的实施总结
- ✅ `可视化智能体编排器-简化实施说明.md` - 编排器实施指南
- ✅ `最终验收文档.md` - 验收文档
- ✅ `test-knowledge-base.sh` - Linux/Mac测试脚本
- ✅ `test-knowledge-base.bat` - Windows测试脚本

## 🚀 快速开始

### 前置条件

- Java 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+
- OpenAI API Key

### 1. 配置环境变量

```bash
export OPENAI_API_KEY="your-api-key"
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 4. 访问应用

- 前端：http://localhost:5173
- 后端API：http://localhost:8080

### 5. 测试功能

#### Linux/Mac:
```bash
chmod +x test-knowledge-base.sh
./test-knowledge-base.sh
```

#### Windows:
```cmd
test-knowledge-base.bat
```

## 📚 核心功能

### 知识库模块 ✅

#### 已实现功能

1. **知识库管理**
   - ✅ 创建知识库
   - ✅ 编辑知识库
   - ✅ 删除知识库
   - ✅ 查看知识库列表

2. **文档管理**
   - ✅ 上传文档（PDF、Word、TXT、MD）
   - ✅ 自动文本提取
   - ✅ 自动分块
   - ✅ 自动向量化
   - ✅ 查看文档状态
   - ✅ 查看文档块
   - ✅ 删除文档

3. **语义检索**
   - ✅ 向量相似度搜索
   - ✅ Top-K检索
   - ✅ 知识库范围过滤

4. **RAG对话**
   - ✅ 自动检索相关知识
   - ✅ 上下文注入
   - ✅ 增强回答生成

5. **会话关联**
   - ✅ 知识库与会话关联
   - ✅ 多知识库支持

#### 待完善功能

- ⏳ 引用来源可视化
- ⏳ 文档标签筛选
- ⏳ 统计监控面板
- ⏳ 增量更新
- ⏳ 批量操作

### 可视化智能体编排器 📋

#### 已完成

- ✅ 完整的需求文档
- ✅ 完整的设计文档
- ✅ 详细的任务列表
- ✅ 实施指南

#### 待实现

- ⏳ 工作流引擎
- ⏳ 节点类型实现
- ⏳ Vue Flow画布
- ⏳ 执行监控
- ⏳ 前端界面

## 🎨 界面预览

### 知识库管理页面
- 知识库列表展示
- 创建/编辑对话框
- 卡片式布局

### 文档管理页面
- 文档列表表格
- 上传对话框
- 状态实时更新
- 文档块查看

### 导航菜单
- 新增知识库图标
- 路由跳转
- 高亮显示

## 📖 API文档

### 知识库管理

```
POST   /api/knowledge-bases                    创建知识库
GET    /api/knowledge-bases                    获取知识库列表
GET    /api/knowledge-bases/{id}               获取知识库详情
PUT    /api/knowledge-bases/{id}               更新知识库
DELETE /api/knowledge-bases/{id}               删除知识库
POST   /api/knowledge-bases/{id}/associate-session  关联会话
GET    /api/knowledge-bases/session/{sessionId}     获取会话的知识库
```

### 文档管理

```
POST   /api/documents/upload                   上传文档
GET    /api/documents?knowledgeBaseId=xxx      获取文档列表
GET    /api/documents/{id}                     获取文档详情
DELETE /api/documents/{id}                     删除文档
GET    /api/documents/{id}/status              获取处理状态
GET    /api/documents/{id}/chunks              获取文档分块
```

### 知识库对话

```
POST   /api/chat/with-knowledge                带知识库的对话
GET    /api/chat/{messageId}/citations         获取消息引用
```

## 🔧 技术栈

### 后端
- Spring Boot 3.2
- Spring AI (OpenAI + Simple Vector Store)
- MyBatis-Plus
- MySQL
- Apache PDFBox (PDF处理)
- Apache POI (Word处理)
- Liquibase (数据库迁移)

### 前端
- Vue 3 + TypeScript
- Element Plus
- Vue Router
- Axios

### 向量存储
- Spring AI Simple Vector Store
  - 基于JSON文件
  - 零配置
  - 适合中小规模应用

## 📊 项目统计

### 代码量

| 模块 | 文件数 | 代码行数（估算） |
|------|--------|------------------|
| 后端实体 | 5 | ~200 |
| 后端服务 | 9 | ~1500 |
| 后端控制器 | 2 | ~300 |
| 前端页面 | 2 | ~800 |
| 配置文件 | 3 | ~200 |
| **总计** | **21** | **~3000** |

### 功能完成度

| 模块 | 完成度 | 说明 |
|------|--------|------|
| 知识库后端 | 85% | 核心功能完成 |
| 知识库前端 | 80% | 主要界面完成 |
| 智能体编排器 | 0% | 仅完成规范 |

## 🎯 下一步计划

### 短期（1-2周）
1. 完善引用来源展示
2. 添加文档标签筛选
3. 优化错误处理
4. 性能测试和优化

### 中期（1-2月）
1. 实现统计监控面板
2. 添加增量更新功能
3. 实现批量操作
4. 升级向量数据库（可选）

### 长期（3-6月）
1. 实现可视化智能体编排器
2. 或集成现有工具（Dify/Langflow）
3. 分布式部署支持
4. 高级功能扩展

## 🐛 已知问题

1. Simple Vector Store不支持按元数据批量删除
2. 不返回相似度分数
3. 文件存储在本地磁盘
4. 单机部署

## 🔄 升级路径

### 向量数据库升级

可以无缝升级到：
- Chroma（推荐）
- PgVector
- Milvus

只需更换VectorStore实现，接口保持不变。

## 📞 技术支持

### 查看日志

```bash
# 应用日志
tail -f logs/spring.log

# 文档处理日志
tail -f logs/spring.log | grep "DocumentVectorizationService"

# 检索日志
tail -f logs/spring.log | grep "RAGService"
```

### 数据库检查

```sql
-- 查看知识库
SELECT * FROM knowledge_bases;

-- 查看文档
SELECT * FROM documents;

-- 查看文档块
SELECT * FROM document_chunks;
```

### 常见问题

**Q: 文档一直处于PENDING状态？**
A: 检查OpenAI API Key配置和网络连接

**Q: 检索不到相关内容？**
A: 确认文档已处理完成（COMPLETED状态）

**Q: 如何升级向量数据库？**
A: 参考设计文档中的升级指南

## 📄 许可证

本项目遵循原项目的许可证。

## 🙏 致谢

感谢以下开源项目：
- Spring AI
- Vue.js
- Element Plus
- Apache PDFBox
- Apache POI

---

**项目状态**：知识库模块可用于生产环境测试 ✅

**最后更新**：2024年12月
