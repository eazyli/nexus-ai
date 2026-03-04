-- ========================================
-- 企业级通用AI平台 - 数据库表结构
-- MySQL 8.0+
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `nexus_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `nexus_ai`;

-- ========================================
-- 1. 应用管理表（通用，适配所有业务系统）
-- ========================================
DROP TABLE IF EXISTS `ai_app`;
CREATE TABLE `ai_app` (
    `app_id` VARCHAR(64) PRIMARY KEY COMMENT '应用ID（业务系统唯一标识）',
    `app_name` VARCHAR(128) NOT NULL COMMENT '应用名称（业务系统名称）',
    `app_secret` VARCHAR(128) NOT NULL COMMENT '应用密钥（鉴权用）',
    `tenant_id` VARCHAR(64) COMMENT '租户ID（多租户支持）',
    `description` VARCHAR(512) COMMENT '应用描述',
    `app_type` VARCHAR(32) DEFAULT 'chatbot' COMMENT '应用类型（chatbot/assistant/workflow/agent）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1启用/0禁用）',
    `qps_limit` INT DEFAULT 100 COMMENT 'QPS限制（通用）',
    `daily_limit` INT DEFAULT 10000 COMMENT '日调用限额（通用）',
    `default_model_id` VARCHAR(64) COMMENT '默认模型ID',
    `system_prompt` TEXT COMMENT '系统提示词',
    `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度参数',
    `max_tokens` INT DEFAULT 4096 COMMENT '最大Token数',
    `extra_config` JSON COMMENT '扩展配置（JSON）',
    `capabilities` JSON COMMENT '能力标签列表（用于多智能体匹配）',
    `collaboration_mode` VARCHAR(32) DEFAULT 'single' COMMENT '协作模式（single/react/plan_execute/supervisor）',
    `execution_config` JSON COMMENT 'Agent执行配置',
    `variables` JSON COMMENT '变量定义',
    `greeting` TEXT COMMENT '开场白',
    `sample_questions` JSON COMMENT '示例问题列表',
    `priority` INT DEFAULT 0 COMMENT '优先级（数值越大优先级越高）',
    `icon` VARCHAR(512) COMMENT '图标URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_tenant` (`tenant_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_collaboration_mode` (`collaboration_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务系统应用表（通用）';

