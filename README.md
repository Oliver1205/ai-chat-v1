# 🚀 企业级全栈 AI 助手 (V5.1 持久记忆版)

这是一个基于 **Spring Boot 3.x**、**LangChain4j** 和 **MySQL** 构建的端到端 AI 对话系统。它不仅支持 RAG（检索增强生成）知识库，还具备 Agent（工具调用）能力，并通过数据库实现了完美的对话持久化。

---

<img width="1920" height="989" alt="image" src="https://github.com/user-attachments/assets/f6c5a715-51d7-4293-9ea2-0eb4b5291f63" />


## ✨ 核心特性

- 🤖 **大模型集成**：深度集成 DeepSeek-V3 流式对话，响应速度快，思维逻辑强。
- 💾 **多会话持久化**：使用 MySQL 存储会话与聊天记录，支持“关机再开”记忆不丢失。
- 📱 **微信级 UI**：响应式布局，具备左侧会话切换和右侧左右交替式聊天气泡。
- 📚 **RAG 知识库**：支持 PDF 异步上传与向量处理，AI 会基于你的文档进行回答。
- 🛠️ **Agent 工具化**：预挂载天气查询等 Tool，AI 会根据需求自动调用外部 API。
- 🐳 **容器化部署**：一键式 `docker-compose` 启动环境，无需手动配置繁琐的数据库。

---

## 🛠️ 技术栈

| 维度 | 技术选型 |
| :--- | :--- |
| **后端框架** | Spring Boot 3.5.x |
| **AI 框架** | LangChain4j (1.12.2-beta) |
| **持久层** | Spring Data JPA + MySQL 8.0 |
| **向量库** | BGE-Small-ZH (本地 Embeddings) |
| **前端** | HTML5 + CSS3 (Flexbox) + JavaScript (ES6) |
| **部署** | Docker & Docker Compose |

---

## 🚀 快速开始

### 1. 环境准备
确保你的电脑已安装：
- JDK 17+
- Docker Desktop
- Maven 3.6+

### 2. 配置 API KEY
在 `src/main/resources/application.yml` 中配置你的 DeepSeek API Key，或者设置环境变量：
```yaml
langchain4j:
  open-ai:
    streaming-chat-model:
      api-key: ${DEEPSEEK_API_KEY}
