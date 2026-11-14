-- ============================================================
-- 05 公众号运营、消息配置、SSO、存储、菜单可视化、端入口（T04/T27/T28/T62~T70）
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `mp_account`;
CREATE TABLE `mp_account` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`           BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_name`        VARCHAR(100) NOT NULL COMMENT '公众号名称',
  `wechat_id`           VARCHAR(50)  DEFAULT NULL COMMENT '微信号',
  `app_id`              VARCHAR(64)  NOT NULL COMMENT 'AppID',
  `app_secret`          VARCHAR(255) DEFAULT NULL COMMENT 'AppSecret（AES 加密，接口只回掩码）',
  `server_url`          VARCHAR(200) DEFAULT NULL COMMENT '服务器地址（自动生成）',
  `token`               VARCHAR(255) DEFAULT NULL COMMENT '消息令牌（加密）',
  `aes_key`             VARCHAR(255) DEFAULT NULL COMMENT '消息加解密密钥（加密）',
  `oauth_domain`        VARCHAR(200) DEFAULT NULL COMMENT '网页授权域名',
  `js_safe_domain`      VARCHAR(200) DEFAULT NULL COMMENT 'JS 安全域名',
  `account_type`        VARCHAR(20) NOT NULL DEFAULT '服务号' COMMENT '公众号类型：服务号/订阅号',
  `verified`            TINYINT NOT NULL DEFAULT 0 COMMENT '是否已认证',
  `qrcode`              VARCHAR(200) DEFAULT NULL COMMENT '二维码图键',
  `remark`              VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `created_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_appid` (`tenant_id`, `app_id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号台账与凭证';

DROP TABLE IF EXISTS `mp_tag`;
CREATE TABLE `mp_tag` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`   BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `tag_name`     VARCHAR(50) NOT NULL COMMENT '标签名称',
  `fan_count`    INT NOT NULL DEFAULT 0 COMMENT '粉丝数',
  `sync_status`  VARCHAR(20) NOT NULL DEFAULT '已同步' COMMENT '同步状态：已同步/同步失败',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_account_name` (`tenant_id`, `account_id`, `tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号标签表';

DROP TABLE IF EXISTS `mp_fan`;
CREATE TABLE `mp_fan` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`       BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `open_id`          VARCHAR(64) NOT NULL COMMENT '用户标识',
  `union_id`         VARCHAR(64) DEFAULT NULL COMMENT 'UnionID（去重口径）',
  `nickname`         VARCHAR(100) DEFAULT NULL COMMENT '昵称',
  `avatar`           VARCHAR(200) DEFAULT NULL COMMENT '头像',
  `gender`           VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `region`           VARCHAR(100) DEFAULT NULL COMMENT '地区',
  `remark`           VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `subscribe_status` VARCHAR(20) NOT NULL DEFAULT '已关注' COMMENT '订阅状态：已关注/已取消关注',
  `subscribe_time`   DATETIME DEFAULT NULL COMMENT '关注时间',
  `unsubscribe_time` DATETIME DEFAULT NULL COMMENT '取关时间',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_account_open` (`tenant_id`, `account_id`, `open_id`),
  KEY `idx_tenant_union` (`tenant_id`, `union_id`),
  KEY `idx_tenant_nickname` (`tenant_id`, `nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号粉丝表';

DROP TABLE IF EXISTS `mp_message`;
CREATE TABLE `mp_message` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`    BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `open_id`       VARCHAR(64) NOT NULL COMMENT '用户标识',
  `msg_type`      VARCHAR(20) NOT NULL COMMENT '消息类型（字典中文名）：文本/图片/语音/视频/图文/事件',
  `sender`        VARCHAR(20) NOT NULL COMMENT '发送方：用户/公众号',
  `content`       VARCHAR(2000) DEFAULT NULL COMMENT '内容',
  `raw_payload`   LONGTEXT COMMENT '原始报文',
  `error_code`    VARCHAR(50) DEFAULT NULL COMMENT '错误码（原样落库）',
  `error_msg`     VARCHAR(500) DEFAULT NULL COMMENT '错误描述（原样落库）',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type_time` (`tenant_id`, `msg_type`, `created_time`),
  KEY `idx_tenant_open` (`tenant_id`, `open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号消息流水';

DROP TABLE IF EXISTS `mp_material`;
CREATE TABLE `mp_material` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`    BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `media_id`      VARCHAR(100) DEFAULT NULL COMMENT '素材标识',
  `name`          VARCHAR(200) NOT NULL COMMENT '素材名称',
  `material_type` VARCHAR(20) NOT NULL COMMENT '类型：图片/语音/视频',
  `size_bytes`    BIGINT DEFAULT NULL COMMENT '大小（字节）',
  `permanent`     TINYINT NOT NULL DEFAULT 1 COMMENT '是否永久素材',
  `thumb_media_id` VARCHAR(100) DEFAULT NULL COMMENT '视频缩略图标识',
  `file_key`      VARCHAR(200) DEFAULT NULL COMMENT '对象存储键',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`, `material_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号素材表';

DROP TABLE IF EXISTS `mp_draft`;
CREATE TABLE `mp_draft` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`    BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `title`         VARCHAR(200) NOT NULL COMMENT '标题',
  `author`        VARCHAR(50) DEFAULT NULL COMMENT '作者',
  `source_url`    VARCHAR(300) DEFAULT NULL COMMENT '原文地址',
  `cover_key`     VARCHAR(200) DEFAULT NULL COMMENT '封面图键（建议 900×383）',
  `digest`        VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `body_html`     LONGTEXT COMMENT '富文本正文',
  `article_index` INT NOT NULL DEFAULT 1 COMMENT '多图文序号，1 为头条',
  `draft_media_id` VARCHAR(100) DEFAULT NULL COMMENT '草稿标识',
  `status`        VARCHAR(20) NOT NULL DEFAULT '草稿' COMMENT '状态：草稿/已发表',
  `publish_time`  DATETIME DEFAULT NULL COMMENT '发表时间',
  `publish_result` VARCHAR(20) DEFAULT NULL COMMENT '发表结果：成功/失败/审核中',
  `fail_reason`   VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图文草稿与发表记录';

DROP TABLE IF EXISTS `mp_menu_config`;
CREATE TABLE `mp_menu_config` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`   BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `config_name`  VARCHAR(100) NOT NULL COMMENT '菜单配置名称',
  `tree_json`    LONGTEXT NOT NULL COMMENT '菜单树 JSON（一级 ≤3，二级 ≤5）',
  `published`    TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发布到微信',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_account` (`tenant_id`, `account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号菜单配置';

DROP TABLE IF EXISTS `mp_auto_reply`;
CREATE TABLE `mp_auto_reply` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `account_id`   BIGINT UNSIGNED NOT NULL COMMENT '公众号标识',
  `reply_type`   VARCHAR(20) NOT NULL COMMENT '类型：关注时回复/消息回复/关键词回复',
  `keyword`      VARCHAR(100) DEFAULT NULL COMMENT '关键词',
  `match_type`   VARCHAR(20) DEFAULT NULL COMMENT '匹配类型：完全匹配/半匹配',
  `msg_type`     VARCHAR(20) NOT NULL DEFAULT '文本' COMMENT '回复消息类型：文本/图片/语音/视频/图文/音乐',
  `content`      VARCHAR(2000) DEFAULT NULL COMMENT '回复内容',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`, `reply_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公众号自动回复';

DROP TABLE IF EXISTS `msg_channel_config`;
CREATE TABLE `msg_channel_config` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `channel_code`  VARCHAR(30) NOT NULL COMMENT '短信渠道：aliyun/tencent/huawei/jd/aws_sns/local_log',
  `channel_name`  VARCHAR(50) NOT NULL COMMENT '渠道中文名',
  `enabled`       TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用（全局互斥，仅一条为 1）',
  `config_json`   TEXT COMMENT '渠道参数（密钥加密）',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_channel` (`tenant_id`, `channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息通道配置（4 通道 + 6 短信渠道）';

DROP TABLE IF EXISTS `msg_template`;
CREATE TABLE `msg_template` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `trigger_node`   VARCHAR(50) NOT NULL COMMENT '触发节点中文名：预约成功/定金支付成功/入住提醒/护理异常/尾款提醒/42 天回访等',
  `channel`        VARCHAR(30) NOT NULL COMMENT '通道：公众号模板消息/小程序订阅消息/App 推送/站内信',
  `template_code`  VARCHAR(100) NOT NULL COMMENT '模板编号',
  `field_mapping`  VARCHAR(500) DEFAULT NULL COMMENT '字段映射，如 thing1=客户姓名,time2=入住时间',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_node_channel` (`tenant_id`, `trigger_node`, `channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息模板表（同节点同通道唯一）';

DROP TABLE IF EXISTS `sys_sso_config`;
CREATE TABLE `sys_sso_config` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `enabled`          TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用',
  `protocol`         VARCHAR(20) NOT NULL DEFAULT 'OIDC' COMMENT '协议：CAS/OAuth2/OIDC/Sa-Token',
  `server_url`       VARCHAR(300) DEFAULT NULL COMMENT '服务器根地址（自动拼协议路径）',
  `service_url`      VARCHAR(300) DEFAULT NULL COMMENT 'Service 回调地址',
  `client_id`        VARCHAR(100) DEFAULT NULL COMMENT '客户端标识',
  `client_secret`    VARCHAR(255) DEFAULT NULL COMMENT '客户端密钥（加密）',
  `account_mapping`  VARCHAR(200) DEFAULT NULL COMMENT '账号映射字段',
  `fallback_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'SSO 故障时是否降级账号密码登录',
  `reject_strategy`  VARCHAR(20) NOT NULL DEFAULT '拒绝' COMMENT '账号不存在策略：拒绝/自动创建',
  `updated_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SSO 配置（单租户单条）';

DROP TABLE IF EXISTS `sys_storage_config`;
CREATE TABLE `sys_storage_config` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `driver`        VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '驱动：local/s3/oss/cos/minio',
  `config_json`   TEXT COMMENT '驱动参数（密钥加密）',
  `tested`        TINYINT NOT NULL DEFAULT 0 COMMENT '连接测试是否通过',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对象存储配置';

DROP TABLE IF EXISTS `sys_nav_node`;
CREATE TABLE `sys_nav_node` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `end_type`   VARCHAR(20) NOT NULL COMMENT '适用端：公众号/PC官网/PC后台/小程序tabBar/H5底部导航',
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级节点',
  `title`      VARCHAR(50) NOT NULL COMMENT '标题（中文）',
  `link_type`  VARCHAR(20) NOT NULL DEFAULT '站内页' COMMENT '类型：站内页/外链/小程序页/关键词回复',
  `path`       VARCHAR(200) DEFAULT NULL COMMENT '路径或链接',
  `icon`       VARCHAR(50) DEFAULT NULL COMMENT '图标',
  `sort`       INT NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`    TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_end` (`tenant_id`, `end_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多端菜单可视化节点';

DROP TABLE IF EXISTS `sys_entry`;
CREATE TABLE `sys_entry` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `entry_type`   VARCHAR(30) NOT NULL COMMENT '类型：小程序码/公众号二维码/H5链接/App下载',
  `name`         VARCHAR(100) NOT NULL COMMENT '名称',
  `store_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '门店',
  `channel_code` VARCHAR(50) DEFAULT NULL COMMENT '渠道参数（来源归因）',
  `image_key`    VARCHAR(200) DEFAULT NULL COMMENT '码图键',
  `url`          VARCHAR(300) DEFAULT NULL COMMENT '链接地址',
  `scan_count`   INT NOT NULL DEFAULT 0 COMMENT '扫码次数',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`, `entry_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='端入口与带参码';

DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `object_key`   VARCHAR(300) NOT NULL COMMENT '对象键（tenants/{租户标识}/...）',
  `original_name` VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
  `content_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
  `size_bytes`   BIGINT DEFAULT NULL COMMENT '大小',
  `driver`       VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '存储驱动',
  `biz_type`     VARCHAR(50) DEFAULT NULL COMMENT '业务类型：房型图/合同/护理照片/资质',
  `uploader_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '上传人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `object_key`),
  KEY `idx_tenant_biz` (`tenant_id`, `biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件台账';

DROP TABLE IF EXISTS `sys_export_log`;
CREATE TABLE `sys_export_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `module`       VARCHAR(50) NOT NULL COMMENT '导出模块',
  `purpose`      VARCHAR(200) NOT NULL COMMENT '导出用途（必填）',
  `row_count`    INT NOT NULL DEFAULT 0 COMMENT '导出行数（单次上限 5000）',
  `watermark`    VARCHAR(100) DEFAULT NULL COMMENT '水印内容',
  `operator_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导出时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据导出留痕';
