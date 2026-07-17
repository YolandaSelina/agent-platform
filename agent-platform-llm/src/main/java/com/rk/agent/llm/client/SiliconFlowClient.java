package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import org.springframework.stereotype.Component;

/**
 * 硅基流动（OpenAI 兼容协议）
 * 默认模型：Qwen/Qwen2.5-72B-Instruct
 */
@Component
public class SiliconFlowClient extends AbstractOpenAiCompatibleClient {

    public SiliconFlowClient(AgentProperties props, ObjectMapper objectMapper) {
        super(props.getLlm().getSiliconflow(), objectMapper);
    }

    @Override
    public String platform() {
        return "SILICONFLOW";
    }
}
