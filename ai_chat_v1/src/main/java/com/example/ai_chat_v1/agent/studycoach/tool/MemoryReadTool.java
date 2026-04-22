package com.example.ai_chat_v1.agent.studycoach.tool;

import com.example.ai_chat_v1.agent.studycoach.access.StudyCoachMemoryAccessService;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadRequest;
import com.example.ai_chat_v1.agent.studycoach.dto.MemoryReadResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemoryReadTool {

    private final StudyCoachMemoryAccessService memoryAccessService;

    public MemoryReadTool(StudyCoachMemoryAccessService memoryAccessService) {
        this.memoryAccessService = memoryAccessService;
    }

    public MemoryReadResult execute(MemoryReadRequest request) {
        List<String> hits = memoryAccessService.readRelevantMemoryLines(
                request.sessionId(),
                request.currentQuestion()
        );

        if (hits.isEmpty()) {
            return MemoryReadResult.empty();
        }

        return new MemoryReadResult(String.join("\n", hits), hits);
    }
}