-- ========================================
-- 2. 工具注册表（通用，适配所有工具类型）
-- ========================================
DROP TABLE IF EXISTS `ai_mcp_tool`;
CREATE TABLE `ai_mcp_tool` (
    `tool_id` VARCHAR(64) PRIMARY KEY COMMENT '工具ID（唯一）',
    `tool_name` VARCHAR(128) NOT NULL COMMENT '工具名称',
    `tool_type` VARCHAR(32) NOT NULL COMMENT '工具类型（HTTP/DB/Dubbo/THIRD_PARTY_API）',
    `description` VARCHAR(512) COMMENT '工具描述',
    `config` JSON NOT NULL COMMENT '工具配置（URL/参数/响应结构/重试配置，通用JSON）',
    `app_id` VARCHAR(64) COMMENT '所属应用ID（可为空，通用工具）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1启用/0禁用）',
    `permission_apps` VARCHAR(512) COMMENT '可调用该工具的AppId列表（逗号分隔）',
    `retry_times` INT DEFAULT 3 COMMENT '重试次数',
    `retry_interval` INT DEFAULT 1000 COMMENT '重试间隔（ms）',
    `timeout` INT DEFAULT 30000 COMMENT '超时时间（ms）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_app` (`app_id`),
    INDEX `idx_type` (`tool_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MCP工具注册表（通用）';

-- ========================================
-- 3. 知识库表（通用，适配所有知识库类型）
-- ========================================
DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
    `knowledge_id` VARCHAR(64) PRIMARY KEY COMMENT '知识库ID（唯一）',
    `knowledge_name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `knowledge_type` VARCHAR(32) NOT NULL COMMENT '知识库类型（document/database/api）',
    `description` VARCHAR(512) COMMENT '知识库描述',
    `app_id` VARCHAR(64) COMMENT '所属应用ID（可为空，通用知识库）',
    `file_list` JSON COMMENT '文件列表（知识库类型为document时）',
    `data_source` JSON COMMENT '数据源配置（知识库类型为database/api时）',
    `vector_db_config` JSON NOT NULL COMMENT '向量库配置（通用）',
    `embedding_model` VARCHAR(64) COMMENT 'Embedding模型ID',
    `chunk_size` INT DEFAULT 500 COMMENT '切片大小',
    `chunk_overlap` INT DEFAULT 50 COMMENT '切片重叠',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0处理中/1成功/2失败）',
    `process_progress` INT DEFAULT 0 COMMENT '处理进度（0-100）',
    `doc_count` INT DEFAULT 0 COMMENT '文档数量',
    `chunk_count` INT DEFAULT 0 COMMENT '切片数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_app` (`app_id`),
    INDEX `idx_type` (`knowledge_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表（通用）';

-- ========================================
-- 5. 知识库文档表
-- ========================================
DROP TABLE IF EXISTS `ai_knowledge_doc`;
CREATE TABLE `ai_knowledge_doc` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `knowledge_id` VARCHAR(64) NOT NULL COMMENT '知识库ID',
    `file_name` VARCHAR(256) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(512) COMMENT '文件路径',
    `file_type` VARCHAR(32) COMMENT '文件类型（pdf/word/excel/txt）',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `chunk_count` INT DEFAULT 0 COMMENT '切片数量',
    `status` TINYINT DEFAULT 0 COMMENT '状态（0处理中/1成功/2失败）',
    `error_msg` VARCHAR(512) COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_knowledge` (`knowledge_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- ========================================
-- 6. 记忆表（通用，短期+长期记忆）
-- ========================================
DROP TABLE IF EXISTS `ai_memory`;
CREATE TABLE `ai_memory` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（通用）',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（短期记忆关联）',
    `memory_type` VARCHAR(32) NOT NULL COMMENT '记忆类型（short/long/business）',
    `role` VARCHAR(32) COMMENT '角色（user/assistant/system/tool）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `memory_data` JSON COMMENT '扩展数据（通用JSON）',
    `token_count` INT DEFAULT 0 COMMENT 'Token数量',
    `expire_time` DATETIME COMMENT '过期时间（短期记忆必填）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_app` (`user_id`, `app_id`),
    INDEX `idx_session` (`session_id`),
    INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记忆表（通用）';

-- ========================================
-- 7. 调用日志表（通用，全平台日志）
-- ========================================
DROP TABLE IF EXISTS `ai_call_log`;
CREATE TABLE `ai_call_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `request_id` VARCHAR(64) COMMENT '请求ID',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
    `session_id` VARCHAR(64) COMMENT '会话ID',
    `user_id` VARCHAR(64) COMMENT '用户ID',
    `query` TEXT COMMENT '用户输入',
    `result` JSON COMMENT '调用结果',
    `natural_response` TEXT COMMENT '自然语言回复',
    `used_tools` VARCHAR(512) COMMENT '使用的工具列表',
    `execution_steps` INT DEFAULT 0 COMMENT '执行步骤数',
    `execution_time` INT COMMENT '执行耗时（ms）',
    `token_input` INT DEFAULT 0 COMMENT '输入Token数',
    `token_output` INT DEFAULT 0 COMMENT '输出Token数',
    `status` TINYINT NOT NULL COMMENT '调用状态（1成功/0失败）',
    `error_msg` VARCHAR(512) COMMENT '错误信息（失败时）',
    `client_ip` VARCHAR(64) COMMENT '客户端IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_app_time` (`app_id`, `create_time`),
    INDEX `idx_session` (`session_id`),
    INDEX `idx_request` (`request_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI调用日志表（通用）';

-- ========================================
-- 9. 模型配置表（通用，支持多模型切换）
-- ========================================
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config` (
    `model_id` VARCHAR(64) PRIMARY KEY COMMENT '模型ID（唯一）',
    `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `provider` VARCHAR(32) NOT NULL COMMENT '模型提供商（openai/azure/anthropic/qwen/deepseek）',
    `model_type` VARCHAR(32) DEFAULT 'chat' COMMENT '模型类型（chat/embedding/rerank）',
    `api_key` VARCHAR(256) COMMENT 'API密钥（加密存储）',
    `api_base` VARCHAR(256) COMMENT 'API地址',
    `model_version` VARCHAR(64) COMMENT '模型版本',
    `max_context` INT DEFAULT 8192 COMMENT '最大上下文长度',
    `config` JSON COMMENT '模型配置（温度、采样等）',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1启用/0禁用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_provider` (`provider`),
    INDEX `idx_type` (`model_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表（通用）';

-- ========================================
-- 10. 会话表
-- ========================================
DROP TABLE IF EXISTS `ai_session`;
CREATE TABLE `ai_session` (
    `session_id` VARCHAR(64) PRIMARY KEY COMMENT '会话ID',
    `app_id` VARCHAR(64) NOT NULL COMMENT '应用ID',
    `user_id` VARCHAR(64) COMMENT '用户ID',
    `title` VARCHAR(256) COMMENT '会话标题',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `last_message` TEXT COMMENT '最后一条消息',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1活跃/0归档）',
    `expire_time` DATETIME COMMENT '过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_app_user` (`app_id`, `user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- ========================================
-- 11. 插件表
-- ========================================
DROP TABLE IF EXISTS `ai_plugin`;
CREATE TABLE `ai_plugin` (
    `plugin_id` VARCHAR(64) PRIMARY KEY COMMENT '插件ID',
    `plugin_name` VARCHAR(128) NOT NULL COMMENT '插件名称',
    `plugin_version` VARCHAR(32) NOT NULL COMMENT '插件版本',
    `description` VARCHAR(512) COMMENT '插件描述',
    `plugin_type` VARCHAR(32) COMMENT '插件类型',
    `main_class` VARCHAR(256) COMMENT '主类全路径',
    `config_schema` JSON COMMENT '配置Schema',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1启用/0禁用）',
    `install_time` DATETIME COMMENT '安装时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_type` (`plugin_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='插件表';

-- ========================================
-- 初始化数据
-- ========================================

-- 初始化默认应用
INSERT INTO `ai_app` (`app_id`, `app_name`, `app_secret`, `app_type`, `status`,
    `collaboration_mode`, `capabilities`, `greeting`, `sample_questions`) VALUES
('default-app', '默认应用', 'default-secret-key-12345', 'agent', 1,
    'react', '["rag", "tool_calling", "general_qa"]', 
    '您好，我是您的AI助手。我可以帮您查询天气、检索知识库，或者回答您的问题。',
    '["今天北京的天气怎么样？", "帮我查一下最新的产品文档", "你能帮我做什么？"]');

-- 初始化默认模型配置
INSERT INTO `ai_model_config` (`model_id`, `model_name`, `provider`, `model_type`, `model_version`, `max_context`, `status`) VALUES
('gpt-4', 'GPT-4', 'openai', 'chat', 'gpt-4-turbo-preview', 128000, 1),
('gpt-35-turbo', 'GPT-3.5 Turbo', 'openai', 'chat', 'gpt-3.5-turbo', 16384, 1),
('qwen-max', '通义千问-Max', 'qwen', 'chat', 'qwen-max', 8192, 1),
('deepseek-chat', 'DeepSeek Chat', 'deepseek', 'chat', 'deepseek-chat', 32768, 1),
('text-embedding-ada-002', 'OpenAI Embedding', 'openai', 'embedding', 'text-embedding-ada-002', 8191, 1);

-- ========================================
-- 智能体相关视图
-- ========================================

-- 活跃智能体视图
CREATE OR REPLACE VIEW `v_active_agents` AS
SELECT 
    `app_id`,
    `app_name`,
    `app_type`,
    `collaboration_mode`,
    `capabilities`,
    `priority`,
    `icon`
FROM `ai_app`
WHERE `status` = 1 AND `app_type` = 'agent'
ORDER BY `priority` DESC, `create_time` ASC;
