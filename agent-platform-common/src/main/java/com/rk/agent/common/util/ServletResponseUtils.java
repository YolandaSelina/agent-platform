package com.rk.agent.common.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应工具
 */
public final class ServletResponseUtils {

    private ServletResponseUtils() {
    }

    public static void writeJson(HttpServletResponse response, Object obj) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter writer = response.getWriter()) {
            writer.write(JsonUtils.toJson(obj));
            writer.flush();
        }
    }

    public static void writeSseEvent(HttpServletResponse response, String event, String data) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        try (PrintWriter writer = response.getWriter()) {
            if (event != null) {
                writer.write("event: " + event + "\n");
            }
            writer.write("data: " + data + "\n\n");
            writer.flush();
        }
    }
}
