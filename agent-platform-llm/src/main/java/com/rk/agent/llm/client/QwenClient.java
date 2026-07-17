package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import org.springframework.stereotype.Component;

/**
 * 通义千问（兼容 OpenAI 协议）
 */
@Component
public class QwenClient extends AbstractOpenAiCompatibleClient {

    public QwenClient(AgentProperties props, ObjectMapper objectMapper) {
        super(props.getLlm().getQwen(), objectMapper);
    }

    @Override
    public String platform() {
        return "QWEN";
    }
}
