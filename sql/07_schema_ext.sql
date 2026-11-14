-- ============================================================
-- 07 业务补充表（T30 清洁维修任务、T36 合同与变更、T46 班次定义、
--     T47 营销活动、T78 收货地址）
-- 说明：01~06 已覆盖 78 张主表，本脚本只补实现阶段新增的 6 张协作表。
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `room_task`;
CREATE TABLE `room_task` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `room_id`      BIGINT UNSIGNED NOT NULL COMMENT '房间标识',
  `task_type`    VARCHAR(20) NOT NULL DEFAULT '清洁' COMMENT '类型：清洁/维修',
  `status`       VARCHAR(20) NOT NULL DEFAULT '待处理' COMMENT '状态：待处理/进行中/已完成',
  `assignee_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '执行人',
  `due_time`     DATETIME DEFAULT NULL COMMENT '要求完成时间',
  `remark`       VARCHAR(300) DEFAULT NULL COMMENT '任务说明',
  `finished_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_room` (`tenant_id`, `room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房间清洁与维修任务';

DROP TABLE IF EXISTS `biz_order_contract`;
CREATE TABLE `biz_order_contract` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `file_key`    VARCHAR(300) NOT NULL COMMENT '合同附件（对象存储键）',
  `file_name`   VARCHAR(200) DEFAULT NULL COMMENT '文件名',
  `signed_time` DATETIME DEFAULT NULL COMMENT '签署时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单合同附件';

DROP TABLE IF EXISTS `biz_order_change`;
CREATE TABLE `biz_order_change` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `change_type` VARCHAR(30) NOT NULL COMMENT '变更类型：换房/延期/加项/改价/退订',
  `before_json` TEXT COMMENT '变更前快照',
  `after_json`  TEXT COMMENT '变更后快照',
  `amount_diff` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额差额',
  `reason`      VARCHAR(300) DEFAULT NULL COMMENT '变更原因',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单变更留痕';

DROP TABLE IF EXISTS `staff_shift`;
CREATE TABLE `staff_shift` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `shift_name`  VARCHAR(20) NOT NULL COMMENT '班次名称：早班/中班/夜班',
  `start_time`  VARCHAR(8) NOT NULL COMMENT '开始时间 HH:mm',
  `end_time`    VARCHAR(8) NOT NULL COMMENT '结束时间 HH:mm',
  `cross_day`   TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨天',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_shift` (`tenant_id`, `shift_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班次定义';

DROP TABLE IF EXISTS `mkt_activity`;
CREATE TABLE `mkt_activity` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`          VARCHAR(100) NOT NULL COMMENT '活动名称',
  `activity_type` VARCHAR(20) NOT NULL DEFAULT '限时优惠' COMMENT '类型：拼团/限时优惠/满减',
  `rule_json`     TEXT COMMENT '活动规则（成团人数、折扣、库存等）',
  `start_time`    DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time`      DATETIME DEFAULT NULL COMMENT '结束时间',
  `status`        VARCHAR(20) NOT NULL DEFAULT '未开始' COMMENT '状态：未开始/进行中/已结束/已停用',
  `join_count`    INT NOT NULL DEFAULT 0 COMMENT '参与人数',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营销活动（拼团/限时）';

DROP TABLE IF EXISTS `member_address`;
CREATE TABLE `member_address` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '会员标识',
  `receiver`     VARCHAR(50) NOT NULL COMMENT '收货人',
  `phone`        VARCHAR(64) NOT NULL COMMENT '手机号（加密）',
  `region`       VARCHAR(100) DEFAULT NULL COMMENT '省市区',
  `detail`       VARCHAR(200) NOT NULL COMMENT '详细地址',
  `is_default`   TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认地址',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_member` (`tenant_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员收货地址';

-- 初始班次（每个存量租户各写一份，新增租户由 TenantInitService 写入）
INSERT INTO `staff_shift` (`tenant_id`, `shift_name`, `start_time`, `end_time`, `cross_day`)
SELECT t.`id`, s.`shift_name`, s.`start_time`, s.`end_time`, s.`cross_day`
FROM `platform_tenant` t
JOIN (
  SELECT '早班' AS `shift_name`, '08:00' AS `start_time`, '16:00' AS `end_time`, 0 AS `cross_day`
  UNION ALL SELECT '中班', '16:00', '00:00', 1
  UNION ALL SELECT '夜班', '00:00', '08:00', 0
) s
WHERE NOT EXISTS (
  SELECT 1 FROM `staff_shift` x WHERE x.`tenant_id` = t.`id` AND x.`shift_name` = s.`shift_name`
);
