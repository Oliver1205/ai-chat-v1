# 🚀 ai-chat-v1｜企业级全栈 AI 助手（持久记忆版）

一个基于 **Spring Boot 3.x**、**LangChain4j**、**MySQL** 构建的端到端 AI 对话系统。  
它不仅支持 **流式对话**、**RAG 检索增强生成**、**Agent 工具调用**，还通过数据库实现了 **多会话持久化记忆**，并逐步打磨为一个更接近真实产品形态的 AI 应用项目。

---

## ✨ 项目简介

`ai-chat-v1` 是一个从 0 到 1 持续迭代的个人 AI 应用项目，目标不是只做一个“能跑起来的 Demo”，而是尽量完成一个：

- 架构清晰
- 可持续扩展
- 前后端分离
- 具备产品展示感
- 适合写进简历 / GitHub 展示 / 项目录屏讲解

的 AI 助手系统。

当前版本已经具备基础聊天能力、知识库问答能力、工具调用能力，以及基于 MySQL 的会话与消息持久化能力，并在近期持续完成了后端职责拆分与前端产品化升级。

---

## 🔥 核心特性

- 🤖 **大模型集成**  
  深度集成 DeepSeek 系列模型，支持流式输出，响应自然，交互体验更接近真实 AI 产品。

- 💾 **多会话持久化**  
  使用 MySQL 存储会话与聊天记录，支持历史会话管理，“关机再开”记忆不丢失。

- 📚 **RAG 知识库能力**  
  支持 PDF 文档异步上传、解析、切块、向量化与检索，AI 可以基于本地知识进行增强回答。

- 🛠️ **Agent / Tool Calling**  
  预留并接入天气查询等工具能力，模型可在需要时自动调用外部工具完成任务。

- 🧠 **持久记忆体验**  
  对话历史与会话上下文统一管理，为后续进一步增强 Memory / Agent 能力打下基础。

- 🎨 **产品化前端界面**  
  聊天界面已从基础 Demo 风格逐步升级为更接近真实 AI 产品的展示风格，具备侧边栏、多会话、欢迎页、空状态、加载态、流式态、错误态等完整体验。

- 🐳 **容器化部署**  
  支持通过 `docker-compose` 快速拉起依赖环境，降低本地启动成本。

---

## 🏗️ 技术栈

| 维度 | 技术选型 |
| :--- | :--- |
| **后端框架** | Spring Boot 3.5.x |
| **AI 框架** | LangChain4j 1.12.2-beta |
| **持久层** | Spring Data JPA + MySQL 8.0 |
| **向量能力** | BGE-Small-ZH（本地 Embeddings） |
| **前端** | HTML5 + CSS3 + JavaScript（ES6） |
| **通信方式** | SSE（Server-Sent Events）流式输出 |
| **部署方式** | Docker / Docker Compose |

---

## 🧩 当前能力结构

### 1. Chat 对话主链路
- 支持流式对话输出
- 支持多轮对话上下文承接
- 支持会话创建、切换、标题生成

### 2. Session 会话管理
- 会话列表查询
- 会话消息回显
- 新建对话
- 重命名会话
- 会话上下文持久化

### 3. RAG 知识增强
- PDF 上传
- 文档解析
- 文本切块
- 本地向量化
- 检索增强回答

### 4. Tool Calling
- 天气工具调用
- 时间类问题特殊处理
- 为后续更多工具扩展预留能力入口

### 5. 产品化前端
- 左侧会话侧边栏
- 主聊天区域
- 欢迎页 / 空状态 / 加载态 / 错误态
- 输入区产品化设计
- 设置 / 扩展抽屉预留

---

## 🆕 最近更新

### 2026-04-17｜聊天架构渐进式优化 + 产品级前端升级

#### 后端部分
- 拆分原本职责较重的 `ChatController`
- 新增：
    - `KnowledgeController`
    - `SessionController`
    - `SessionService`
- 持续瘦身 `LlmChatService`，将多个职责逐步拆分为独立组件：
    - `ReferencePromptBuilder`
    - `TimeQuestionHandler`
    - `ChatToolManager`
    - `ChatMessagePreparer`
    - `SessionAutoTitleTrigger`
    - `ChatSessionContextService`

#### 前端部分
- 将原有聊天页面升级为更接近真实 AI 产品的界面风格
- 重构：
    - 左侧边栏
    - 顶部状态区
    - 主聊天区
    - 产品化输入区
    - 设置抽屉
- 补齐：
    - 欢迎页
    - 空会话状态
    - 无历史记录状态
    - 加载状态
    - 流式输出状态
    - 网络错误状态

#### 交互与稳定性优化
- 修复会话切换时界面乱跳问题
- 增加请求序号控制，避免旧请求覆盖新请求
- 使用 `AbortController` 中断过期请求
- 切换会话时主动关闭旧流式连接
- 增加消息缓存，减少切换时的闪烁与重复加载

#### 本次更新价值
这次更新的重点不是“单纯把页面变好看”，而是让项目整体更接近一个真正可展示、可讲解、可继续扩展的 AI 产品。

---

### 2026-04-22 - StudyCoachAgent v1

#### 新增
- 新增 `StudyCoachAgent` 第一版，定位为学习教练型任务 Agent
- 新增基于 ReAct 思路的调度链路：意图判断、记忆读取、知识检索、草稿生成、计划评估、结果修正、最终格式化输出
- 新增 `MemoryReadTool`，可从当前会话历史中提取相关学习上下文
- 新增 `LearningKnowledgeSearchTool`，复用现有知识库检索能力
- 新增 `PlanEvaluateTool`，用于判断学习计划是否具体、可执行、是否过载、优先级是否清晰
- 新增 StudyCoach 相关 DTO、Evaluator、Orchestrator、Formatter、PromptFactory 等模块

#### 改动
- 扩展聊天入口 `/api/chat`，新增 `mode` 参数支持 `study-coach` 模式
- 前端新增最小模式切换入口，可在“通用对话”和 `StudyCoachAgent` 之间切换
- 前端补充 StudyCoach 欢迎态、标题文案和输入提示
- 保持现有 SSE、多会话、会话持久化和自动标题能力不变，采用渐进式增强接入 Agent

#### 测试
- 新增 `StudyCoachIntentAnalyzerTest`
- 新增 `StudyCoachPlanEvaluatorTest`
- 调整测试以保证本地无额外环境变量时也可通过基础测试
- 本地执行 `./mvnw.cmd test` 已通过

#### 第一版能力边界
- 聚焦学习规划、学习复盘、动态调整下一步、知识点辅助解释
- 暂不引入多 Agent
- 暂不实现复杂长期记忆体系
- 继续复用现有项目的 RAG、会话和流式输出基础设施

## 📁 项目结构（示意）

```text
src/main/java/com/example/ai_chat_v1
├── controller
│   ├── ChatController
│   ├── SessionController
│   └── KnowledgeController
├── service
│   ├── LlmChatService
│   ├── SessionService
│   ├── ChatToolManager
│   ├── ChatMessagePreparer
│   ├── ChatSessionContextService
│   ├── TimeQuestionHandler
│   ├── ReferencePromptBuilder
│   └── SessionAutoTitleTrigger
├── repository
├── entity
├── dto
├── config
└── tool
