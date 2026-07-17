package com.rk.agent.common.sse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * SSE 事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SseEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Type {
        /** 节点开始 */
        NODE_START,
        /** 节点结束 */
        NODE_END,
        /** 思考/输出片段 */
        CHUNK,
        /** 工具调用 */
        TOOL_CALL,
        /** 工具结果 */
        TOOL_RESULT,
        /** 投票进度 */
        VOTE,
        /** 状态变更 */
        STATUS,
        /** 错误 */
        ERROR,
        /** 完成 */
        DONE,
        /** 通知/心跳 */
        HEARTBEAT
    }

    /** 事件类型 */
    private Type type;
    /** 节点 ID */
    private String nodeId;
    /** 节点名称 */
    private String nodeName;
    /** 时间戳 */
    private Long timestamp;
    /** 消息 */
    private String message;
    /** 负载数据 */
    private Object payload;
    /** 扩展字段 */
    private Map<String, Object> extras;
}
