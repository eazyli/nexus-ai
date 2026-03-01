-- ===============================================
-- 会话记忆存储相关SQL
-- 用于持久化存储AI对话历史
-- ===============================================

-- 1. 确保ai_memory表支持会话记忆存储
-- 如果表已存在，可以跳过此步骤

-- 检查并添加必要的索引
CREATE INDEX IF NOT EXISTS idx_memory_session_id ON ai_memory(session_id);
CREATE INDEX IF NOT EXISTS idx_memory_expire_time ON ai_memory(expire_time);

-- 2. 会话消息清理存储过程
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS clean_expired_session_memory()
BEGIN
    -- 清理过期的短期记忆
    DELETE FROM ai_memory 
    WHERE memory_type = 'short' 
    AND expire_time IS NOT NULL 
    AND expire_time < NOW();
    
    -- 输出清理结果
    SELECT ROW_COUNT() AS deleted_rows;
END //
DELIMITER ;

-- 3. 创建定时清理事件（可选，根据需要启用）
-- SET GLOBAL event_scheduler = ON;
-- CREATE EVENT IF NOT EXISTS clean_memory_event
-- ON SCHEDULE EVERY 1 HOUR
-- DO CALL clean_expired_session_memory();

-- 4. 会话消息统计视图
CREATE OR REPLACE VIEW v_session_stats AS
SELECT 
    session_id,
    COUNT(*) as message_count,
    SUM(token_count) as total_tokens,
    MIN(create_time) as first_message_time,
    MAX(create_time) as last_message_time,
    GROUP_CONCAT(DISTINCT role) as roles
FROM ai_memory
WHERE session_id IS NOT NULL
GROUP BY session_id;

-- 5. 添加会话记忆配置表（可选）
CREATE TABLE IF NOT EXISTS ai_session_config (
    session_id VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
    app_id VARCHAR(64) COMMENT '应用ID',
    user_id VARCHAR(64) COMMENT '用户ID',
    max_messages INT DEFAULT 20 COMMENT '最大消息数',
    session_ttl INT DEFAULT 168 COMMENT '会话过期时间(小时)',
    config JSON COMMENT '扩展配置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_app_user (app_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话配置表';
