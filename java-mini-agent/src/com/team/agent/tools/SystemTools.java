package com.team.agent.tools;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 系统类工具，为智能体提供时间等运行时信息。
 */
public class SystemTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<Tool> tools() {
        return Arrays.asList(new CurrentTime(), new SystemInfo());
    }

    public static class CurrentTime implements Tool {

        @Override
        public String name() {
            return "current_time";
        }

        @Override
        public String description() {
            return "获取当前日期、星期与时间，用于回答“今天几号”“现在几点”等问题。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create().build();
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            LocalDateTime now = LocalDateTime.now();
            String[] weekdays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
            int index = now.getDayOfWeek().getValue() - 1;
            return now.format(FORMATTER) + " " + weekdays[index];
        }
    }

    public static class SystemInfo implements Tool {

        @Override
        public String name() {
            return "system_info";
        }

        @Override
        public String description() {
            return "获取运行环境信息，包括操作系统、Java 版本、可用处理器数与 JVM 内存情况。";
        }

        @Override
        public Map<String, Object> parameters() {
            return Schema.create().build();
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            Runtime runtime = Runtime.getRuntime();
            return "操作系统：" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n"
                    + "Java 版本：" + System.getProperty("java.version") + "\n"
                    + "处理器数量：" + runtime.availableProcessors() + "\n"
                    + "JVM 最大内存：" + runtime.maxMemory() / 1024 / 1024 + " MB";
        }
    }
}
