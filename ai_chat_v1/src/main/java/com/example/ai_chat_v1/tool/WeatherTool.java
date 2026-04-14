package com.example.ai_chat_v1.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    // 👇 核心魔法：这个注解就是大模型看的“说明书”
    @Tool("获取指定城市的实时天气情况")
    public String getWeather(String city) {

        // 打印一行日志，方便我们在后台监控大模型是不是偷偷调用了这个方法
        System.out.println("🔧 [Agent 触发] 大模型正在调用天气工具，查询城市：" + city);

        // 模拟调用外部天气 API 的返回结果（企业里这里会真实去发 HTTP 请求查天气）
        if (city.contains("北京")) {
            return "北京今天：晴转多云，气温 15°C ~ 25°C，适合户外活动。";
        } else if (city.contains("上海")) {
            return "上海今天：阵雨，气温 20°C ~ 26°C，出门请务必带伞！";
        } else if (city.contains("广州") || city.contains("深圳")) {
            return city + "今天：雷阵雨，气温 25°C ~ 32°C，非常闷热。";
        } else {
            return city + "今天：天气晴朗，气温 22°C，微风不燥。";
        }
    }
}