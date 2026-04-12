package com.example.ai_chat_v1.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzhv15.BgeSmallZhV15EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class KnowledgeBaseManager {

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private final EmbeddingModel embeddingModel = new BgeSmallZhV15EmbeddingModel();

    // 👇 新增 1：生产进度看板。记录每个文件处理到哪一步了，方便前台实时查询
    private final Map<String, String> documentStatusMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        System.out.println("⏳ 正在初始化本地知识库...");
        // 这里保留了你之前写的默认测试数据，保证项目启动不报错
        String secretText = "【公司内部绝密规章】\n1. 公司的 Wi-Fi 密码是：Ragent2026!，严禁告诉外人。";
        Document document = Document.from(secretText);
        List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(document);
        embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);
        System.out.println("✅ 默认知识库加载完毕！");
    }

    // 4. 检索方法（保持你的最新版写法不变）
    public String search(String question) {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingModel.embed(question).content())
                .maxResults(2)
                .minScore(0.3)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        return searchResult.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n\n"));
    }

    // 👇 新增 2：让前台查询某个文件处理状态的接口
    public String getStatus(String fileId) {
        return documentStatusMap.getOrDefault(fileId, "未知状态");
    }

    // 👇 新增 3：真正的异步流水线核心方法！
    // @Async 告诉 Spring：这个方法不要在主线程运行，去叫我们刚才配置的工人来干活！
    @Async("knowledgeBaseExecutor")
    public void processPdfAsync(String fileId, String fileName, byte[] fileBytes) {
        try {
            documentStatusMap.put(fileId, "⏳ 正在解析 PDF 内容...");

            // 步骤 A: 把字节数组变回文件流，并用官方 PDF 解析器读取
            InputStream inputStream = new ByteArrayInputStream(fileBytes);
            DocumentParser parser = new ApachePdfBoxDocumentParser();
            Document document = parser.parse(inputStream);

            documentStatusMap.put(fileId, "🔪 正在切分段落与向量化 (这步最耗时)...");

            // 步骤 B: 切块（Chunking）
            List<TextSegment> segments = DocumentSplitters.recursive(300, 50).split(document);

            // 步骤 C: 翻译成向量并入库（Embedding）
            embeddingStore.addAll(embeddingModel.embedAll(segments).content(), segments);

            documentStatusMap.put(fileId, "✅ 处理完成！已成功将 " + segments.size() + " 个知识块存入大脑。");
            System.out.println("🚀 后台车间汇报：文件 [" + fileName + "] 入库成功！");

        } catch (Exception e) {
            documentStatusMap.put(fileId, "❌ 处理失败：" + e.getMessage());
            System.err.println("❌ 后台车间报错：文件 [" + fileName + "] 处理失败！");
            e.printStackTrace();
        }
    }
}