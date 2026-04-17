# Changelog

## 2026-04-17

### Summary
完成了一轮较完整的架构渐进式优化与前端产品化升级，使 ai-chat-v1 从“可运行 demo”进一步提升为“更像真实 AI 产品的个人项目”。

### Backend
- 拆分原本职责较重的 `ChatController`
- 新增 `KnowledgeController`，独立承接知识库上传与状态查询
- 新增 `SessionController`，独立承接会话管理相关接口
- 抽出 `SessionService`，收拢会话查询、创建、重命名等逻辑
- 持续瘦身 `LlmChatService`，将多个子职责逐步拆分为独立组件：
    - `ReferencePromptBuilder`
    - `TimeQuestionHandler`
    - `ChatToolManager`
    - `ChatMessagePreparer`
    - `SessionAutoTitleTrigger`
    - `ChatSessionContextService`
- 主聊天服务职责更聚焦，整体结构更适合后续继续扩展 RAG、Tool Calling 与 Agent 能力

### Frontend
- 重构聊天界面为更接近真实 AI 产品的风格
- 优化左侧边栏、顶部状态区、主聊天区域、输入区、设置抽屉
- 补齐欢迎页、空会话状态、无历史记录状态、加载状态、流式输出状态、网络错误状态
- 整体视觉风格向 Claude / Linear / Vercel 的融合设计靠拢
- 页面更适合桌面端展示与录屏演示，也兼顾平板宽度下的布局变化

### Fixes
- 修复会话切换时界面乱跳、旧请求覆盖新请求的问题
- 增加请求序号控制，避免异步返回顺序打乱界面
- 使用 `AbortController` 中断过期请求
- 切换会话时主动关闭旧的流式连接
- 增加消息缓存，减少切换时的闪烁和重复加载

### Notes
- 本次更新重点在“渐进式优化”，没有推倒重写核心业务逻辑
- 当前版本已经更适合用于 GitHub 展示、项目录屏、简历项目讲解