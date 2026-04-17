package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.tool.WeatherTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.springframework.stereotype.Component;

@Component
public class ChatToolManager {

    private final WeatherTool weatherTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatToolManager(WeatherTool weatherTool) {
        this.weatherTool = weatherTool;
    }

    public ChatRequest.Builder applyToolSpecifications(ChatRequest.Builder builder) {
        return builder.toolSpecifications(
                ToolSpecifications.toolSpecificationsFrom(weatherTool)
        );
    }

    public ToolExecutionResultMessage execute(ToolExecutionRequest toolReq) throws Exception {
        String toolName = toolReq.name();

        if ("getWeather".equals(toolName)) {
            JsonNode args = objectMapper.readTree(toolReq.arguments());
            String city = args.has("city") ? args.get("city").asText() : "";
            String toolResult = weatherTool.getWeather(city);
            return ToolExecutionResultMessage.from(toolReq, toolResult);
        }

        throw new IllegalStateException("暂不支持的工具调用：" + toolName);
    }
}