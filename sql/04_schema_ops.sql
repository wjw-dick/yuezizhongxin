-- ============================================================
-- 04 护理/康复/餐食/人力/会员/内容表（T04）
-- ============================================================

USE `yzz_platform`;

DROP TABLE IF EXISTS `care_plan`;
CREATE TABLE `care_plan` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `customer_name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
  `room_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '房间标识',
  `care_mode`     VARCHAR(20) NOT NULL DEFAULT '一对一专护' COMMENT '护理模式',
  `template_code` VARCHAR(50) DEFAULT NULL COMMENT '模板编码',
  `status`        VARCHAR(20) NOT NULL DEFAULT '执行中' COMMENT '状态：执行中/已结束',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='护理计划表';

DROP TABLE IF EXISTS `care_plan_item`;
CREATE TABLE `care_plan_item` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `plan_id`       BIGINT UNSIGNED NOT NULL COMMENT '护理计划标识',
  `item_name`     VARCHAR(50) NOT NULL COMMENT '护理项目（字典中文名）',
  `target_type`   VARCHAR(20) NOT NULL DEFAULT '产妇' COMMENT '对象：产妇/新生儿',
  `plan_date`     DATE NOT NULL COMMENT '计划日期',
  `time_slot`     VARCHAR(20) DEFAULT NULL COMMENT '时段',
  `nurse_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '执行护理师',
  `status`        VARCHAR(20) NOT NULL DEFAULT '待执行' COMMENT '状态：待执行/已完成/已跳过',
  `finished_time` DATETIME DEFAULT NULL COMMENT '完成时间（服务端时间戳）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_plan` (`tenant_id`, `plan_id`),
  KEY `idx_tenant_date` (`tenant_id`, `plan_date`),
  KEY `idx_nurse` (`nurse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='护理计划项表';

DROP TABLE IF EXISTS `care_record`;
CREATE TABLE `care_record` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `plan_item_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '护理计划项标识',
  `order_id`        BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `target_type`     VARCHAR(20) NOT NULL DEFAULT '产妇' COMMENT '对象：产妇/新生儿',
  `item_name`       VARCHAR(50) NOT NULL COMMENT '护理项目',
  `temperature`     DECIMAL(4,1) DEFAULT NULL COMMENT '体温（℃）',
  `blood_pressure`  VARCHAR(20) DEFAULT NULL COMMENT '血压',
  `pulse`           INT DEFAULT NULL COMMENT '脉搏',
  `weight`          DECIMAL(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `feeding_amount`  DECIMAL(6,2) DEFAULT NULL COMMENT '喂养量（ml）',
  `jaundice_value`  DECIMAL(5,2) DEFAULT NULL COMMENT '黄疸测值',
  `stool_count`     INT DEFAULT NULL COMMENT '大小便次数',
  `content`         VARCHAR(1000) DEFAULT NULL COMMENT '记录内容',
  `photos`          JSON DEFAULT NULL COMMENT '照片（对象存储键数组）',
  `nurse_id`        BIGINT UNSIGNED NOT NULL COMMENT '提交人',
  `submit_time`     DATETIME NOT NULL COMMENT '提交时间（服务端时间戳）',
  `backfill_reason` VARCHAR(200) DEFAULT NULL COMMENT '补录原因（超时补录必填）',
  `status`          VARCHAR(20) NOT NULL DEFAULT '已提交' COMMENT '状态：已提交/待审批',
  `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`),
  KEY `idx_tenant_submit` (`tenant_id`, `submit_time`),
  KEY `idx_nurse` (`nurse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='护理记录表（24 小时内可改并留痕）';

DROP TABLE IF EXISTS `care_handover`;
CREATE TABLE `care_handover` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `shift_date`      DATE NOT NULL COMMENT '班次日期',
  `shift_type`      VARCHAR(20) NOT NULL COMMENT '班次：早班/中班/夜班',
  `from_nurse_id`   BIGINT UNSIGNED NOT NULL COMMENT '交班人',
  `to_nurse_id`     BIGINT UNSIGNED NOT NULL COMMENT '接班人',
  `watch_points`    VARCHAR(1000) DEFAULT NULL COMMENT '重点观察对象',
  `unfinished`      VARCHAR(1000) DEFAULT NULL COMMENT '未完成事项',
  `abnormal_notes`  VARCHAR(1000) DEFAULT NULL COMMENT '异常延续',
  `from_sign`       VARCHAR(200) DEFAULT NULL COMMENT '交班签名图键',
  `to_sign`         VARCHAR(200) DEFAULT NULL COMMENT '接班签名图键',
  `confirmed_time`  DATETIME DEFAULT NULL COMMENT '接班确认时间',
  `status`          VARCHAR(20) NOT NULL DEFAULT '待确认' COMMENT '状态：待确认/已确认',
  `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_date` (`tenant_id`, `shift_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交接班表';

DROP TABLE IF EXISTS `care_abnormal`;
CREATE TABLE `care_abnormal` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `abnormal_type` VARCHAR(30) NOT NULL COMMENT '异常类型（字典中文名）',
  `urgency`       VARCHAR(20) NOT NULL DEFAULT '一般' COMMENT '紧急程度：一般/紧急/特急',
  `description`   VARCHAR(1000) NOT NULL COMMENT '描述',
  `photos`        JSON DEFAULT NULL COMMENT '照片',
  `reporter_id`   BIGINT UNSIGNED NOT NULL COMMENT '上报人',
  `supervisor_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '护理主管',
  `handle_result` VARCHAR(1000) DEFAULT NULL COMMENT '处置记录',
  `family_notified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否告知家属',
  `status`        VARCHAR(20) NOT NULL DEFAULT '待处理' COMMENT '状态：待处理/处理中/已闭环',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `closed_time`   DATETIME DEFAULT NULL COMMENT '闭环时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_created` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='护理异常单表';

DROP TABLE IF EXISTS `rehab_item`;
CREATE TABLE `rehab_item` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`           VARCHAR(100) NOT NULL COMMENT '项目名称',
  `stage`          VARCHAR(30) NOT NULL COMMENT '适用阶段（字典中文名）',
  `duration_min`   INT NOT NULL DEFAULT 30 COMMENT '单次时长（分钟）',
  `course_times`   INT NOT NULL DEFAULT 1 COMMENT '疗程次数',
  `price`          DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '价格',
  `qualification`  VARCHAR(100) DEFAULT NULL COMMENT '人员资质要求',
  `status`         VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='康复项目库';

DROP TABLE IF EXISTS `rehab_assess`;
CREATE TABLE `rehab_assess` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`       BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `pelvic_score`   INT DEFAULT NULL COMMENT '盆底肌评分',
  `diastasis_cm`   DECIMAL(4,1) DEFAULT NULL COMMENT '腹直肌分离（cm）',
  `posture`        VARCHAR(200) DEFAULT NULL COMMENT '体态评估',
  `mood_score`     INT DEFAULT NULL COMMENT '情绪量表得分',
  `report`         TEXT COMMENT '评估报告与建议疗程',
  `assessor_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '评估师',
  `assess_time`    DATETIME NOT NULL COMMENT '评估时间',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='康复评估表';

DROP TABLE IF EXISTS `rehab_course`;
CREATE TABLE `rehab_course` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`       BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `item_id`        BIGINT UNSIGNED NOT NULL COMMENT '康复项目',
  `total_times`    INT NOT NULL DEFAULT 0 COMMENT '总次数',
  `used_times`     INT NOT NULL DEFAULT 0 COMMENT '已用次数',
  `expire_date`    DATE DEFAULT NULL COMMENT '有效期',
  `source`         VARCHAR(20) NOT NULL DEFAULT '套餐赠送' COMMENT '来源：套餐赠送/单独购买',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='疗程卡表';

DROP TABLE IF EXISTS `rehab_appointment`;
CREATE TABLE `rehab_appointment` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `course_id`     BIGINT UNSIGNED NOT NULL COMMENT '疗程卡标识',
  `room_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '康复室',
  `therapist_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '治疗师',
  `appoint_date`  DATE NOT NULL COMMENT '预约日期',
  `start_time`    VARCHAR(10) NOT NULL COMMENT '开始时间',
  `end_time`      VARCHAR(10) NOT NULL COMMENT '结束时间',
  `status`        VARCHAR(20) NOT NULL DEFAULT '已预约' COMMENT '状态：已预约/已签到/已取消/已完成',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_date` (`tenant_id`, `appoint_date`),
  KEY `idx_therapist_time` (`therapist_id`, `appoint_date`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='康复预约表（治疗师与时段冲突校验）';

DROP TABLE IF EXISTS `rehab_record`;
CREATE TABLE `rehab_record` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `appointment_id` BIGINT UNSIGNED NOT NULL COMMENT '预约标识',
  `item_name`     VARCHAR(100) NOT NULL COMMENT '项目名称',
  `duration_min`  INT DEFAULT NULL COMMENT '实际时长',
  `therapist_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '治疗师',
  `feedback`      VARCHAR(500) DEFAULT NULL COMMENT '客户反馈',
  `next_appoint`  DATE DEFAULT NULL COMMENT '下次预约日期',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_appointment` (`tenant_id`, `appointment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='康复执行记录表';

DROP TABLE IF EXISTS `meal_template`;
CREATE TABLE `meal_template` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `stage`       VARCHAR(30) NOT NULL COMMENT '餐食阶段：排恶露期/调理期/滋补期/塑形期',
  `day_index`   INT NOT NULL DEFAULT 1 COMMENT '阶段内第几天',
  `meal_type`   VARCHAR(20) NOT NULL COMMENT '餐次：早餐/上午加餐/午餐/下午加餐/晚餐/晚间加餐',
  `dishes`      VARCHAR(500) NOT NULL COMMENT '菜品清单',
  `avoid_tags`  VARCHAR(200) DEFAULT NULL COMMENT '本餐忌口标签',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_stage` (`tenant_id`, `stage`, `day_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月子餐模板表';

DROP TABLE IF EXISTS `meal_order`;
CREATE TABLE `meal_order` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`     BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `room_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '房间',
  `meal_date`    DATE NOT NULL COMMENT '用餐日期',
  `meal_type`    VARCHAR(20) NOT NULL COMMENT '餐次',
  `dishes`       VARCHAR(500) DEFAULT NULL COMMENT '实际菜品',
  `special_tag`  VARCHAR(100) DEFAULT NULL COMMENT '特殊标记：糖尿病餐/素食/宗教忌口',
  `extra_charge` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '加餐收费',
  `checked`      TINYINT NOT NULL DEFAULT 0 COMMENT '是否出餐核对',
  `abnormal`     VARCHAR(200) DEFAULT NULL COMMENT '异常回传',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_date` (`tenant_id`, `meal_date`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日出餐表';

DROP TABLE IF EXISTS `meal_restrict`;
CREATE TABLE `meal_restrict` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单标识',
  `restrict_type` VARCHAR(30) NOT NULL COMMENT '忌口类型（字典中文名）',
  `content`     VARCHAR(300) NOT NULL COMMENT '忌口内容/过敏原',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='忌口与过敏原表';

DROP TABLE IF EXISTS `staff_info`;
CREATE TABLE `staff_info` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`         VARCHAR(50) NOT NULL COMMENT '姓名',
  `phone`        VARCHAR(64) DEFAULT NULL COMMENT '手机号（加密）',
  `job_type`     VARCHAR(30) NOT NULL COMMENT '岗位：护理师/月嫂/育婴师/护士/营养师/保洁',
  `user_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '关联登录账号',
  `status`       VARCHAR(20) NOT NULL DEFAULT '在职' COMMENT '状态：在职/离职',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_job` (`tenant_id`, `job_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员档案表';

DROP TABLE IF EXISTS `staff_cert`;
CREATE TABLE `staff_cert` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `staff_id`     BIGINT UNSIGNED NOT NULL COMMENT '人员标识',
  `cert_type`    VARCHAR(50) NOT NULL COMMENT '证书类型：育婴员/护士执业证书/母婴护理培训证书',
  `cert_no`      VARCHAR(64) DEFAULT NULL COMMENT '证书编号',
  `file_key`     VARCHAR(200) DEFAULT NULL COMMENT '证书扫描件（对象存储键）',
  `expire_date`  DATE DEFAULT NULL COMMENT '到期日期，到期前 60 天提醒',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_staff` (`tenant_id`, `staff_id`),
  KEY `idx_expire` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员资质证件表';

DROP TABLE IF EXISTS `staff_schedule`;
CREATE TABLE `staff_schedule` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `staff_id`     BIGINT UNSIGNED NOT NULL COMMENT '人员标识',
  `schedule_date` DATE NOT NULL COMMENT '排班日期',
  `shift_type`   VARCHAR(20) NOT NULL COMMENT '班次：早班/中班/夜班',
  `floor`        VARCHAR(10) DEFAULT NULL COMMENT '楼层',
  `order_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '一对一负责的订单',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff_date_shift` (`tenant_id`, `staff_id`, `schedule_date`, `shift_type`),
  KEY `idx_tenant_date` (`tenant_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='排班表';

DROP TABLE IF EXISTS `staff_attendance`;
CREATE TABLE `staff_attendance` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `staff_id`      BIGINT UNSIGNED NOT NULL COMMENT '人员标识',
  `attendance_date` DATE NOT NULL COMMENT '考勤日期',
  `check_in_time` DATETIME DEFAULT NULL COMMENT '上班打卡',
  `check_out_time` DATETIME DEFAULT NULL COMMENT '下班打卡',
  `location`      VARCHAR(100) DEFAULT NULL COMMENT '打卡定位（可选）',
  `leave_type`    VARCHAR(20) DEFAULT NULL COMMENT '请假类型',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_staff_date` (`tenant_id`, `staff_id`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤表';

DROP TABLE IF EXISTS `member_info`;
CREATE TABLE `member_info` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `union_id`       VARCHAR(64) DEFAULT NULL COMMENT '微信 UnionID（多端会员唯一标识）',
  `open_id`        VARCHAR(64) DEFAULT NULL COMMENT '公众号/小程序 OpenID',
  `phone`          VARCHAR(64) NOT NULL COMMENT '手机号（加密）',
  `nickname`       VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar`         VARCHAR(200) DEFAULT NULL COMMENT '头像',
  `source`         VARCHAR(50) DEFAULT NULL COMMENT '来源渠道',
  `points`         INT NOT NULL DEFAULT 0 COMMENT '积分余额',
  `balance`        DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '储值余额',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_union` (`tenant_id`, `union_id`),
  KEY `idx_tenant_phone` (`tenant_id`, `phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员表（UnionID 统一）';

DROP TABLE IF EXISTS `member_tag`;
CREATE TABLE `member_tag` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`         VARCHAR(50) NOT NULL COMMENT '标签名称',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员标签表';

DROP TABLE IF EXISTS `member_tag_rel`;
CREATE TABLE `member_tag_rel` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `member_id`  BIGINT UNSIGNED NOT NULL COMMENT '会员标识',
  `tag_id`     BIGINT UNSIGNED NOT NULL COMMENT '标签标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member_tag` (`tenant_id`, `member_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员标签关联表';

DROP TABLE IF EXISTS `member_point_log`;
CREATE TABLE `member_point_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`    BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '会员标识',
  `change_value` INT NOT NULL COMMENT '变动值（正增负减）',
  `reason`       VARCHAR(100) NOT NULL COMMENT '变动原因',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_member` (`tenant_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员积分流水';

DROP TABLE IF EXISTS `mkt_coupon`;
CREATE TABLE `mkt_coupon` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `name`          VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `coupon_type`   VARCHAR(20) NOT NULL COMMENT '类型：新人券/满减券/续住折扣',
  `amount`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  `threshold`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛',
  `total_count`   INT NOT NULL DEFAULT 0 COMMENT '发行总量',
  `issued_count`  INT NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `start_time`    DATETIME DEFAULT NULL COMMENT '生效时间',
  `end_time`      DATETIME DEFAULT NULL COMMENT '失效时间',
  `status`        VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `created_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券模板表';

DROP TABLE IF EXISTS `member_coupon`;
CREATE TABLE `member_coupon` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `member_id`  BIGINT UNSIGNED NOT NULL COMMENT '会员标识',
  `coupon_id`  BIGINT UNSIGNED NOT NULL COMMENT '优惠券标识',
  `status`     VARCHAR(20) NOT NULL DEFAULT '未使用' COMMENT '状态：未使用/已使用/已过期',
  `used_time`  DATETIME DEFAULT NULL COMMENT '使用时间',
  `order_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '核销订单',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领券时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_member` (`tenant_id`, `member_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员优惠券表';

DROP TABLE IF EXISTS `member_recharge`;
CREATE TABLE `member_recharge` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `member_id`      BIGINT UNSIGNED NOT NULL COMMENT '会员标识',
  `recharge_no`    VARCHAR(40) NOT NULL COMMENT '储值单号',
  `amount`         DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '储值金额',
  `gift_amount`    DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '赠送金额',
  `status`         VARCHAR(20) NOT NULL DEFAULT '已支付' COMMENT '状态：已支付/已退款',
  `rule_shown`     TINYINT NOT NULL DEFAULT 1 COMMENT '是否已展示预付卡规则',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_recharge_no` (`tenant_id`, `recharge_no`),
  KEY `idx_tenant_member` (`tenant_id`, `member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='储值记录表（预付卡规则公示留痕）';

DROP TABLE IF EXISTS `mkt_referral`;
CREATE TABLE `mkt_referral` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `referrer_member` BIGINT UNSIGNED NOT NULL COMMENT '推荐人会员标识',
  `referred_lead`   BIGINT UNSIGNED DEFAULT NULL COMMENT '被推荐线索',
  `reward_points`   INT NOT NULL DEFAULT 0 COMMENT '奖励积分',
  `reward_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '奖励金额',
  `status`          VARCHAR(20) NOT NULL DEFAULT '待结算' COMMENT '状态：待结算/已结算',
  `created_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_referrer` (`tenant_id`, `referrer_member`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='转介绍奖励表';

DROP TABLE IF EXISTS `cms_content`;
CREATE TABLE `cms_content` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `content_type`   VARCHAR(30) NOT NULL COMMENT '类型：Banner/品牌故事/房型/康复/餐食/团队/案例/常见问题/资讯',
  `title`          VARCHAR(200) NOT NULL COMMENT '标题',
  `cover`          VARCHAR(200) DEFAULT NULL COMMENT '封面图键',
  `body_html`      LONGTEXT COMMENT '富文本正文',
  `apply_ends`     VARCHAR(100) DEFAULT NULL COMMENT '适用端：官网/公众号/小程序/H5',
  `seo_title`      VARCHAR(200) DEFAULT NULL COMMENT 'SEO 标题',
  `seo_keywords`   VARCHAR(300) DEFAULT NULL COMMENT 'SEO 关键词',
  `seo_description` VARCHAR(500) DEFAULT NULL COMMENT 'SEO 描述',
  `authorized`     TINYINT NOT NULL DEFAULT 0 COMMENT '案例是否已取得客户授权',
  `status`         VARCHAR(20) NOT NULL DEFAULT '草稿' COMMENT '状态：草稿/待审核/已发布/已下线',
  `published_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `created_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type` (`tenant_id`, `content_type`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容表';

DROP TABLE IF EXISTS `cms_content_i18n`;
CREATE TABLE `cms_content_i18n` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户标识',
  `content_id`  BIGINT UNSIGNED NOT NULL COMMENT '内容标识',
  `lang_code`   VARCHAR(20) NOT NULL COMMENT '语言编码',
  `title`       VARCHAR(200) DEFAULT NULL COMMENT '标题',
  `body_html`   LONGTEXT COMMENT '正文',
  `translated`  TINYINT NOT NULL DEFAULT 0 COMMENT '是否已翻译',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_content_lang` (`tenant_id`, `content_id`, `lang_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容多语言表';
