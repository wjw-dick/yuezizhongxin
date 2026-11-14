-- ============================================================
-- 01 平台级表（T09）：统一 platform_ 前缀，不带 tenant_id
-- 平台运营端只读写这些表，看不到任何租户业务明细
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `platform_tenant`;
CREATE TABLE `platform_tenant` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_code`       VARCHAR(32)  NOT NULL COMMENT '租户标识，用于子域名与存储路径',
  `tenant_name`       VARCHAR(100) NOT NULL COMMENT '租户名称',
  `industry`          VARCHAR(50)  DEFAULT NULL COMMENT '所属行业',
  `contact_name`      VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone`     VARCHAR(30)  DEFAULT NULL COMMENT '联系电话',
  `package_id`        BIGINT UNSIGNED NOT NULL COMMENT '套餐标识',
  `status`            VARCHAR(20)  NOT NULL DEFAULT 'TRIAL' COMMENT '状态：TRIAL试用中/NORMAL正常/ARREARS欠费/DISABLED停用/EXPIRED已到期',
  `trial_end_time`    DATETIME     DEFAULT NULL COMMENT '试用到期时间',
  `expire_time`       DATETIME     DEFAULT NULL COMMENT '服务到期时间',
  `seat_limit`        INT          NOT NULL DEFAULT 0 COMMENT '席位上限',
  `mp_limit`          INT          NOT NULL DEFAULT 0 COMMENT '公众号数上限',
  `storage_limit_gb`  INT          NOT NULL DEFAULT 0 COMMENT '存储配额（GB）',
  `sms_limit`         INT          NOT NULL DEFAULT 0 COMMENT '短信配额（条）',
  `api_limit`         INT          NOT NULL DEFAULT 0 COMMENT '接口调用配额',
  `created_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_status` (`status`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台租户表';

DROP TABLE IF EXISTS `platform_package`;
CREATE TABLE `platform_package` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_name`      VARCHAR(50)   NOT NULL COMMENT '套餐名称',
  `price`             DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '套餐价格（元）',
  `billing_cycle`     VARCHAR(10)   NOT NULL DEFAULT '年' COMMENT '计费周期：月/年',
  `end_count`         INT           NOT NULL DEFAULT 5 COMMENT '含端数量',
  `seat_quota`        INT           NOT NULL DEFAULT 0 COMMENT '席位额度',
  `mp_quota`          INT           NOT NULL DEFAULT 0 COMMENT '公众号额度',
  `storage_quota_gb`  INT           NOT NULL DEFAULT 0 COMMENT '存储额度（GB）',
  `sms_quota`         INT           NOT NULL DEFAULT 0 COMMENT '短信额度（条）',
  `api_quota`         INT           NOT NULL DEFAULT 0 COMMENT '接口额度',
  `trial_supported`   TINYINT       NOT NULL DEFAULT 1 COMMENT '是否支持试用：1 是 0 否',
  `remark`            VARCHAR(255)  DEFAULT NULL COMMENT '备注',
  `created_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台套餐表';

DROP TABLE IF EXISTS `platform_tenant_usage`;
CREATE TABLE `platform_tenant_usage` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `used_seat`         INT NOT NULL DEFAULT 0 COMMENT '已用席位',
  `used_mp`           INT NOT NULL DEFAULT 0 COMMENT '已用公众号数',
  `used_storage_gb`   INT NOT NULL DEFAULT 0 COMMENT '已用存储（GB）',
  `used_sms`          INT NOT NULL DEFAULT 0 COMMENT '已用短信条数',
  `used_api`          INT NOT NULL DEFAULT 0 COMMENT '已用接口调用量',
  `stat_date`         DATE NOT NULL COMMENT '统计日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台租户用量表（按日汇总）';

DROP TABLE IF EXISTS `platform_bill`;
CREATE TABLE `platform_bill` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `bill_period`   VARCHAR(10)   NOT NULL COMMENT '账期，格式 yyyy-MM',
  `item_name`     VARCHAR(200)  NOT NULL COMMENT '计费项',
  `quantity`      INT           NOT NULL DEFAULT 0 COMMENT '计费数量（席位数）',
  `amount`        DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额（元）',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待支付/PAID已支付/OVERDUE已逾期',
  `pay_time`      DATETIME      DEFAULT NULL COMMENT '支付时间',
  `created_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_period` (`tenant_id`, `bill_period`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台账单表';

DROP TABLE IF EXISTS `platform_audit`;
CREATE TABLE `platform_audit` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module`         VARCHAR(50)   NOT NULL COMMENT '模块',
  `action`         VARCHAR(50)   NOT NULL COMMENT '动作',
  `target`         VARCHAR(200)  DEFAULT NULL COMMENT '操作对象',
  `detail`         VARCHAR(1000) DEFAULT NULL COMMENT '明细',
  `result`         VARCHAR(10)   NOT NULL DEFAULT '成功' COMMENT '结果：成功/失败',
  `operator_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人标识',
  `operator_name`  VARCHAR(50)   DEFAULT NULL COMMENT '操作人姓名',
  `platform_role`  VARCHAR(50)   DEFAULT NULL COMMENT '平台角色',
  `tenant_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '目标租户标识',
  `tenant_code`    VARCHAR(32)   DEFAULT NULL COMMENT '目标租户编码',
  `ip`             VARCHAR(64)   DEFAULT NULL COMMENT '来源 IP',
  `created_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`, `created_time`),
  KEY `idx_module_time` (`module`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台审计表（与租户审计物理分离）';

DROP TABLE IF EXISTS `platform_user`;
CREATE TABLE `platform_user` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`         VARCHAR(50)  NOT NULL COMMENT '平台账号',
  `password`         VARCHAR(100) NOT NULL COMMENT '口令散列（BCrypt）',
  `real_name`        VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  `role_code`        VARCHAR(30)  NOT NULL COMMENT '平台角色：platform_super/platform_oper/platform_fin',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `last_login_time`  DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  `created_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台账号表';

DROP TABLE IF EXISTS `platform_menu`;
CREATE TABLE `platform_menu` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级菜单，0 为根',
  `title`      VARCHAR(50)  NOT NULL COMMENT '菜单标题（中文）',
  `path`       VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  `icon`       VARCHAR(50)  DEFAULT NULL COMMENT '图标',
  `menu_type`  VARCHAR(20)  NOT NULL DEFAULT '菜单' COMMENT '类型：目录/菜单/按钮',
  `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：1 是 0 否',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台菜单表（与租户菜单物理隔离）';

DROP TABLE IF EXISTS `platform_role_menu`;
CREATE TABLE `platform_role_menu` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_code`  VARCHAR(30) NOT NULL COMMENT '平台角色编码',
  `menu_id`    BIGINT UNSIGNED NOT NULL COMMENT '菜单标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_code`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台角色菜单关联表';

DROP TABLE IF EXISTS `platform_tenant_admin`;
CREATE TABLE `platform_tenant_admin` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户账号标识',
  `primary_admin`   TINYINT NOT NULL DEFAULT 1 COMMENT '是否主管理员：1 是 0 否',
  `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台租户管理员表';
