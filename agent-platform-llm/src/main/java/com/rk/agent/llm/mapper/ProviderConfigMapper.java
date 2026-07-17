package com.rk.agent.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rk.agent.llm.entity.ProviderConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProviderConfigMapper extends BaseMapper<ProviderConfigEntity> {

    /**
     * 按优先级查找匹配的配置
     * 1. project + platform + model
     * 2. project + platform + model=NULL
     * 3. system + platform + model
     * 4. system + platform + model=NULL
     * 取 enabled=1 中 priority 最高 + id 最新
     */
    @Select("""
        SELECT * FROM llm_provider_config
        WHERE platform = #{platform}
          AND deleted = 0
          AND enabled = 1
          AND (
            (project_id = #{projectId} AND (model = #{model} OR (model IS NULL AND #{model} IS NOT NULL) OR (model = #{model})))
            OR (project_id = #{projectId} AND model IS NULL AND #{model} IS NOT NULL)
            OR (project_id IS NULL AND (model = #{model} OR (model IS NULL AND #{model} IS NOT NULL) OR (model = #{model})))
            OR (project_id IS NULL AND model IS NULL AND #{model} IS NOT NULL)
          )
        ORDER BY (CASE WHEN project_id = #{projectId} THEN 1 ELSE 0 END) DESC,
                 (CASE WHEN model = #{model} THEN 1 ELSE 0 END) DESC,
                 priority DESC, id DESC
        LIMIT 1
    """)
    ProviderConfigEntity findBestMatch(String platform, String model, Long projectId);

    /**
     * 按平台 + 项目列出所有配置
     */
    @Select("""
        SELECT * FROM llm_provider_config
        WHERE platform = #{platform}
          AND (project_id = #{projectId} OR project_id IS NULL)
          AND deleted = 0
        ORDER BY (CASE WHEN project_id = #{projectId} THEN 1 ELSE 0 END) DESC,
                 (CASE WHEN model = #{model} THEN 1 ELSE 0 END) DESC,
                 priority DESC, id DESC
    """)
    List<ProviderConfigEntity> listByPlatformAndProject(String platform, Long projectId, String model);

    /**
     * 列出某平台下已配置的所有 model（去重；含内置 client 默认 model）
     */
    @Select("""
        SELECT DISTINCT model FROM llm_provider_config
        WHERE platform = #{platform} AND deleted = 0 AND model IS NOT NULL AND model != ''
        ORDER BY model
    """)
    List<String> listModelsByPlatform(String platform);
}
