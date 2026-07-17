-- ============================================================
-- V8 智能问答历史记录
-- ============================================================

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL                COMMENT '所属用户 ID',
    title           VARCHAR(200)    DEFAULT '新对话'        COMMENT '会话标题（默认首条 user 消息前 30 字）',
    platform        VARCHAR(50)     DEFAULT NULL            COMMENT '默认 LLM 平台',
    model           VARCHAR(100)    DEFAULT NULL            COMMENT '默认 LLM 模型',
    message_count   INT             DEFAULT 0               COMMENT '消息数（冗余字段，方便排序/统计）',
    last_message_at DATETIME        DEFAULT NULL            COMMENT '最后一条消息时间',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_recent (user_id, deleted, last_message_at),
    KEY idx_user_create (user_id, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能问答会话';

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    session_id    BIGINT          NOT NULL                COMMENT '所属会话 ID',
    user_id       BIGINT          NOT NULL                COMMENT '所属用户 ID（冗余）',
    role          VARCHAR(20)     NOT NULL                COMMENT 'user/assistant/system',
    content       LONGTEXT                                COMMENT '消息内容（Markdown）',
    platform      VARCHAR(50)     DEFAULT NULL            COMMENT 'assistant 时记录实际 LLM 平台',
    model         VARCHAR(100)    DEFAULT NULL            COMMENT 'assistant 时记录实际 LLM 模型',
    tokens        INT             DEFAULT NULL            COMMENT 'assistant 时总 token 数',
    latency_ms    BIGINT          DEFAULT NULL            COMMENT 'assistant 时延迟（毫秒）',
    create_time   DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT         DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_session (session_id, deleted, create_time),
    KEY idx_user_recent (user_id, deleted, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能问答消息';
