package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Anthropic Claude 客户端
 */
@Slf4j
@Component
public class AnthropicClient implements LlmClient {

    private final AgentProperties.Llm.ProviderConfig config;
    private final ObjectMapper objectMapper;
    private final OkHttpClient http;

    public AnthropicClient(AgentProperties props, ObjectMapper objectMapper) {
        this.config = props.getLlm().getAnthropic();
        this.objectMapper = objectMapper;
        int timeout = config.getTimeoutSeconds() == null ? 120 : Math.max(config.getTimeoutSeconds(), 30);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String platform() {
        return "ANTHROPIC";
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long start = System.currentTimeMillis();
        try {
            if (config.getApiKey() == null || config.getApiKey().isBlank() || "REPLACE_ME".equalsIgnoreCase(config.getApiKey())) {
                throw new BizException(ResultCode.LLM_CALL_FAILED, "Anthropic API Key 未配置");
            }
            String model = request.getModel() != null ? request.getModel() : config.getDefaultModel();

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);
            if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
            if (request.getTopP() != null) body.put("top_p", request.getTopP());
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                body.put("system", request.getSystemPrompt());
            }

            List<Map<String, Object>> msgs = new ArrayList<>();
            if (request.getMessages() != null) {
                for (LlmChatRequest.Message m : request.getMessages()) {
                    if ("system".equalsIgnoreCase(m.getRole())) continue;
                    Map<String, Object> mm = new HashMap<>();
                    mm.put("role", m.getRole());
                    mm.put("content", m.getContent() != null ? m.getContent() : "");
                    msgs.add(mm);
                }
            }
            body.put("messages", msgs);

            if (request.getTools() != null && !request.getTools().isEmpty()) {
                List<Map<String, Object>> tools = new ArrayList<>();
                for (Map<String, Object> t : request.getTools()) {
                    Map<String, Object> fn = (Map<String, Object>) t.getOrDefault("function", t);
                    Map<String, Object> tool = new HashMap<>();
                    tool.put("name", fn.get("name"));
                    tool.put("description", fn.getOrDefault("description", ""));
                    tool.put("input_schema", fn.getOrDefault("parameters", Map.of("type", "object")));
                    tools.add(tool);
                }
                body.put("tools", tools);
            }

            Request httpReq = new Request.Builder()
                    .url(config.getBaseUrl() + "/v1/messages")
                    .header("x-api-key", config.getApiKey())
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(body), MediaType.parse("application/json")))
                    .build();

            try (Response resp = http.newCall(httpReq).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    log.warn("Anthropic 调用失败 status={} body={}", resp.code(), respBody);
                    return LlmChatResponse.builder()
                            .success(false)
                            .errorCode("HTTP_" + resp.code())
                            .errorMessage(respBody)
                            .platform(platform())
                            .model(model)
                            .latencyMs(System.currentTimeMillis() - start)
                            .build();
                }
                return parseResponse(respBody, model, start);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Anthropic 调用异常", e);
            return LlmChatResponse.builder()
                    .success(false)
                    .errorCode("EXCEPTION")
                    .errorMessage(e.getMessage())
                    .platform(platform())
                    .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @SuppressWarnings("unchecked")
    private LlmChatResponse parseResponse(String body, String model, long start) throws Exception {
        Map<String, Object> root = objectMapper.readValue(body, Map.class);
        List<Map<String, Object>> content = (List<Map<String, Object>>) root.getOrDefault("content", List.of());
        StringBuilder text = new StringBuilder();
        List<LlmChatRequest.ToolCall> toolCalls = null;
        String stopReason = (String) root.get("stop_reason");
        for (Map<String, Object> c : content) {
            String type = (String) c.get("type");
            if ("text".equals(type)) {
                Object t = c.get("text");
                if (t != null) text.append(t);
            } else if ("tool_use".equals(type)) {
                if (toolCalls == null) toolCalls = new ArrayList<>();
                LlmChatRequest.ToolCall call = LlmChatRequest.ToolCall.builder()
                        .id((String) c.get("id"))
                        .type("function")
                        .function(LlmChatRequest.ToolFunction.builder()
                                .name((String) c.get("name"))
                                .arguments(c.get("input") == null ? "{}" : objectMapper.writeValueAsString(c.get("input")))
                                .build())
                        .build();
                toolCalls.add(call);
            }
        }
        Map<String, Object> usage = (Map<String, Object>) root.get("usage");
        LlmChatResponse.Usage u = null;
        if (usage != null) {
            u = LlmChatResponse.Usage.builder()
                    .promptTokens(asInt(usage.get("input_tokens")))
                    .completionTokens(asInt(usage.get("output_tokens")))
                    .totalTokens(asInt(usage.get("input_tokens")) + asInt(usage.get("output_tokens")))
                    .build();
        }
        return LlmChatResponse.builder()
                .success(true)
                .content(text.toString())
                .finishReason(stopReason)
                .toolCalls(toolCalls)
                .usage(u)
                .platform(platform())
                .model(model)
                .latencyMs(System.currentTimeMillis() - start)
                .requestId((String) root.get("id"))
                .raw(root)
                .build();
    }

    private Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
