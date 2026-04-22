package com.example.ai_chat_v1.agent.studycoach.tool;

import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.LearningKnowledgeSearchResult;
import com.example.ai_chat_v1.service.KnowledgeBaseManager;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class LearningKnowledgeSearchTool {

    private final KnowledgeBaseManager knowledgeBaseManager;

    public LearningKnowledgeSearchTool(KnowledgeBaseManager knowledgeBaseManager) {
        this.knowledgeBaseManager = knowledgeBaseManager;
    }

    public LearningKnowledgeSearchResult execute(LearningKnowledgeSearchRequest request) {
        String raw = knowledgeBaseManager.search(request.query());
        if (raw == null || raw.isBlank()) {
            return LearningKnowledgeSearchResult.empty();
        }

        List<String> fragments = Arrays.stream(raw.split("\\n\\n+"))
                .map(String::trim)
                .filter(fragment -> !fragment.isBlank())
                .map(this::truncateFragment)
                .limit(3)
                .toList();

        if (fragments.isEmpty()) {
            return LearningKnowledgeSearchResult.empty();
        }

        return new LearningKnowledgeSearchResult(String.join("\n", fragments), fragments);
    }

    private String truncateFragment(String fragment) {
        if (fragment.length() <= 240) {
            return fragment;
        }
        return fragment.substring(0, 240) + "...";
    }
}
