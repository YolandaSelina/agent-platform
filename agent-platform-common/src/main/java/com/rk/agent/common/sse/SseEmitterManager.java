package com.rk.agent.common.sse;

import com.rk.agent.common.util.JsonUtils;
import com.rk.agent.common.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE 推送管理器
 * <p>
 * 内部状态使用 static 字段存储（单 JVM 内全局共享），所有业务方法为 static。
 * 这样心跳线程（也是在 static 块里启动的）能直接调用 send/complete/remove，
 * 不存在"非静态方法不能从静态上下文中调用"的问题。
 */
@Slf4j
@Component
public class SseEmitterManager {

    private static final Map<String, SseEmitter> EMITTERS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService HEARTBEAT = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    static {
        HEARTBEAT.scheduleAtFixedRate(() -> {
            try {
                SseEvent beat = SseEvent.builder()
                        .type(SseEvent.Type.HEARTBEAT)
                        .timestamp(System.currentTimeMillis())
                        .message("ping")
                        .build();
                EMITTERS.forEach((id, em) -> send(id, beat));
            } catch (Exception ex) {
                log.warn("SSE 心跳异常: {}", ex.getMessage());
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    public SseEmitter create(String emitterId, Long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        EMITTERS.put(emitterId, emitter);
        emitter.onCompletion(() -> remove(emitterId));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(emitterId);
        });
        emitter.onError(t -> remove(emitterId));
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("connected")
                    .data(Map.of("emitterId", emitterId, "traceId", MDC.get(TraceIdUtils.MDC_KEY))));
        } catch (IOException e) {
            remove(emitterId);
        }
        return emitter;
    }

    public static SseEmitter createStatic(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        return new SseEmitter();
    }

    public static void send(String emitterId, SseEvent event) {
        SseEmitter emitter = EMITTERS.get(emitterId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name(event.getType() == null ? "message" : event.getType().name().toLowerCase())
                    .data(JsonUtils.toJson(event)));
        } catch (Exception e) {
            log.debug("SSE 发送失败, emitter={}, err={}", emitterId, e.getMessage());
            remove(emitterId);
        }
    }

    public static void sendAll(SseEvent event) {
        EMITTERS.forEach((id, em) -> send(id, event));
    }

    public static void complete(String emitterId) {
        SseEmitter emitter = EMITTERS.remove(emitterId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    public static void remove(String emitterId) {
        SseEmitter emitter = EMITTERS.remove(emitterId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    public static int activeCount() {
        return EMITTERS.size();
    }
}
