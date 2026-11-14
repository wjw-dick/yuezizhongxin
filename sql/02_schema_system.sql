-- ============================================================
-- 02 系统与权限表（T03/T04）
-- 全部业务表带 tenant_id，唯一约束必须带租户维度
-- 字典表为平台统一下发（中文名称唯一出口），不带 tenant_id
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `username`         VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`         VARCHAR(100) NOT NULL COMMENT '口令散列（BCrypt）',
  `real_name`        VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  `phone`            VARCHAR(30)  DEFAULT NULL COMMENT '手机号（加密存储）',
  `role_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '角色标识',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `last_login_time`  DATETIME     DEFAULT NULL COMMENT '最近登录时间',
  `created_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户账号表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `role_code`   VARCHAR(30) NOT NULL COMMENT '角色编码（内部使用）',
  `role_name`   VARCHAR(50) NOT NULL COMMENT '角色中文名（页面唯一展示值）',
  `remark`      VARCHAR(200) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role` (`tenant_id`, `role_code`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户角色表（开通预置九角色）';

DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级菜单，0 为根',
  `title`      VARCHAR(50)  NOT NULL COMMENT '菜单标题（中文）',
  `path`       VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  `icon`       VARCHAR(50)  DEFAULT NULL COMMENT '图标',
  `menu_type`  VARCHAR(20)  NOT NULL DEFAULT '菜单' COMMENT '类型：目录/菜单/按钮',
  `perm`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可见：1 是 0 否',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户后台菜单表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `role_id`    BIGINT UNSIGNED NOT NULL COMMENT '角色标识',
  `menu_id`    BIGINT UNSIGNED NOT NULL COMMENT '菜单标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_menu` (`tenant_id`, `role_id`, `menu_id`),
  KEY `idx_tenant_role` (`tenant_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户角色菜单关联表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `config_group`  VARCHAR(50)  NOT NULL COMMENT '分组：站点基础/门店信息/订单规则/护理规则/营销规则/消息',
  `config_key`    VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value`  VARCHAR(500) DEFAULT NULL COMMENT '参数值',
  `value_type`    VARCHAR(20)  NOT NULL DEFAULT 'string' COMMENT '值类型：string/number/boolean/json',
  `remark`        VARCHAR(200) DEFAULT NULL COMMENT '说明',
  `updated_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `config_key`),
  KEY `idx_tenant_group` (`tenant_id`, `config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户系统参数表';

DROP TABLE IF EXISTS `sys_dict_category`;
CREATE TABLE `sys_dict_category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category`    VARCHAR(50) NOT NULL COMMENT '字典分类编码（内部使用）',
  `name`        VARCHAR(50) NOT NULL COMMENT '字典分类中文名',
  `sort`        INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典分类表（平台统一下发）';

DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category`       VARCHAR(50) NOT NULL COMMENT '字典分类编码',
  `item_code`      VARCHAR(50) NOT NULL COMMENT '字典项编码（仅内部使用，页面禁止展示）',
  `item_name`      VARCHAR(50) NOT NULL COMMENT '字典项中文名称（页面唯一展示值）',
  `color`          VARCHAR(20) DEFAULT NULL COMMENT '颜色标记，用于房态与状态标签',
  `sort`           INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status`         VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_item` (`category`, `item_code`),
  KEY `idx_category_sort` (`category`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项表（中文名称唯一出口）';

DROP TABLE IF EXISTS `sys_language`;
CREATE TABLE `sys_language` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(20) NOT NULL COMMENT '语言编码，如 zh-CN / en-US',
  `name`        VARCHAR(50) NOT NULL COMMENT '语言中文名',
  `is_default`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认语言',
  `enabled`     TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort`        INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='语言列表';

DROP TABLE IF EXISTS `sys_i18n_message`;
CREATE TABLE `sys_i18n_message` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lang_code`   VARCHAR(20)  NOT NULL COMMENT '语言编码',
  `msg_key`     VARCHAR(200) NOT NULL COMMENT '文案键',
  `msg_value`   VARCHAR(2000) DEFAULT NULL COMMENT '文案内容',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lang_key` (`lang_code`, `msg_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='界面语言包';

DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '租户标识',
  `tenant_code`    VARCHAR(32)   DEFAULT NULL COMMENT '租户编码',
  `module`         VARCHAR(50)   NOT NULL COMMENT '模块',
  `action`         VARCHAR(50)   NOT NULL COMMENT '动作',
  `target`         VARCHAR(200)  DEFAULT NULL COMMENT '操作对象',
  `detail`         VARCHAR(2000) DEFAULT NULL COMMENT '明细',
  `result`         VARCHAR(10)   NOT NULL DEFAULT '成功' COMMENT '结果：成功/失败',
  `operator_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人标识',
  `operator_name`  VARCHAR(50)   DEFAULT NULL COMMENT '操作人姓名',
  `role_name`      VARCHAR(50)   DEFAULT NULL COMMENT '角色中文名',
  `ip`             VARCHAR(64)   DEFAULT NULL COMMENT '来源 IP',
  `created_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`, `created_time`),
  KEY `idx_module_time` (`module`, `created_time`),
  KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户审计日志（只追加，不可修改删除）';
