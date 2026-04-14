package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.tool.WeatherTool; // 👇 新增导入：我们的武器库
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class LlmChatService {

    private final StreamingChatModel chatModel;
    private final ChatMemoryManager memoryManager;
    private final KnowledgeBaseManager knowledgeBaseManager;

    // 👇 新增 1：引入武器库和 JSON 解析器
    private final WeatherTool weatherTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 👇 新增 2：构造器注入
    public LlmChatService(StreamingChatModel chatModel, ChatMemoryManager memoryManager, KnowledgeBaseManager knowledgeBaseManager, WeatherTool weatherTool) {
        this.chatModel = chatModel;
        this.memoryManager = memoryManager;
        this.knowledgeBaseManager = knowledgeBaseManager;
        this.weatherTool = weatherTool;
    }

    public void streamChat(String sessionId,
                           String userMessage,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Throwable> onError) {

        if (userMessage == null || userMessage.isBlank()) {
            onError.accept(new IllegalArgumentException("消息不能为空"));
            return;
        }

        // --- RAG 检索阶段 (保持你完美的防污染设计不变) ---
        String referenceInfo = knowledgeBaseManager.search(userMessage);
        ChatMemory chatMemory = memoryManager.getOrCreate(sessionId);
        chatMemory.add(UserMessage.from(userMessage));

        List<ChatMessage> messagesToSend = new ArrayList<>(chatMemory.messages());

        if (!referenceInfo.isEmpty()) {
            messagesToSend.remove(messagesToSend.size() - 1);

            // 👇 核心升级：给大模型下达极度严厉的“过滤指令”
            String augmentedText = "下面是一些【参考资料】。请仔细阅读并判断它们是否与我的问题真正相关。\n" +
                    "1. 如果相关，请结合资料进行回答。\n" +
                    "2. 🚨【极其重要】如果参考资料与我的问题【毫无关系】（例如我问天气或人物，资料却是公司规章或Wi-Fi），请【完全忽略】这些资料，直接用你的常识回答！\n" +
                    "3. 🚨【禁止事项】在回答时，【绝对不要】出现“根据提供的参考资料”、“参考资料中没有提到”、“资料显示”等类似的话术，做到自然、顺畅。\n\n" +
                    "【参考资料】\n" + referenceInfo + "\n\n" +
                    "【我的问题】\n" + userMessage;

            messagesToSend.add(UserMessage.from(augmentedText));
        }

        // 👇 新增 3：启动 Agent 核心执行引擎
        executeAgentLoop(chatMemory, messagesToSend, onToken, onComplete, onError);
    }

    // 👇 新增 4：Agent 核心思考与执行循环 (真正的魔法发生在这里)
    private void executeAgentLoop(ChatMemory chatMemory,
                                  List<ChatMessage> messagesToSend,
                                  Consumer<String> onToken,
                                  Runnable onComplete,
                                  Consumer<Throwable> onError) {

        // 组装请求，并把工具箱翻译成“说明书”挂载给大模型
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messagesToSend)
                .toolSpecifications(ToolSpecifications.toolSpecificationsFrom(weatherTool)) // 🔧 挂载武器！
                .build();

        StringBuilder aiMessageBuilder = new StringBuilder();

        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                aiMessageBuilder.append(partialResponse);
                onToken.accept(partialResponse); // 把文字实时推给前台网页
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMessage = completeResponse.aiMessage();
                // 无论 AI 回答了最终文字，还是发出了工具调用指令，都必须存入记忆
                chatMemory.add(aiMessage);

                // 🚨 Agent 拦截器：大模型是不是发出调用工具的请求了？
                if (aiMessage.hasToolExecutionRequests()) {
                    for (ToolExecutionRequest toolReq : aiMessage.toolExecutionRequests()) {

                        // 发现它想调用我们写的 getWeather 方法！
                        if ("getWeather".equals(toolReq.name())) {
                            try {
                                // 步骤 A: 解析大模型传过来的 JSON 参数（例如 {"city": "上海"}）
                                JsonNode args = objectMapper.readTree(toolReq.arguments());
                                String city = args.get("city").asText();

                                // 步骤 B: 真正执行我们的 Java 方法！（后台控制台会打印出那句提示）
                                String toolResult = weatherTool.getWeather(city);

                                // 步骤 C: 将执行结果打包成特殊的消息
                                ToolExecutionResultMessage toolMessage = ToolExecutionResultMessage.from(toolReq, toolResult);

                                // 步骤 D: 把结果塞进记忆和发送列表中
                                chatMemory.add(toolMessage);
                                messagesToSend.add(aiMessage); // 发送列表中必须包含刚才的调用指令
                                messagesToSend.add(toolMessage); // 发送列表中加上我们的执行结果

                            } catch (Exception e) {
                                onError.accept(e);
                                return;
                            }
                        }
                    }

                    // 💡 神之一手（递归调用）：带着拿到真实数据的聊天记录，再次问大模型！
                    executeAgentLoop(chatMemory, messagesToSend, onToken, onComplete, onError);

                } else {
                    // 如果没有工具请求，说明大模型已经拿到了所有信息并生成了最终回答，彻底结束！
                    onComplete.run();
                }
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }
}