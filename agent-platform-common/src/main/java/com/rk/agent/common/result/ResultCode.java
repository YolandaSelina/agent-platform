package com.rk.agent.common.result;

/**
 * 业务状态码枚举
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务码：1xxxx 通用业务
    BUSINESS_ERROR(1001, "业务异常"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_ALREADY_EXISTS(1003, "数据已存在"),

    // 业务码：2xxxx 认证相关
    LOGIN_FAILED(2001, "登录失败"),
    INVALID_TOKEN(2002, "无效的令牌"),
    TOKEN_EXPIRED(2003, "令牌已过期"),
    USER_DISABLED(2004, "账号已禁用"),
    PASSWORD_ERROR(2005, "密码错误"),

    // 业务码：3xxxx 项目/任务
    PROJECT_NOT_FOUND(3001, "项目不存在"),
    TASK_NOT_FOUND(3002, "任务不存在"),

    // 业务码：4xxxx 流水线
    PIPELINE_NOT_FOUND(4001, "流水线不存在"),
    PIPELINE_INVALID(4002, "流水线配置无效"),
    PIPELINE_RUNNING(4003, "流水线正在执行中"),

    // 业务码：5xxxx 节点执行
    NODE_EXEC_FAILED(5001, "节点执行失败"),
    NODE_TIMEOUT(5002, "节点执行超时"),
    LLM_CALL_FAILED(5101, "LLM 调用失败"),
    LLM_RATE_LIMIT(5102, "LLM 限流"),
    HUMAN_REVIEW_REQUIRED(5201, "需要人工审核"),
    HUMAN_REVIEW_REJECTED(5202, "审核被拒绝"),

    // 业务码：6xxxx 工具
    TOOL_NOT_FOUND(6001, "工具不存在"),
    TOOL_CALL_FAILED(6002, "工具调用失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
