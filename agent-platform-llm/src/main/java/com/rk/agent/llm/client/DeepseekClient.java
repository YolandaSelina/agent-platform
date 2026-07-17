package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek（兼容 OpenAI 协议）
 */
@Component
public class DeepseekClient extends AbstractOpenAiCompatibleClient {

    public DeepseekClient(AgentProperties props, ObjectMapper objectMapper) {
        super(props.getLlm().getDeepseek(), objectMapper);
    }

    @Override
    public String platform() {
        return "DEEPSEEK";
    }
}
