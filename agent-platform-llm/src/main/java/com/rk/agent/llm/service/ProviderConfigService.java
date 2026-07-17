package com.rk.agent.llm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rk.agent.common.config.AgentProperties;
import com.rk.agent.common.exception.BizException;
import com.rk.agent.common.result.ResultCode;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import com.rk.agent.llm.mapper.ProviderConfigMapper;
import com.rk.agent.llm.util.KeyEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderConfigService extends ServiceImpl<ProviderConfigMapper, ProviderConfigEntity> {

    private final AgentProperties agentProperties;

    public IPage<ProviderConfigEntity> page(Long current, Long size, String platform, Long projectId, String keyword) {
        LambdaQueryWrapper<ProviderConfigEntity> qw = new LambdaQueryWrapper<>();
        if (platform != null && !platform.isBlank()) qw.eq(ProviderConfigEntity::getPlatform, platform);
        if (projectId != null) qw.eq(ProviderConfigEntity::getProjectId, projectId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(ProviderConfigEntity::getName, keyword)
                    .or().like(ProviderConfigEntity::getModel, keyword)
                    .or().like(ProviderConfigEntity::getDescription, keyword));
        }
        qw.orderByAsc(ProviderConfigEntity::getPlatform).orderByAsc(ProviderConfigEntity::getModel);
        return page(new Page<>(current, size), qw);
    }

    /**
     * 保存（加密 API Key）
     */
    public ProviderConfigEntity saveConfig(ProviderConfigEntity entity, Long userId, String userName) {
        if (entity.getId() == null) {
            // 唯一性检查
            ProviderConfigEntity exist = baseMapper.selectOne(new LambdaQueryWrapper<ProviderConfigEntity>()
                    .eq(ProviderConfigEntity::getPlatform, entity.getPlatform())
                    .eq(ProviderConfigEntity::getModel, entity.getModel())
                    .eq(entity.getProjectId() != null, ProviderConfigEntity::getProjectId, entity.getProjectId())
                    .isNull(entity.getProjectId() == null, ProviderConfigEntity::getProjectId)
                    .last("LIMIT 1"));
            if (exist != null) {
                throw new BizException(ResultCode.DATA_ALREADY_EXISTS,
                        "同平台/模型/项目下已存在配置: id=" + exist.getId());
            }
            entity.setCreatorId(userId);
            entity.setCreatorName(userName);
        }
        if (entity.getApiKey() != null && !entity.getApiKey().isBlank()
                && !entity.getApiKey().startsWith("****")) {
            entity.setApiKey(KeyEncryptor.encrypt(entity.getApiKey()));
        }
        if (entity.getEnabled() == null) entity.setEnabled(1);
        if (entity.getTimeoutSeconds() == null) entity.setTimeoutSeconds(120);
        if (entity.getPriority() == null) entity.setPriority(0);
        saveOrUpdate(entity);
        return entity;
    }

    /**
     * 解密 API Key（仅 ADMIN）
     */
    public String revealKey(Long id) {
        ProviderConfigEntity c = getById(id);
        if (c == null) throw new BizException(ResultCode.DATA_NOT_FOUND);
        return KeyEncryptor.decrypt(c.getApiKey());
    }

    /**
     * 按 platform + model + projectId 取最佳（已解密的 key）
     */
    public ProviderConfigEntity findBest(String platform, String model, Long projectId) {
        ProviderConfigEntity c = baseMapper.findBestMatch(platform, model, projectId);
        if (c != null) {
            c.setApiKey(KeyEncryptor.decrypt(c.getApiKey()));
        }
        return c;
    }

    /**
     * 列出某平台+项目的所有配置
     */
    public List<ProviderConfigEntity> listByPlatformAndProject(String platform, Long projectId) {
        return baseMapper.listByPlatformAndProject(platform, projectId, null);
    }

    /**
     * 列出某平台下已配置的 model 列表（来自 DB 配置 + 内置 client 默认 model）
     */
    public List<String> listModelsByPlatform(String platform) {
        if (platform == null || platform.isBlank()) return List.of();
        List<String> dbModels = baseMapper.listModelsByPlatform(platform);
        // 加上内置 client 的默认 model（如果用户还没在 DB 配过）
        String defaultModel = getDefaultModelFromYml(platform);
        java.util.Set<String> all = new java.util.LinkedHashSet<>();
        if (defaultModel != null && !defaultModel.isBlank()) all.add(defaultModel);
        all.addAll(dbModels);
        return new java.util.ArrayList<>(all);
    }

    private String getDefaultModelFromYml(String platform) {
        return switch (platform.toUpperCase()) {
            case "OPENAI" -> agentProperties.getLlm().getOpenai().getDefaultModel();
            case "ANTHROPIC" -> agentProperties.getLlm().getAnthropic().getDefaultModel();
            case "QWEN" -> agentProperties.getLlm().getQwen().getDefaultModel();
            case "DEEPSEEK" -> agentProperties.getLlm().getDeepseek().getDefaultModel();
            case "SILICONFLOW" -> agentProperties.getLlm().getSiliconflow().getDefaultModel();
            default -> null;
        };
    }
}
