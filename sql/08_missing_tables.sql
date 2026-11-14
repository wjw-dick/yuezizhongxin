-- =============================================================
-- 补充建表：功能性迭代新增的实体，原 01~05 建表脚本未覆盖
-- 这些表由以下功能直接使用，缺失会导致接口 500：
--   room_task          房务「清洁与维修」任务（RoomTaskService）
--   staff_shift        人力「班次定义」（StaffService.shifts）
--   mkt_activity       会员「营销活动」（MarketingService.activities）
--   member_address     会员收货地址（MemberService.addresses）
--   biz_order_change   订单变更留痕（OrderService.change）
--   biz_order_contract 订单合同附件（OrderService.contract）
-- 全部使用 IF NOT EXISTS，可安全重复执行。
-- =============================================================
USE yzz_platform;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `room_task` (
  `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     bigint unsigned NOT NULL COMMENT '租户',
  `room_id`       bigint unsigned NOT NULL COMMENT '房间',
  `task_type`     varchar(20)  NOT NULL COMMENT '类型：清洁/维修',
  `status`        varchar(20)  NOT NULL DEFAULT '待处理' COMMENT '状态：待处理/处理中/已完成/已取消',
  `assignee_id`   bigint unsigned DEFAULT NULL COMMENT '执行人（员工）',
  `due_time`      datetime     DEFAULT NULL COMMENT '要求完成时间',
  `remark`        varchar(255) DEFAULT NULL COMMENT '备注',
  `finished_time` datetime     DEFAULT NULL COMMENT '完成时间',
  `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rt_tenant` (`tenant_id`, `status`),
  KEY `idx_rt_room` (`tenant_id`, `room_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间清洁与维修任务';

CREATE TABLE IF NOT EXISTS `staff_shift` (
  `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    bigint unsigned NOT NULL COMMENT '租户',
  `shift_name`   varchar(30) NOT NULL COMMENT '班次名称：白班/夜班/中班',
  `start_time`   varchar(8)  NOT NULL COMMENT '开始时间 HH:mm',
  `end_time`     varchar(8)  NOT NULL COMMENT '结束时间 HH:mm',
  `cross_day`    tinyint     NOT NULL DEFAULT 0 COMMENT '是否跨天',
  `created_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ss_tenant` (`tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '班次定义';

CREATE TABLE IF NOT EXISTS `mkt_activity` (
  `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     bigint unsigned NOT NULL COMMENT '租户',
  `name`          varchar(100) NOT NULL COMMENT '活动名称',
  `activity_type` varchar(30)  NOT NULL COMMENT '类型：满减/折扣/赠品/拼团',
  `rule_json`     varchar(1000) DEFAULT NULL COMMENT '活动规则（JSON）',
  `start_time`    datetime     DEFAULT NULL COMMENT '开始时间',
  `end_time`      datetime     DEFAULT NULL COMMENT '结束时间',
  `status`        varchar(20)  NOT NULL DEFAULT '进行中' COMMENT '状态：未开始/进行中/已结束/已停用',
  `join_count`    int          NOT NULL DEFAULT 0 COMMENT '参与人数',
  `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ma_tenant` (`tenant_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '营销活动';

CREATE TABLE IF NOT EXISTS `member_address` (
  `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    bigint unsigned NOT NULL COMMENT '租户',
  `member_id`    bigint unsigned NOT NULL COMMENT '会员',
  `receiver`     varchar(50)  NOT NULL COMMENT '收货人',
  `phone`        varchar(128) NOT NULL COMMENT '手机号（密文）',
  `region`       varchar(100) DEFAULT NULL COMMENT '省市区',
  `detail`       varchar(255) DEFAULT NULL COMMENT '详细地址',
  `is_default`   tinyint      NOT NULL DEFAULT 0 COMMENT '是否默认',
  `created_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ma_member` (`tenant_id`, `member_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员收货地址';

CREATE TABLE IF NOT EXISTS `biz_order_change` (
  `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    bigint unsigned NOT NULL COMMENT '租户',
  `order_id`     bigint unsigned NOT NULL COMMENT '订单',
  `change_type`  varchar(30)  NOT NULL COMMENT '类型：换房/延期/改套餐/改数量',
  `before_json`  varchar(2000) DEFAULT NULL COMMENT '变更前快照',
  `after_json`   varchar(2000) DEFAULT NULL COMMENT '变更后快照',
  `amount_diff`  decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '金额差额',
  `reason`       varchar(255) DEFAULT NULL COMMENT '变更原因',
  `operator_id`  bigint unsigned DEFAULT NULL COMMENT '操作人',
  `created_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_boc_order` (`tenant_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单变更留痕';

CREATE TABLE IF NOT EXISTS `biz_order_contract` (
  `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    bigint unsigned NOT NULL COMMENT '租户',
  `order_id`     bigint unsigned NOT NULL COMMENT '订单',
  `file_key`     varchar(255) NOT NULL COMMENT '对象键',
  `file_name`    varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `signed_time`  datetime     DEFAULT NULL COMMENT '签署时间',
  `created_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bc_order` (`tenant_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单合同附件';
