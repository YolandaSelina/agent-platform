package com.rk.agent.api.controller;

import com.rk.agent.api.config.GatewayProperties;
import com.rk.agent.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 简易 API Gateway：通过 OkHttp 代理将请求转发到对应服务
 * 前端只需对接 :9080
 *
 * 路由规则（按路径前缀匹配）：
 *   /api/auth/**       -> 9081 auth
 *   /api/project/**    -> 9082 project
 *   /api/pipeline/**   -> 9083 pipeline
 *   /api/llm/**        -> 9084 llm
 *   /api/task/**       -> 9082 project
 *   /api/page/**       -> 9082 project
 *   /api/document/**   -> 9082 project
 *   /api/agent-template/** -> 9083 pipeline
 *   /api/tool/**       -> 9083 pipeline
 */
@Slf4j
@Tag(name = "API Gateway")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatewayController {

    private final GatewayProperties gatewayProperties;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private String resolveTarget(String path) {
        // 配置优先
        for (Map.Entry<String, String> e : gatewayProperties.getRoutes().entrySet()) {
            if (path.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        // 默认路由表（兼容 /api/xxx 和 /api/xxx/ 两种形式）
        if (path.equals("/api/auth") || path.startsWith("/api/auth/")) return "http://localhost:9081";
        if (path.equals("/api/project") || path.startsWith("/api/project/")) return "http://localhost:9082";
        if (path.equals("/api/task") || path.startsWith("/api/task/")) return "http://localhost:9082";
        if (path.equals("/api/page") || path.startsWith("/api/page/")) return "http://localhost:9082";
        if (path.equals("/api/document") || path.startsWith("/api/document/")) return "http://localhost:9082";
        if (path.equals("/api/pipeline") || path.startsWith("/api/pipeline/")) return "http://localhost:9083";
        if (path.equals("/api/pipeline-template") || path.startsWith("/api/pipeline-template/")) return "http://localhost:9083";
        if (path.equals("/api/agent-template") || path.startsWith("/api/agent-template/")) return "http://localhost:9083";
        if (path.equals("/api/pipeline-webhook") || path.startsWith("/api/pipeline-webhook/") || path.startsWith("/api/pipeline-webhook")) return "http://localhost:9083";
        if (path.equals("/api/stats") || path.startsWith("/api/stats/")) return "http://localhost:9083";
        if (path.equals("/api/search") || path.startsWith("/api/search/")) return "http://localhost:9082";
        if (path.equals("/api/tool") || path.startsWith("/api/tool/")) return "http://localhost:9083";
        if (path.equals("/api/llm") || path.startsWith("/api/llm/")) return "http://localhost:9084";
        if (path.equals("/api/llm-config") || path.startsWith("/api/llm-config/")) return "http://localhost:9084";
        if (path.equals("/api/chat") || path.startsWith("/api/chat/")) return "http://localhost:9084";
        return null;
    }

    @RequestMapping("/**")
    @Operation(hidden = true)
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        String target = resolveTarget(path);
        if (target == null) {
            writeJson(response, HttpStatus.NOT_FOUND, Result.error(404, "No route for: " + path));
            return;
        }
        // 转发到目标服务时去掉 /api 前缀（因为具体服务的 controller 路径不带 /api）
        String targetPath = path.startsWith("/api/") ? path.substring(4) : path;
        if (targetPath.isEmpty()) targetPath = "/";
        String queryString = request.getQueryString();
        String url = target + targetPath + (queryString != null ? "?" + queryString : "");
        log.debug("Gateway: {} {} -> {}", request.getMethod(), path, url);

        try {
            // 构建 OkHttp 请求
            okhttp3.Request.Builder rb = new okhttp3.Request.Builder().url(url);

            // 复制请求头（过滤 hop-by-hop 和 host）
            Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String lname = name.toLowerCase();
                if ("host".equals(lname) || "content-length".equals(lname) || "connection".equals(lname)) continue;
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    rb.addHeader(name, values.nextElement());
                }
            }

            // 读取 body
            byte[] body = null;
            try (InputStream in = request.getInputStream()) {
                body = in.readAllBytes();
            } catch (Exception ignored) {
                // 无 body
            }

            // 构造 body
            okhttp3.RequestBody requestBody;
            String contentType = request.getContentType();
            okhttp3.MediaType mediaType = (contentType != null) ? okhttp3.MediaType.parse(contentType) : null;
            if (body != null && body.length > 0) {
                if (mediaType != null) {
                    requestBody = okhttp3.RequestBody.create(body, mediaType);
                } else {
                    requestBody = okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/octet-stream"));
                }
            } else {
                // OkHttp 对 POST/PUT/PATCH 不允许 null body，给一个空 body
                if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod())) {
                    requestBody = okhttp3.RequestBody.create(new byte[0], okhttp3.MediaType.parse("application/octet-stream"));
                } else {
                    requestBody = null;
                }
            }

            rb.method(request.getMethod(), requestBody);

            try (okhttp3.Response okResp = httpClient.newCall(rb.build()).execute()) {
                // 写回响应状态
                response.setStatus(okResp.code());
                // 写回响应头
                for (String name : okResp.headers().names()) {
                    if ("content-encoding".equalsIgnoreCase(name) || "transfer-encoding".equalsIgnoreCase(name)
                            || "content-length".equalsIgnoreCase(name) || "connection".equalsIgnoreCase(name)) continue;
                    for (String value : okResp.headers(name)) {
                        response.addHeader(name, value);
                    }
                }
                // 写 body
                if (okResp.body() != null) {
                    try (InputStream is = okResp.body().byteStream();
                         java.io.OutputStream os = response.getOutputStream()) {
                        is.transferTo(os);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gateway proxy error: {} -> {}: {}", path, url, e.getMessage());
            try {
                writeJson(response, HttpStatus.BAD_GATEWAY, Result.error(502, "Upstream error: " + e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object obj) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        try (java.io.PrintWriter w = response.getWriter()) {
            w.write(com.rk.agent.common.util.JsonUtils.toJson(obj));
            w.flush();
        }
    }

    @GetMapping("/_health")
    @Operation(summary = "Gateway 健康检查")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("routes", gatewayProperties.getRoutes());
        return Result.success(data);
    }
}
