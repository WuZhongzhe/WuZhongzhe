package com.team.agent.tools;

import com.team.agent.util.Http;
import com.team.agent.util.Json;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 天气工具，数据来自 Open-Meteo 开放接口（无需密钥）。
 * 智能体拿到天气数据后，再结合模型能力生成出行建议。
 */
public class WeatherTools {

    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";

    public List<Tool> tools() {
        return Arrays.asList(new GetWeather());
    }

    /** 查询指定城市当前天气与未来两天的概况。 */
    public static class GetWeather implements Tool {

        @Override
        public String name() {
            return "get_weather";
        }

        @Override
        public String description() {
            return "查询指定城市的当前天气与未来两天预报，返回温度、天气现象、降水概率和风力，用于生成出行计划。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create()
                    .property("city", "string", "城市名称，例如 广州、北京、Shanghai")
                    .required("city")
                    .build();
        }

        @Override
        public String execute(Map<String, Object> arguments) throws Exception {
            String city = Json.text(arguments, "city");
            if (city == null || city.trim().isEmpty()) {
                return "缺少必要参数：city";
            }
            double[] location = geocode(city.trim());
            return forecast(city.trim(), location[0], location[1]);
        }

        private double[] geocode(String city) throws IOException, InterruptedException {
            String url = GEO_URL + "?name=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&count=1&language=zh&format=json";
            Object parsed = Json.parse(Http.get(url));
            List<Object> results = Json.asArray(Json.asObject(parsed).get("results"));
            if (results.isEmpty()) {
                throw new IOException("没有找到城市：" + city);
            }
            Map<String, Object> first = Json.asObject(results.get(0));
            return new double[]{
                    ((Number) first.get("latitude")).doubleValue(),
                    ((Number) first.get("longitude")).doubleValue()
            };
        }

        private String forecast(String city, double latitude, double longitude) throws IOException, InterruptedException {
            String url = FORECAST_URL + "?latitude=" + latitude + "&longitude=" + longitude
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code"
                    + "&timezone=Asia%2FShanghai&forecast_days=3";
            Map<String, Object> data = Json.asObject(Json.parse(Http.get(url)));
            Map<String, Object> current = Json.asObject(data.get("current"));
            Map<String, Object> daily = Json.asObject(data.get("daily"));
            List<Object> maxList = Json.asArray(daily.get("temperature_2m_max"));
            List<Object> minList = Json.asArray(daily.get("temperature_2m_min"));
            List<Object> rainList = Json.asArray(daily.get("precipitation_probability_max"));
            List<Object> codeList = Json.asArray(daily.get("weather_code"));
            List<Object> dates = Json.asArray(daily.get("time"));

            StringBuilder sb = new StringBuilder();
            sb.append(city).append("当前：")
                    .append(current.get("temperature_2m")).append("℃，")
                    .append(describeWeather(((Number) current.get("weather_code")).intValue())).append("，")
                    .append("湿度 ").append(current.get("relative_humidity_2m")).append("%，")
                    .append("风速 ").append(current.get("wind_speed_10m")).append(" km/h\n");
            sb.append("未来三天：\n");
            for (int i = 1; i < dates.size(); i++) {
                sb.append(dates.get(i)).append("：")
                        .append(minList.get(i)).append("~").append(maxList.get(i)).append("℃，")
                        .append(describeWeather(((Number) codeList.get(i)).intValue())).append("，")
                        .append("降水概率 ").append(rainList.get(i)).append("%\n");
            }
            return sb.toString().trim();
        }

        /** 将 WMO 天气代码转换为中文描述。 */
        private static String describeWeather(int code) {
            switch (code) {
                case 0:
                    return "晴";
                case 1:
                case 2:
                    return "少云";
                case 3:
                    return "阴";
                case 45:
                case 48:
                    return "雾";
                case 51:
                case 53:
                case 55:
                case 56:
                case 57:
                    return "毛毛雨";
                case 61:
                case 63:
                case 65:
                case 66:
                case 67:
                    return "降雨";
                case 71:
                case 73:
                case 75:
                case 77:
                    return "降雪";
                case 80:
                case 81:
                case 82:
                    return "阵雨";
                case 85:
                case 86:
                    return "阵雪";
                case 95:
                case 96:
                case 99:
                    return "雷阵雨";
                default:
                    return "未知天气";
            }
        }
    }
}
