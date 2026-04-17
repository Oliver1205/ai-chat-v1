package com.example.ai_chat_v1.service;

import org.springframework.stereotype.Component;

@Component
public class ReferencePromptBuilder {

    public String build(String userMessage, String referenceInfo) {
        return "下面是一些【参考资料】。请仔细阅读并判断它们是否与我的问题真正相关。\n" +
                "1. 如果相关，请结合资料进行回答。\n" +
                "2. 🚨【极其重要】如果参考资料与我的问题【毫无关系】（例如我问天气、日期、时间或人物，资料却是公司规章或 Wi-Fi），请【完全忽略】这些资料，直接回答问题！\n" +
                "3. 🚨【禁止事项】如果参考资料无关，【绝对不要】在回答里提“参考资料无关”“根据资料”“资料中没有提到”等说明，直接自然回答。\n\n" +
                "【参考资料】\n" + referenceInfo + "\n\n" +
                "【我的问题】\n" + userMessage;
    }
}