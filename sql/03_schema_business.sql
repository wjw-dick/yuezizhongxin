-- ============================================================
-- 03 业务核心表（T04）房态/套餐商品/线索/订单/入出馆
-- 唯一约束一律带 tenant_id，禁止跨租户重复
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `room_type`;
CREATE TABLE `room_type` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`         VARCHAR(50)  NOT NULL COMMENT '房型名称',
  `level`        VARCHAR(20)  NOT NULL DEFAULT '温馨单间' COMMENT '房型档位：温馨单间/豪华套间/尊享套房',
  `area`         DECIMAL(6,2) DEFAULT NULL COMMENT '面积（㎡）',
  `max_guest`    INT          NOT NULL DEFAULT 2 COMMENT '可住人数',
  `bed_type`     VARCHAR(50)  DEFAULT NULL COMMENT '床型',
  `facilities`   VARCHAR(500) DEFAULT NULL COMMENT '房间配置：独立卫浴/家属陪护床/冰箱',
  `images`       JSON         DEFAULT NULL COMMENT '实景图集（对象存储键数组）',
  `description`  TEXT         COMMENT '房型介绍（富文本）',
  `status`       VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `sort`         INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房型表';

DROP TABLE IF EXISTS `room`;
CREATE TABLE `room` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `room_no`       VARCHAR(20) NOT NULL COMMENT '房间编号',
  `room_type_id`  BIGINT UNSIGNED NOT NULL COMMENT '房型标识',
  `floor`         VARCHAR(10) DEFAULT NULL COMMENT '楼层',
  `status`        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '房态：AVAILABLE可预订/LOCKED已锁定/OCCUPIED已入住/CLEANING待清洁/MAINTENANCE维修中',
  `remark`        VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_room_no` (`tenant_id`, `room_no`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_type` (`tenant_id`, `room_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房间台账表';

DROP TABLE IF EXISTS `room_schedule`;
CREATE TABLE `room_schedule` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `room_id`          BIGINT UNSIGNED NOT NULL COMMENT '房间标识',
  `biz_date`         DATE NOT NULL COMMENT '档期日期',
  `state`            VARCHAR(20) NOT NULL DEFAULT 'LOCKED' COMMENT '档期状态：LOCKED已锁定/OCCUPIED已入住/CLEANING待清洁/MAINTENANCE维修中',
  `order_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '占用订单',
  `hold_expire_time` DATETIME DEFAULT NULL COMMENT '销售预留过期时间，超时由队列自动释放',
  `remark`           VARCHAR(200) DEFAULT NULL COMMENT '备注（换房、延期等留痕）',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_room_date` (`tenant_id`, `room_id`, `biz_date`),
  KEY `idx_tenant_date` (`tenant_id`, `biz_date`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房间档期表（同房同日期唯一占用，防并发抢房）';

DROP TABLE IF EXISTS `pkg_package`;
CREATE TABLE `pkg_package` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`             VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `days`             INT NOT NULL DEFAULT 28 COMMENT '套餐天数：28/42/自定义',
  `room_type_level`  VARCHAR(20) DEFAULT NULL COMMENT '适用房型档位',
  `care_mode`        VARCHAR(20) NOT NULL DEFAULT '一对一专护' COMMENT '护理模式：一对一专护/一对多轮值',
  `meal_standard`    VARCHAR(20) DEFAULT NULL COMMENT '月子餐标准：三餐三点/三餐两点',
  `rehab_items`      VARCHAR(500) DEFAULT NULL COMMENT '含康复项目清单',
  `price`            DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '套餐价格（元）',
  `deposit_ratio`    DECIMAL(5,2) NOT NULL DEFAULT 30.00 COMMENT '定金比例（%）',
  `store_ids`        VARCHAR(200) DEFAULT NULL COMMENT '可售门店标识，逗号分隔',
  `status`           VARCHAR(20) NOT NULL DEFAULT 'OFF_SHELF' COMMENT '状态：ON_SHELF上架/OFF_SHELF下架',
  `sort`             INT NOT NULL DEFAULT 0 COMMENT '排序',
  `detail_html`      LONGTEXT COMMENT '套餐详情富文本（多语言按语言分行存储于 cms_content_i18n）',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐表';

DROP TABLE IF EXISTS `pkg_item`;
CREATE TABLE `pkg_item` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `package_id`  BIGINT UNSIGNED NOT NULL COMMENT '套餐标识',
  `item_type`   VARCHAR(20) NOT NULL COMMENT '类型：康复项目/增值服务',
  `item_name`   VARCHAR(100) NOT NULL COMMENT '项目名称',
  `quantity`    INT NOT NULL DEFAULT 1 COMMENT '含数量',
  `price`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '单价（元）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_package` (`tenant_id`, `package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐包含项表';

DROP TABLE IF EXISTS `prod_product`;
CREATE TABLE `prod_product` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`          VARCHAR(100) NOT NULL COMMENT '商品名称',
  `category`      VARCHAR(50)  DEFAULT NULL COMMENT '分类：待产包/月子服/护理耗材',
  `price`         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '售价（元）',
  `stock`         INT NOT NULL DEFAULT 0 COMMENT '库存',
  `images`        JSON DEFAULT NULL COMMENT '商品图',
  `detail_html`   LONGTEXT COMMENT '详情富文本',
  `status`        VARCHAR(20) NOT NULL DEFAULT 'OFF_SHELF' COMMENT '状态：ON_SHELF/OFF_SHELF',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品表';

DROP TABLE IF EXISTS `prod_sku`;
CREATE TABLE `prod_sku` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `product_id`  BIGINT UNSIGNED NOT NULL COMMENT '商品标识',
  `sku_name`    VARCHAR(100) NOT NULL COMMENT '规格名称',
  `price`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '规格售价',
  `stock`       INT NOT NULL DEFAULT 0 COMMENT '规格库存',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_product` (`tenant_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格表';

DROP TABLE IF EXISTS `crm_lead`;
CREATE TABLE `crm_lead` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `customer_name`     VARCHAR(50) NOT NULL COMMENT '联系人',
  `phone`             VARCHAR(64) NOT NULL COMMENT '手机号（加密存储，展示脱敏）',
  `due_date`          DATE DEFAULT NULL COMMENT '预产期',
  `store_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '意向门店',
  `intend_room_type`  VARCHAR(50) DEFAULT NULL COMMENT '意向房型',
  `budget_min`        DECIMAL(12,2) DEFAULT NULL COMMENT '预算下限',
  `budget_max`        DECIMAL(12,2) DEFAULT NULL COMMENT '预算上限',
  `source_code`       VARCHAR(50) NOT NULL COMMENT '线索来源（字典中文名）',
  `stage`             VARCHAR(20) NOT NULL DEFAULT '待联系' COMMENT '跟进阶段：待联系/已联系/已参观/已报价/已签约/已流失',
  `advisor_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '归属销售顾问',
  `next_follow_time`  DATETIME DEFAULT NULL COMMENT '下次跟进时间',
  `lost_reason`       VARCHAR(50) DEFAULT NULL COMMENT '流失原因（字典中文名，流失必填）',
  `remark`            VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_stage` (`tenant_id`, `stage`),
  KEY `idx_tenant_source` (`tenant_id`, `source_code`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`),
  KEY `idx_next_follow` (`next_follow_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线索表';

DROP TABLE IF EXISTS `crm_follow`;
CREATE TABLE `crm_follow` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `lead_id`      BIGINT UNSIGNED NOT NULL COMMENT '线索标识',
  `follow_time`  DATETIME NOT NULL COMMENT '跟进时间',
  `content`      VARCHAR(1000) DEFAULT NULL COMMENT '沟通内容',
  `result`       VARCHAR(50) DEFAULT NULL COMMENT '通话结果',
  `next_time`    DATETIME DEFAULT NULL COMMENT '下次提醒时间',
  `operator_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '跟进人',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_lead` (`tenant_id`, `lead_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线索跟进记录表';

DROP TABLE IF EXISTS `crm_visit`;
CREATE TABLE `crm_visit` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `lead_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '线索标识',
  `visit_date`    DATE NOT NULL COMMENT '参观日期',
  `time_slot`     VARCHAR(30) NOT NULL COMMENT '时段',
  `person_count`  INT NOT NULL DEFAULT 1 COMMENT '到店人数',
  `advisor_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '接待顾问',
  `checked_in`    TINYINT NOT NULL DEFAULT 0 COMMENT '是否到店签到：1 是 0 否',
  `feedback`      VARCHAR(500) DEFAULT NULL COMMENT '参观后评价',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_date` (`tenant_id`, `visit_date`),
  KEY `idx_lead` (`lead_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参观预约表';

DROP TABLE IF EXISTS `biz_order`;
CREATE TABLE `biz_order` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_no`         VARCHAR(40) NOT NULL COMMENT '订单编号（编码规则见 SRS 编码章）',
  `lead_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '来源线索',
  `customer_name`    VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `phone`            VARCHAR(64) NOT NULL COMMENT '客户手机号（加密）',
  `package_id`       BIGINT UNSIGNED NOT NULL COMMENT '套餐标识',
  `room_type_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '房型标识',
  `room_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '房间标识',
  `checkin_date`     DATE NOT NULL COMMENT '入住日期',
  `checkout_date`    DATE NOT NULL COMMENT '出馆日期',
  `care_mode`        VARCHAR(20) NOT NULL DEFAULT '一对一专护' COMMENT '护理模式',
  `companion_count`  INT NOT NULL DEFAULT 1 COMMENT '陪护人数',
  `package_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '套餐费',
  `extra_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '增值服务费',
  `product_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '商品费',
  `discount_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  `total_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应收总额',
  `deposit_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '定金',
  `tail_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '尾款',
  `bond_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '押金',
  `paid_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '已收金额',
  `status`           VARCHAR(20) NOT NULL DEFAULT '待支付' COMMENT '订单状态：待支付/待入住/在住/待结算/已完成/已取消/已退订',
  `source`           VARCHAR(50) DEFAULT NULL COMMENT '订单来源：线上下单/到店签约/续住加单',
  `sign_type`        VARCHAR(20) DEFAULT NULL COMMENT '签约方式',
  `remark`           VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by`       BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order_no` (`tenant_id`, `order_no`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_checkin` (`tenant_id`, `checkin_date`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

DROP TABLE IF EXISTS `biz_order_item`;
CREATE TABLE `biz_order_item` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `item_type`   VARCHAR(20) NOT NULL COMMENT '类型：套餐/增值服务/商品/康复项目',
  `item_name`   VARCHAR(100) NOT NULL COMMENT '项目名称',
  `quantity`    INT NOT NULL DEFAULT 1 COMMENT '数量',
  `price`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '单价',
  `amount`      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

DROP TABLE IF EXISTS `biz_payment`;
CREATE TABLE `biz_payment` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `payment_no`    VARCHAR(40) NOT NULL COMMENT '收款单号',
  `pay_type`      VARCHAR(20) NOT NULL COMMENT '款项类型：定金/尾款/押金',
  `method`        VARCHAR(20) NOT NULL COMMENT '支付方式：微信支付/银行转账/现金/对公转账',
  `amount`        DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '金额',
  `status`        VARCHAR(20) NOT NULL DEFAULT '待支付' COMMENT '状态：待支付/已支付/支付异常/已退款',
  `trade_no`      VARCHAR(64) DEFAULT NULL COMMENT '微信支付交易号',
  `paid_time`     DATETIME DEFAULT NULL COMMENT '支付时间',
  `operator_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '收款人',
  `remark`        VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_payment_no` (`tenant_id`, `payment_no`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收款单表';

DROP TABLE IF EXISTS `biz_refund`;
CREATE TABLE `biz_refund` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`         BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `refund_no`        VARCHAR(40) NOT NULL COMMENT '退款单号',
  `amount`           DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '退款金额',
  `penalty_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '违约金',
  `reason_code`      VARCHAR(50) NOT NULL COMMENT '退款原因（字典中文名）',
  `reason`           VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
  `status`           VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '状态：待审核/已通过/已驳回/已退款',
  `audit_by`         BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人',
  `audit_time`       DATETIME DEFAULT NULL COMMENT '审核时间',
  `created_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_refund_no` (`tenant_id`, `refund_no`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单表';

DROP TABLE IF EXISTS `stay_checkin`;
CREATE TABLE `stay_checkin` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`            BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`             BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `room_id`              BIGINT UNSIGNED NOT NULL COMMENT '房间标识',
  `mother_name`          VARCHAR(50) NOT NULL COMMENT '产妇姓名',
  `mother_age`           INT DEFAULT NULL COMMENT '产妇年龄',
  `gestational_week`     INT DEFAULT NULL COMMENT '孕周',
  `delivery_type`        VARCHAR(20) DEFAULT NULL COMMENT '分娩方式',
  `delivery_date`        DATE DEFAULT NULL COMMENT '分娩日期',
  `baby_birth_date`      DATE DEFAULT NULL COMMENT '新生儿出生日期',
  `baby_weight`          DECIMAL(5,2) DEFAULT NULL COMMENT '新生儿体重（kg）',
  `feeding_type`         VARCHAR(20) DEFAULT NULL COMMENT '喂养方式',
  `family_info`          VARCHAR(500) DEFAULT NULL COMMENT '家属与陪护人',
  `emergency_contact`    VARCHAR(100) DEFAULT NULL COMMENT '紧急联系人',
  `id_card_no`           VARCHAR(255) DEFAULT NULL COMMENT '证件号（加密存储）',
  `health_notice_signed` TINYINT NOT NULL DEFAULT 0 COMMENT '健康告知与知情同意是否签署',
  `deposit_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '押金金额',
  `checkin_time`         DATETIME NOT NULL COMMENT '入馆时间',
  `status`               VARCHAR(20) NOT NULL DEFAULT '在住' COMMENT '状态：在住/已出馆',
  `created_time`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`),
  KEY `idx_tenant_room` (`tenant_id`, `room_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入馆登记表';

DROP TABLE IF EXISTS `stay_checkout`;
CREATE TABLE `stay_checkout` (
  `id`                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`              BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`               BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `checkin_id`             BIGINT UNSIGNED NOT NULL COMMENT '入馆登记标识',
  `tail_amount`            DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '尾款',
  `extra_amount`           DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '增值消费',
  `deposit_refund`         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '退还押金',
  `item_returned`          TINYINT NOT NULL DEFAULT 0 COMMENT '物品是否归还齐全',
  `summary`                TEXT COMMENT '出馆小结',
  `satisfaction`           INT DEFAULT NULL COMMENT '满意度评分（1~5）',
  `review_appointment`     DATE DEFAULT NULL COMMENT '42 天复查预约日期',
  `checkout_time`          DATETIME NOT NULL COMMENT '出馆时间',
  `operator_id`            BIGINT UNSIGNED DEFAULT NULL COMMENT '经办人',
  `created_time`           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出馆结算表';
