package com.example.ai_chat_v1.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理员：负责把文档存入数据库，并提供检索功能
 */
@Component
public class KnowledgeBaseManager {

    // 内存向量数据库（微型档案室）
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    // 本地中文化向量模型（翻译官：负责把文字变成数学坐标）
    private final EmbeddingModel embeddingModel = new BgeSmallZhV15EmbeddingModel();

    // @PostConstruct 意思是：当 Spring Boot 刚启动时，自动执行这个方法
    @PostConstruct
    public void init() {
        System.out.println("⏳ 正在初始化本地知识库，首次启动可能会下载约100MB模型文件，请稍候...");

        // 1. 准备我们的“绝密档案” (真实企业开发中，这里通常是从 PDF 或 Word 读取的)
        String secretText = "【公司内部绝密规章】\n" +
                "1. 公司的 Wi-Fi 密码是：Ragent2026!，严禁告诉外人。\n" +
                "2. 报销流程：所有报销凭证请在每周三下午找财务部的老李签字审核。\n" +
                "3. 公司正在秘密研发的核心 AI 产品代号为“盘古”，预计明年第三季度发布。\n" +
                "4. 食堂的糖醋排骨最好吃，但只有每周五中午才供应，请提前排队。";
        Document document = Document.from(secretText);

        // 2. 切块（Chunking）：把一大段文字切成一小段一小段（这里按最多300字切）
        List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(document);

        // 3. 向量化并入库：把切好的小段落翻译成向量，放进内存档案室
        embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);

        System.out.println("✅ 知识库加载完毕！共分成了 " + segments.size() + " 个知识块。");
    }

    // 4. 检索方法：外部传入一个问题，我返回最相关的参考资料
// 4. 检索方法：外部传入一个问题，我返回最相关的参考资料
    public String search(String question) {
        // 【新版 API 写法】构造一个标准的“查询请求”
        dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest = dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(question).content()) // 把问题翻译成数学坐标
                .maxResults(2) // 找最相关的 Top 2
                .minScore(0.6) // 相似度及格线（0到1之间）
                .build();

        // 去档案室里搜寻，拿到搜寻结果
        dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // 把捞出来的文字拼接到一起返回
        return searchResult.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));
    }
}