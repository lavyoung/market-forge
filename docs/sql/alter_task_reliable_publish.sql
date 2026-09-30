-- 已有环境升级脚本：补齐本地消息任务的分片键、投递状态、退避重试和扫描索引。
-- task 为分片表，本脚本需要分别在 market-forge-001、market-forge-002 执行。
-- processed_message 是固定访问 ds1 的普通表，在 ds2 重复创建不会参与业务访问。
CREATE TABLE IF NOT EXISTS `processed_message`
(
    `id`
    BIGINT
    UNSIGNED
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '自增ID',
    `message_id`
    VARCHAR
(
    64
) NOT NULL COMMENT '消息唯一标识',
    `event_type` VARCHAR
(
    128
) NOT NULL COMMENT '事件类型',
    `occurred_at` DATETIME NOT NULL COMMENT '事件发生时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uq_processed_message_id`
(
    `message_id`
),
    KEY `idx_processed_message_create_time`
(
    `create_time`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='已成功消费消息表';

ALTER TABLE `task`
    ADD COLUMN `user_id` VARCHAR(32) NULL COMMENT '用户ID，同时作为分片键' AFTER `id`,
    ADD COLUMN `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '投递失败重试次数' AFTER `state`,
    ADD COLUMN `next_retry_time` DATETIME NULL COMMENT '下一次允许重试时间' AFTER `retry_count`,
    ADD COLUMN `last_error` VARCHAR(1024) NULL COMMENT '最近一次投递失败原因' AFTER `next_retry_time`,
    ADD INDEX `idx_task_publish` (`state`, `next_retry_time`, `create_time`);

-- 当前本地消息仅承载用户发奖事件，旧消息体中的 userId 可用于补齐分片键。
UPDATE `task`
SET `user_id` = JSON_UNQUOTE(JSON_EXTRACT(`message_body`, '$.userId'))
WHERE `user_id` IS NULL;

ALTER TABLE `task`
    MODIFY COLUMN `user_id` VARCHAR (32) NOT NULL COMMENT '用户ID，同时作为分片键';

UPDATE `task`
SET `state` = CASE `state`
                  WHEN 'processing' THEN 'publishing'
                  WHEN 'completed' THEN 'published'
                  WHEN 'fail' THEN 'publish_failed'
                  ELSE `state`
    END
WHERE `state` IN ('processing', 'completed', 'fail');

ALTER TABLE `task`
    MODIFY COLUMN `state` VARCHAR (16) NOT NULL DEFAULT 'create'
    COMMENT '任务状态；create-待投递、publishing-投递中、published-已投递、publish_failed-投递失败';
