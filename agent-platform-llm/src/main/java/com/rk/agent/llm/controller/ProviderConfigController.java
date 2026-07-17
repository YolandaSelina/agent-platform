package com.rk.agent.llm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rk.agent.common.entity.PageResult;
import com.rk.agent.common.result.Result;
import com.rk.agent.common.security.SecurityContextHolder;
import com.rk.agent.llm.client.LlmClientFactory;
import com.rk.agent.llm.dto.LlmChatRequest;
import com.rk.agent.llm.dto.LlmChatResponse;
import com.rk.agent.llm.dto.ProviderConfigSaveDTO;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import com.rk.agent.llm.service.LlmService;
import com.rk.agent.llm.service.ProviderConfigService;
import com.rk.agent.llm.util.KeyEncryptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 提供商配置（页面配置）
 * - 系统级：projectId = NULL（所有人共享）
 * - 项目级：projectId = X（仅该项目使用）
 * 优先级：项目级 > 系统级
 */
@Tag(name = "LLM 配置管理")
@RestController
@RequestMapping("/llm-config")
@RequiredArgsConstructor
public class ProviderConfigController {

    private final ProviderConfigService configService;
    private final LlmClientFactory clientFactory;
    private final LlmService llmService;

    @Operation(summary = "分页查询配置")
    @GetMapping("/page")
    public Result<PageResult<ProviderConfigEntity>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String keyword) {
        IPage<ProviderConfigEntity> page = configService.page(current, size, platform, projectId, keyword);
        // 列表里只显示 masked key（先解密成明文再 mask，避免对密文做 substring）
        page.getRecords().forEach(c -> {
            if (c.getApiKey() != null && !c.getApiKey().isBlank()) {
                try {
                    String plain = KeyEncryptor.decrypt(c.getApiKey());
                    c.setApiKey(KeyMask(plain));
                } catch (Exception e) {
                    // 解密失败（老数据 / 明文）直接 mask 原值
                    c.setApiKey(KeyMask(c.getApiKey()));
                }
            }
        });
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "配置详情")
    @GetMapping("/{id}")
    public Result<ProviderConfigEntity> detail(@PathVariable Long id) {
        ProviderConfigEntity c = configService.getById(id);
        if (c == null) return Result.error(404, "配置不存在");
        // 不返回真实 key
        c.setApiKey("****");
        return Result.success(c);
    }

    @Operation(summary = "保存/更新配置")
    @PostMapping
    public Result<ProviderConfigEntity> save(@Valid @RequestBody ProviderConfigSaveDTO dto) {
        ProviderConfigEntity entity = dto.toEntity();
        if (dto.getId() != null) entity.setId(dto.getId());
        var u = SecurityContextHolder.required();
        return Result.success(configService.saveConfig(entity, u.getUserId(), u.getUsername()));
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "显示真实 API Key（仅 ADMIN）")
    @GetMapping("/{id}/reveal")
    public Result<String> reveal(@PathVariable Long id) {
        var u = SecurityContextHolder.required();
        // 简化：只允许 ADMIN 看（系统级权限校验）
        if (u.getRoles() == null || !u.getRoles().contains("ADMIN")) {
            return Result.error(403, "无权限查看 API Key");
        }
        return Result.success(configService.revealKey(id));
    }

    @Operation(summary = "测试配置连通性（发送一个简单请求）")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> test(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body) {
        ProviderConfigEntity c = configService.getById(id);
        if (c == null) return Result.error(404, "配置不存在");
        String prompt = body != null && body.get("prompt") != null ? body.get("prompt") : "你好，请用一句话自我介绍。";
        try {
            LlmChatResponse resp = llmService.chatWithConfig(c.getId(), c.getModel(), c.getProjectId(),
                    LlmChatRequest.builder()
                            .platform(c.getPlatform())
                            .model(c.getModel())
                            .messages(List.of(LlmChatRequest.Message.builder().role("user").content(prompt).build()))
                            .temperature(0.5)
                            .maxTokens(200)
                            .build());
            Map<String, Object> result = new HashMap<>();
            result.put("success", resp.isSuccess());
            result.put("content", resp.getContent());
            result.put("latencyMs", resp.getLatencyMs());
            result.put("model", resp.getModel());
            result.put("usage", resp.getUsage());
            if (!resp.isSuccess()) {
                result.put("error", resp.getErrorMessage());
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "测试失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询生效的配置（按 platform/model/projectId 解析）")
    @GetMapping("/resolve")
    public Result<Map<String, Object>> resolve(@RequestParam String platform,
                                                @RequestParam(required = false) String model,
                                                @RequestParam(required = false) Long projectId) {
        LlmClientFactory.LlmChatConfig cfg = clientFactory.resolve(platform, model, projectId);
        Map<String, Object> data = new HashMap<>();
        data.put("platform", platform);
        data.put("model", cfg.model());
        data.put("baseUrl", cfg.baseUrl());
        data.put("apiKey", cfg.apiKey() != null ? KeyMask(cfg.apiKey()) : null);
        data.put("apiKeySet", cfg.isValid());
        data.put("source", "RESOLVED");
        return Result.success(data);
    }

    @Operation(summary = "列出已注册的客户端平台")
    @GetMapping("/platforms")
    public Result<List<String>> platforms() {
        return Result.success(clientFactory.listPlatforms());
    }

    @Operation(summary = "列出某平台下已配置的 model 列表（DB 配置 + 内置 yml 默认）")
    @GetMapping("/models")
    public Result<List<String>> models(@RequestParam String platform) {
        return Result.success(configService.listModelsByPlatform(platform));
    }

    @Operation(summary = "列出某平台下所有模型/配置")
    @GetMapping("/by-platform")
    public Result<List<ProviderConfigEntity>> byPlatform(@RequestParam String platform,
                                                          @RequestParam(required = false) Long projectId) {
        List<ProviderConfigEntity> list = configService.listByPlatformAndProject(platform, projectId);
        // 同样先解密再 mask
        list.forEach(c -> {
            if (c.getApiKey() != null && !c.getApiKey().isBlank()) {
                try {
                    String plain = KeyEncryptor.decrypt(c.getApiKey());
                    c.setApiKey(KeyMask(plain));
                } catch (Exception e) {
                    c.setApiKey(KeyMask(c.getApiKey()));
                }
            }
        });
        return Result.success(list);
    }

    private String KeyMask(String key) {
        if (key == null || key.isBlank()) return null;
        if (key.length() < 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
