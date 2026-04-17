package com.example.ai_chat_v1.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class WeatherTool {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String amapWebApiKey;

    public WeatherTool(@Value("${amap.web-api-key}") String amapWebApiKey,
                       ObjectMapper objectMapper) {
        this.amapWebApiKey = amapWebApiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://restapi.amap.com")
                .build();
    }

    @Tool("获取指定中国城市最近几天的天气情况和出行建议。输入参数应为城市名，例如：广州、深圳、上海、北京。")
    public String getWeather(String city) {
        System.out.println("🔧 [Agent 触发] 大模型正在调用高德天气工具，查询城市：" + city);

        if (city == null || city.isBlank()) {
            return "查询失败：城市不能为空。";
        }

        try {
            String cleanCity = city.trim();
            String adcode = resolveAdcode(cleanCity);

            JsonNode liveRoot = queryWeather(adcode, "base");
            JsonNode forecastRoot = queryWeather(adcode, "all");

            JsonNode live = getFirstElement(liveRoot, "lives");
            JsonNode forecast = getFirstElementFlexible(forecastRoot, "forecasts", "forecast");
            JsonNode casts = forecast.path("casts");

            String province = textOrDefault(live, "province", "");
            String cityName = textOrDefault(live, "city", cleanCity);
            String weather = textOrDefault(live, "weather", "未知");
            String temperature = textOrDefault(live, "temperature", "未知");
            String humidity = textOrDefault(live, "humidity", "未知");
            String windDirection = textOrDefault(live, "winddirection", "未知");
            String windPower = textOrDefault(live, "windpower", "未知");
            String liveReportTime = textOrDefault(live, "reporttime", "未知");

            StringBuilder sb = new StringBuilder();
            sb.append(cityName);
            if (!province.isBlank() && !province.equals(cityName)) {
                sb.append("（").append(province).append("）");
            }
            sb.append("当前天气如下：\n")
                    .append("- 天气：").append(weather).append("\n")
                    .append("- 气温：").append(temperature).append("℃\n")
                    .append("- 湿度：").append(humidity).append("%\n")
                    .append("- 风向：").append(windDirection).append("\n")
                    .append("- 风力：").append(windPower).append("级\n")
                    .append("- 实况发布时间：").append(liveReportTime).append("\n");

            if (casts.isArray() && !casts.isEmpty()) {
                sb.append("\n未来天气预报：\n");

                List<String> suggestions = new ArrayList<>();

                for (int i = 0; i < casts.size(); i++) {
                    JsonNode cast = casts.get(i);
                    String date = textOrDefault(cast, "date", "未知日期");
                    String dayWeather = textOrDefault(cast, "dayweather", "未知");
                    String nightWeather = textOrDefault(cast, "nightweather", "未知");
                    String dayTemp = textOrDefault(cast, "daytemp", "?");
                    String nightTemp = textOrDefault(cast, "nighttemp", "?");
                    String dayWind = textOrDefault(cast, "daywind", "未知");
                    String dayPower = textOrDefault(cast, "daypower", "未知");

                    sb.append(i + 1)
                            .append(". ")
                            .append(date)
                            .append("：白天")
                            .append(dayWeather)
                            .append("，夜间")
                            .append(nightWeather)
                            .append("，")
                            .append(nightTemp).append("℃~").append(dayTemp).append("℃")
                            .append("，白天风向").append(dayWind)
                            .append("，风力").append(dayPower).append("级")
                            .append("\n");

                    collectSuggestion(suggestions, dayWeather, nightWeather, dayTemp, nightTemp);
                }

                if (!suggestions.isEmpty()) {
                    sb.append("\n出行建议：\n");
                    for (int i = 0; i < suggestions.size(); i++) {
                        sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
                    }
                }

                String forecastReportTime = textOrDefault(forecast, "reporttime", "");
                if (!forecastReportTime.isBlank()) {
                    sb.append("\n预报发布时间：").append(forecastReportTime);
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "查询 " + city + " 天气失败：" + e.getMessage();
        }
    }

    private String resolveAdcode(String city) throws Exception {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/geocode/geo")
                        .queryParam("key", amapWebApiKey)
                        .queryParam("address", city)
                        .queryParam("output", "JSON")
                        .build())
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(body);
        validateAmapSuccess(root, "城市编码解析");

        JsonNode geocodes = root.path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            throw new IllegalStateException("未找到城市「" + city + "」对应的高德编码");
        }

        String adcode = geocodes.get(0).path("adcode").asText("");
        if (adcode.isBlank()) {
            throw new IllegalStateException("城市「" + city + "」缺少 adcode");
        }

        return adcode;
    }

    private JsonNode queryWeather(String adcode, String extensions) throws Exception {
        String body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/weather/weatherInfo")
                        .queryParam("key", amapWebApiKey)
                        .queryParam("city", adcode)
                        .queryParam("extensions", extensions)
                        .queryParam("output", "JSON")
                        .build())
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(body);
        validateAmapSuccess(root, "天气查询");
        return root;
    }

    private void validateAmapSuccess(JsonNode root, String actionName) {
        String status = root.path("status").asText("");
        if (!"1".equals(status)) {
            String info = root.path("info").asText("未知错误");
            String infocode = root.path("infocode").asText("未知错误码");
            throw new IllegalStateException(actionName + "失败：info=" + info + ", infocode=" + infocode);
        }
    }

    private JsonNode getFirstElement(JsonNode root, String arrayFieldName) {
        JsonNode arrayNode = root.path(arrayFieldName);
        if (arrayNode.isArray() && !arrayNode.isEmpty()) {
            return arrayNode.get(0);
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode getFirstElementFlexible(JsonNode root, String firstField, String secondField) {
        JsonNode first = root.path(firstField);
        if (first.isArray() && !first.isEmpty()) {
            return first.get(0);
        }

        JsonNode second = root.path(secondField);
        if (second.isArray() && !second.isEmpty()) {
            return second.get(0);
        }

        return objectMapper.createObjectNode();
    }

    private String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            return defaultValue;
        }
        String text = field.asText("");
        return text.isBlank() ? defaultValue : text;
    }

    private void collectSuggestion(List<String> suggestions,
                                   String dayWeather,
                                   String nightWeather,
                                   String dayTemp,
                                   String nightTemp) {
        int maxTemp = parseTemp(dayTemp);
        int minTemp = parseTemp(nightTemp);

        String weatherText = (dayWeather + " " + nightWeather).toLowerCase();

        if ((weatherText.contains("雨") || weatherText.contains("雷")) &&
                suggestions.stream().noneMatch(s -> s.contains("雨具"))) {
            suggestions.add("近期有降雨或雷阵雨，出门建议携带雨具，并尽量避开强对流天气时段。");
        }

        if (maxTemp >= 32 &&
                suggestions.stream().noneMatch(s -> s.contains("防暑") || s.contains("补水"))) {
            suggestions.add("白天气温较高，注意防暑降温，尽量避免长时间暴晒并及时补水。");
        }

        if (minTemp != Integer.MIN_VALUE && maxTemp != Integer.MIN_VALUE && maxTemp - minTemp >= 8 &&
                suggestions.stream().noneMatch(s -> s.contains("温差"))) {
            suggestions.add("昼夜温差偏大，建议采用可增减的穿搭。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("整体天气可正常出行，建议根据实时温度和降水情况灵活调整穿着。");
        }
    }

    private int parseTemp(String temp) {
        try {
            return Integer.parseInt(temp.trim());
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }
}