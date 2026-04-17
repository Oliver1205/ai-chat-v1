package com.example.ai_chat_v1.service;

import com.example.ai_chat_v1.entity.ChatMessage;
import com.example.ai_chat_v1.entity.ChatSession;
import com.example.ai_chat_v1.repository.ChatMessageRepository;
import com.example.ai_chat_v1.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class SessionTitleService {

    private static final int MAX_AUTO_TITLE_LENGTH = 18;
    private static final int MAX_MANUAL_TITLE_LENGTH = 30;
    private static final int MAX_CONTEXT_CHARS = 1500;
    private static final int MAX_SINGLE_MESSAGE_CHARS = 240;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final StreamingChatModel chatModel;
    private final ObjectMapper objectMapper;

    public SessionTitleService(ChatSessionRepository sessionRepository,
                               ChatMessageRepository messageRepository,
                               StreamingChatModel chatModel,
                               ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensureSessionExists(String sessionId) {
        if (sessionRepository.existsById(sessionId)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setTitle("新对话");
        session.setTitleCustomized(false);
        session.setCreateTime(now);
        session.setLastActiveTime(now);
        sessionRepository.save(session);
    }

    @Transactional
    public void touchSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setLastActiveTime(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    public void tryAutoGenerateTitle(String sessionId) {
        Optional<ChatSession> optional = sessionRepository.findById(sessionId);
        if (optional.isEmpty()) {
            return;
        }

        ChatSession session = optional.get();

        if (Boolean.TRUE.equals(session.getTitleCustomized())) {
            touchSession(sessionId);
            return;
        }

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        String conversationContext = buildConversationContext(messages);
        if (conversationContext.isBlank()) {
            touchSession(sessionId);
            return;
        }

        String generatedTitle = generateTitleByAi(conversationContext);
        if (generatedTitle.isBlank()) {
            touchSession(sessionId);
            return;
        }

        session.setTitle(generatedTitle);
        session.setLastActiveTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public ChatSession renameSession(String sessionId, String rawTitle) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会话不存在"));

        String title = sanitizeManualTitle(rawTitle);
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空");
        }

        session.setTitle(title);
        session.setTitleCustomized(true);
        session.setLastActiveTime(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    private String buildConversationContext(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();

        for (ChatMessage message : messages) {
            String role = normalizeRole(message.getRole());
            if (role == null) {
                continue;
            }

            String text = extractReadableText(message.getContent());
            if (text.isBlank()) {
                continue;
            }

            if (text.length() > MAX_SINGLE_MESSAGE_CHARS) {
                text = text.substring(0, MAX_SINGLE_MESSAGE_CHARS);
            }

            String line = role + "：" + text + "\n";
            if (sb.length() + line.length() > MAX_CONTEXT_CHARS) {
                break;
            }

            sb.append(line);
        }

        return sb.toString().trim();
    }

    private String normalizeRole(String role) {
        if ("USER".equalsIgnoreCase(role)) {
            return "用户";
        }
        if ("AI".equalsIgnoreCase(role)) {
            return "助手";
        }
        return null;
    }

    private String extractReadableText(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(rawContent);
            String text = "";

            if (root.has("text") && !root.get("text").isNull()) {
                text = root.get("text").asText("");
            } else if (root.has("contents")
                    && root.get("contents").isArray()
                    && !root.get("contents").isEmpty()
                    && root.get("contents").get(0).has("text")) {
                text = root.get("contents").get(0).get("text").asText("");
            }

            return cleanupText(text);
        } catch (Exception e) {
            return cleanupText(rawContent);
        }
    }

    private String cleanupText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String cleaned = text.trim();

        int questionIndex = cleaned.indexOf("【我的问题】");
        if (questionIndex >= 0) {
            cleaned = cleaned.substring(questionIndex + "【我的问题】".length()).trim();
        }

        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    private String generateTitleByAi(String conversationContext) {
        String prompt = """
                你是一个“会话标题生成器”。
                请根据下面的对话内容，生成一个高度概括主题的中文标题。

                要求：
                1. 只输出标题本身，不要解释，不要加引号
                2. 控制在 8~18 个中文字符内
                3. 尽量概括“用户真正想解决的问题”
                4. 不要出现“聊天记录”“新对话”“根据对话”等废话
                5. 如果内容涉及多个点，优先概括最核心主线

                对话内容：
                %s
                """.formatted(conversationContext);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();

        StringBuilder titleBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        chatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                titleBuilder.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                return "";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }

        return sanitizeGeneratedTitle(titleBuilder.toString());
    }

    private String sanitizeGeneratedTitle(String rawTitle) {
        if (rawTitle == null) {
            return "";
        }

        String title = rawTitle.trim()
                .replace("标题：", "")
                .replace("标题:", "")
                .replaceAll("[\\r\\n`\"'“”‘’]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        title = title.replaceAll("[。！？；：,，、]+$", "").trim();

        if (title.length() > MAX_AUTO_TITLE_LENGTH) {
            title = title.substring(0, MAX_AUTO_TITLE_LENGTH);
        }

        return title;
    }

    private String sanitizeManualTitle(String rawTitle) {
        if (rawTitle == null) {
            return "";
        }

        String title = rawTitle.trim().replaceAll("\\s+", " ");
        if (title.length() > MAX_MANUAL_TITLE_LENGTH) {
            title = title.substring(0, MAX_MANUAL_TITLE_LENGTH);
        }
        return title;
    }
}