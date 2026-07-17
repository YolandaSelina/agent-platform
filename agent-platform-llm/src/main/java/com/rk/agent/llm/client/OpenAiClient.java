package com.rk.agent.llm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rk.agent.common.config.AgentProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI 客户端
 */
@Component
public class OpenAiClient extends AbstractOpenAiCompatibleClient {

    public OpenAiClient(AgentProperties props, ObjectMapper objectMapper) {
        super(props.getLlm().getOpenai(), objectMapper);
    }

    @Override
    public String platform() {
        return "OPENAI";
    }
}
