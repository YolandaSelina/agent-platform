package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 兼容协议客户端（支持 OpenAI / 通义千问兼容模式 / DeepSeek）
 */
@Slf4j
public abstract class AbstractOpenAiCompatibleClient implements LlmClient {

    protected final AgentProperties.Llm.ProviderConfig config;
    protected volatile String overrideBaseUrl;
    protected volatile String overrideApiKey;
    protected volatile String overrideModel;
    protected final OkHttpClient http;
    protected final ObjectMapper objectMapper;

    @Override
    public void overrideBaseUrl(String baseUrl) {
        this.overrideBaseUrl = baseUrl;
    }

    @Override
    public void overrideApiKey(String apiKey) {
        this.overrideApiKey = apiKey;
    }

    @Override
    public void overrideModel(String model) {
        this.overrideModel = model;
    }

    protected String currentBaseUrl() {
        return overrideBaseUrl != null ? overrideBaseUrl : config.getBaseUrl();
    }

    protected String getApiKey() {
        if (overrideApiKey != null && !overrideApiKey.isBlank()) return overrideApiKey;
        String key = config.getApiKey();
        if (key == null || key.isBlank() || "REPLACE_ME".equalsIgnoreCase(key)) {
            throw new BizException(ResultCode.LLM_CALL_FAILED, "API Key 未配置（请在 application.yml 设置）");
        }
        return key;
    }

    protected String getDefaultModel() {
        if (overrideModel != null && !overrideModel.isBlank()) return overrideModel;
        return config.getDefaultModel();
    }

    protected AbstractOpenAiCompatibleClient(AgentProperties.Llm.ProviderConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        int timeout = config.getTimeoutSeconds() == null ? 120 : Math.max(config.getTimeoutSeconds(), 30);
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = buildBody(request);
            Request httpReq = new Request.Builder()
                    .url(currentBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(objectMapper.writeValueAsBytes(body), MediaType.parse("application/json")))
                    .build();
            try (Response resp = http.newCall(httpReq).execute()) {
                String respBody = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    log.warn("LLM 调用失败 status={} body={}", resp.code(), respBody);
                    return LlmChatResponse.builder()
                            .success(false)
                            .errorCode("HTTP_" + resp.code())
                            .errorMessage(respBody)
                            .platform(platform())
                            .model(request.getModel() != null ? request.getModel() : getDefaultModel())
                            .latencyMs(System.currentTimeMillis() - start)
                            .build();
                }
                return parseResponse(respBody, request, start);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 调用异常", e);
            return LlmChatResponse.builder()
                    .success(false)
                    .errorCode("EXCEPTION")
                    .errorMessage(e.getMessage())
                    .platform(platform())
                    .model(request.getModel() != null ? request.getModel() : getDefaultModel())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    protected Map<String, Object> buildBody(LlmChatRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : getDefaultModel());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        body.put("stream", false);
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
            if (request.getToolChoice() != null) {
                body.put("tool_choice", request.getToolChoice());
            }
        }
        List<Map<String, Object>> msgs = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            msgs.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        if (request.getMessages() != null) {
            for (LlmChatRequest.Message m : request.getMessages()) {
                Map<String, Object> mm = new HashMap<>();
                mm.put("role", m.getRole());
                mm.put("content", m.getContent() != null ? m.getContent() : "");
                if (m.getName() != null) mm.put("name", m.getName());
                if (m.getToolCallId() != null) mm.put("tool_call_id", m.getToolCallId());
                if (m.getToolCalls() != null) mm.put("tool_calls", m.getToolCalls());
                msgs.add(mm);
            }
        }
        body.put("messages", msgs);
        return body;
    }

    @SuppressWarnings("unchecked")
    protected LlmChatResponse parseResponse(String respBody, LlmChatRequest request, long start) throws IOException {
        Map<String, Object> root = objectMapper.readValue(respBody, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) root.getOrDefault("choices", List.of());
        String content = "";
        String finishReason = null;
        List<LlmChatRequest.ToolCall> toolCalls = null;
        if (!choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> msg = (Map<String, Object>) choice.get("message");
            if (msg != null) {
                Object c = msg.get("content");
                content = c == null ? "" : c.toString();
                Object tcs = msg.get("tool_calls");
                if (tcs instanceof List<?> list) {
                    toolCalls = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> tc) {
                            Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                            Object typeObj = tc.get("type");
                            String callType = typeObj == null ? "function" : typeObj.toString();
                            LlmChatRequest.ToolCall call = LlmChatRequest.ToolCall.builder()
                                    .id((String) tc.get("id"))
                                    .type(callType)
                                    .function(LlmChatRequest.ToolFunction.builder()
                                            .name(fn != null ? (String) fn.get("name") : null)
                                            .arguments(fn != null ? String.valueOf(fn.get("arguments")) : null)
                                            .build())
                                    .build();
                            toolCalls.add(call);
                        }
                    }
                }
            }
            finishReason = (String) choice.get("finish_reason");
        }
        Map<String, Object> usage = (Map<String, Object>) root.get("usage");
        LlmChatResponse.Usage u = null;
        if (usage != null) {
            u = LlmChatResponse.Usage.builder()
                    .promptTokens(asInt(usage.get("prompt_tokens")))
                    .completionTokens(asInt(usage.get("completion_tokens")))
                    .totalTokens(asInt(usage.get("total_tokens")))
                    .build();
        }
        return LlmChatResponse.builder()
                .success(true)
                .content(content)
                .finishReason(finishReason)
                .toolCalls(toolCalls)
                .usage(u)
                .platform(platform())
                .model(request.getModel() != null ? request.getModel() : getDefaultModel())
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
