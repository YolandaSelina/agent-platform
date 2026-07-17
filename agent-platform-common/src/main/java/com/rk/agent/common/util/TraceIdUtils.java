package com.rk.agent.common.util;

import java.util.UUID;

/**
 * TraceId 工具
 */
public final class TraceIdUtils {

    public static final String MDC_KEY = "traceId";

    private TraceIdUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
