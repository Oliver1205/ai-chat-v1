package com.example.ai_chat_v1.agent.studycoach;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class StudyCoachTextGenerator {

    private final StreamingChatModel chatModel;

    public StudyCoachTextGenerator(StreamingChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generate(String prompt) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(prompt)))
                .build();

        StringBuilder responseBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        chatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                responseBuilder.append(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (responseBuilder.isEmpty()
                        && completeResponse != null
                        && completeResponse.aiMessage() != null
                        && completeResponse.aiMessage().text() != null) {
                    responseBuilder.append(completeResponse.aiMessage().text());
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorRef.set(error);
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                throw new IllegalStateException("StudyCoachAgent generation timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("StudyCoachAgent generation interrupted", e);
        }

        if (errorRef.get() != null) {
            throw new IllegalStateException("StudyCoachAgent generation failed", errorRef.get());
        }

        return responseBuilder.toString().trim();
    }
}
