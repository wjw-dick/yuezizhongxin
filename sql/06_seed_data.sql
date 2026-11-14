-- ============================================================
-- 06 初始化数据（T06）
-- 1) 字典：全部中文名称，页面与导出只展示中文名
-- 2) 平台侧：三角色、菜单树、套餐、演示账号
-- 3) 租户侧：九个角色、默认菜单、默认参数、演示主管理员
-- 演示口令统一为 Admin@123456（BCrypt 散列），上线前必须修改
-- ============================================================

USE `yzz_platform`;
SET NAMES utf8mb4;

-- ---------- 1. 字典分类 ----------
INSERT INTO `sys_dict_category` (`category`, `name`, `sort`) VALUES
('tenant_status', '租户状态', 10),
('room_state', '房态', 20),
('order_status', '订单状态', 30),
('pay_type', '支付方式', 40),
('lead_stage', '线索阶段', 50),
('lead_source', '线索来源', 60),
('care_mode', '护理模式', 70),
('care_item', '护理项目', 80),
('rehab_stage', '康复阶段', 90),
('meal_stage', '餐食阶段', 100),
('meal_restrict', '忌口类型', 110),
('abnormal_type', '异常类型', 120),
('mp_msg_type', '消息类型', 130),
('mp_sender', '发送方', 140),
('publish_status', '发表状态', 150),
('subscribe_status', '订阅状态', 160),
('refund_reason', '退款原因', 170),
('lost_reason', '流失原因', 180),
('bill_status', '账单状态', 190),
('staff_job', '岗位类型', 200);

-- ---------- 2. 字典项 ----------
INSERT INTO `sys_dict_item` (`category`, `item_code`, `item_name`, `color`, `sort`) VALUES
('tenant_status', 'TRIAL', '试用中', '#409EFF', 10),
('tenant_status', 'NORMAL', '正常', '#67C23A', 20),
('tenant_status', 'ARREARS', '欠费', '#E6A23C', 30),
('tenant_status', 'DISABLED', '停用', '#909399', 40),
('tenant_status', 'EXPIRED', '已到期', '#F56C6C', 50),

('room_state', 'AVAILABLE', '可预订', '#67C23A', 10),
('room_state', 'LOCKED', '已锁定', '#E6A23C', 20),
('room_state', 'OCCUPIED', '已入住', '#409EFF', 30),
('room_state', 'CLEANING', '待清洁', '#909399', 40),
('room_state', 'MAINTENANCE', '维修中', '#F56C6C', 50),

('order_status', 'PENDING_PAY', '待支付', '#E6A23C', 10),
('order_status', 'PENDING_IN', '待入住', '#409EFF', 20),
('order_status', 'IN_STAY', '在住', '#67C23A', 30),
('order_status', 'PENDING_SETTLE', '待结算', '#E6A23C', 40),
('order_status', 'FINISHED', '已完成', '#909399', 50),
('order_status', 'CANCELLED', '已取消', '#909399', 60),
('order_status', 'REFUNDED', '已退订', '#F56C6C', 70),

('pay_type', 'WECHAT', '微信支付', NULL, 10),
('pay_type', 'BANK', '银行转账', NULL, 20),
('pay_type', 'CASH', '现金', NULL, 30),
('pay_type', 'CORP', '对公转账', NULL, 40),

('lead_stage', 'TO_CONTACT', '待联系', '#409EFF', 10),
('lead_stage', 'CONTACTED', '已联系', '#409EFF', 20),
('lead_stage', 'VISITED', '已参观', '#E6A23C', 30),
('lead_stage', 'QUOTED', '已报价', '#E6A23C', 40),
('lead_stage', 'SIGNED', '已签约', '#67C23A', 50),
('lead_stage', 'LOST', '已流失', '#909399', 60),

('lead_source', 'OFFICIAL_SITE', '官网表单', NULL, 10),
('lead_source', 'MP', '公众号', NULL, 20),
('lead_source', 'MINI_PROGRAM', '小程序', NULL, 30),
('lead_source', 'H5', 'H5', NULL, 40),
('lead_source', 'PHONE', '电话咨询', NULL, 50),
('lead_source', 'STORE', '到店', NULL, 60),
('lead_source', 'REFERRAL', '转介绍', NULL, 70),
('lead_source', 'COOPERATION', '异业合作', NULL, 80),
('lead_source', 'AD', '广告投放', NULL, 90),

('care_mode', 'ONE_TO_ONE', '一对一专护', NULL, 10),
('care_mode', 'ONE_TO_MANY', '一对多轮值', NULL, 20),

('care_item', 'MOTHER_TEMP', '产妇体温', NULL, 10),
('care_item', 'MOTHER_BP', '血压', NULL, 20),
('care_item', 'LOCHIA', '恶露观察', NULL, 30),
('care_item', 'WOUND', '伤口护理', NULL, 40),
('care_item', 'BREAST', '乳房护理', NULL, 50),
('care_item', 'UTERUS', '子宫恢复', NULL, 60),
('care_item', 'MOOD', '情绪观察', NULL, 70),
('care_item', 'BABY_TEMP', '新生儿体温', NULL, 80),
('care_item', 'BABY_WEIGHT', '体重', NULL, 90),
('care_item', 'FEEDING', '喂养记录', NULL, 100),
('care_item', 'STOOL', '大小便记录', NULL, 110),
('care_item', 'JAUNDICE', '黄疸测值', NULL, 120),
('care_item', 'UMBILICAL', '脐部护理', NULL, 130),
('care_item', 'TOUCH', '抚触', NULL, 140),
('care_item', 'SWIM', '游泳', NULL, 150),

('rehab_stage', 'WEEK1', '产后第一周', NULL, 10),
('rehab_stage', 'WEEK2', '产后第二周', NULL, 20),
('rehab_stage', 'WEEK3_4', '产后第三至四周', NULL, 30),
('rehab_stage', 'AFTER', '出馆后', NULL, 40),

('meal_stage', 'DRAIN', '排恶露期', NULL, 10),
('meal_stage', 'REGULATE', '调理期', NULL, 20),
('meal_stage', 'NOURISH', '滋补期', NULL, 30),
('meal_stage', 'SHAPE', '塑形期', NULL, 40),

('meal_restrict', 'RELIGION', '宗教忌口', NULL, 10),
('meal_restrict', 'ALLERGEN', '过敏原', NULL, 20),
('meal_restrict', 'DIABETES', '妊娠糖尿病餐', NULL, 30),
('meal_restrict', 'VEGAN', '素食', NULL, 40),
('meal_restrict', 'OTHER', '其他', NULL, 50),

('abnormal_type', 'MOTHER', '产妇异常', '#F56C6C', 10),
('abnormal_type', 'BABY', '新生儿异常', '#F56C6C', 20),
('abnormal_type', 'ENV', '环境安全', '#E6A23C', 30),
('abnormal_type', 'OTHER', '其他', '#909399', 40),

('mp_msg_type', 'text', '文本', NULL, 10),
('mp_msg_type', 'image', '图片', NULL, 20),
('mp_msg_type', 'voice', '语音', NULL, 30),
('mp_msg_type', 'video', '视频', NULL, 40),
('mp_msg_type', 'news', '图文', NULL, 50),
('mp_msg_type', 'event', '事件', NULL, 60),

('mp_sender', 'USER', '用户', NULL, 10),
('mp_sender', 'MP', '公众号', NULL, 20),

('publish_status', 'SUCCESS', '成功', '#67C23A', 10),
('publish_status', 'FAIL', '失败', '#F56C6C', 20),
('publish_status', 'AUDITING', '审核中', '#E6A23C', 30),

('subscribe_status', 'SUBSCRIBED', '已关注', '#67C23A', 10),
('subscribe_status', 'UNSUBSCRIBED', '已取消关注', '#909399', 20),

('refund_reason', 'CUSTOMER_CANCEL', '客户主动取消', NULL, 10),
('refund_reason', 'SCHEDULE_CONFLICT', '档期冲突', NULL, 20),
('refund_reason', 'DISSATISFIED', '服务不满意', NULL, 30),
('refund_reason', 'DUPLICATE_PAY', '重复支付', NULL, 40),
('refund_reason', 'OTHER', '其他', NULL, 50),

('lost_reason', 'PRICE', '价格因素', NULL, 10),
('lost_reason', 'DISTANCE', '距离因素', NULL, 20),
('lost_reason', 'OTHER_ORG', '选择了其他机构', NULL, 30),
('lost_reason', 'POSTPONE', '暂缓入住', NULL, 40),
('lost_reason', 'UNREACHABLE', '联系不上', NULL, 50),

('bill_status', 'PENDING', '待支付', '#E6A23C', 10),
('bill_status', 'PAID', '已支付', '#67C23A', 20),
('bill_status', 'OVERDUE', '已逾期', '#F56C6C', 30),

('staff_job', 'NURSE', '护理师', NULL, 10),
('staff_job', 'MATERNITY', '月嫂', NULL, 20),
('staff_job', 'CHILDCARE', '育婴师', NULL, 30),
('staff_job', 'REGISTERED_NURSE', '护士', NULL, 40),
('staff_job', 'NUTRITIONIST', '营养师', NULL, 50),
('staff_job', 'CLEANER', '保洁', NULL, 60);

-- ---------- 3. 语言 ----------
INSERT INTO `sys_language` (`code`, `name`, `is_default`, `enabled`, `sort`) VALUES
('zh-CN', '简体中文', 1, 1, 10),
('en-US', 'English', 0, 1, 20);

-- ---------- 4. 平台账号（演示口令 Admin@123456） ----------
INSERT INTO `platform_user` (`username`, `password`, `real_name`, `role_code`, `status`) VALUES
('platform', '$2a$10$J83QpEfchT.8zn7yPQ55tO6Yp97XJ.z4bghs8r4cgeWixzJ0Ld1qG', '平台超级管理员', 'platform_super', 'ENABLED'),
('oper', '$2a$10$J83QpEfchT.8zn7yPQ55tO6Yp97XJ.z4bghs8r4cgeWixzJ0Ld1qG', '平台运营', 'platform_oper', 'ENABLED'),
('finance', '$2a$10$J83QpEfchT.8zn7yPQ55tO6Yp97XJ.z4bghs8r4cgeWixzJ0Ld1qG', '平台财务', 'platform_fin', 'ENABLED');

-- ---------- 5. 平台菜单与角色授权 ----------
INSERT INTO `platform_menu` (`id`, `parent_id`, `title`, `path`, `icon`, `menu_type`, `sort`, `visible`) VALUES
(1, 0, '平台看板', '/platform/dashboard', 'dashboard', '菜单', 10, 1),
(2, 0, '租户管理', '/platform/tenant', 'office-building', '菜单', 20, 1),
(3, 0, '套餐与配额', '/platform/package', 'tickets', '菜单', 30, 1),
(4, 0, '账单与计费', '/platform/bill', 'money', '菜单', 40, 1),
(5, 0, '平台公告', '/platform/notice', 'bell', '菜单', 50, 1),
(6, 0, '平台审计', '/platform/audit', 'document', '菜单', 60, 1),
(7, 0, '账号与角色', '/platform/account', 'user', '菜单', 70, 1);

INSERT INTO `platform_role_menu` (`role_code`, `menu_id`) VALUES
('platform_super', 1), ('platform_super', 2), ('platform_super', 3), ('platform_super', 4),
('platform_super', 5), ('platform_super', 6), ('platform_super', 7),
('platform_oper', 1), ('platform_oper', 2), ('platform_oper', 5), ('platform_oper', 6),
('platform_fin', 1), ('platform_fin', 4), ('platform_fin', 2);

-- ---------- 6. 套餐 ----------
INSERT INTO `platform_package` (`id`, `package_name`, `price`, `billing_cycle`, `end_count`, `seat_quota`, `mp_quota`, `storage_quota_gb`, `sms_quota`, `api_quota`, `trial_supported`, `remark`) VALUES
(1, '基础版', 19800.00, '年', 5, 10, 1, 20, 2000, 100000, 1, '单店起步'),
(2, '标准版', 39800.00, '年', 7, 30, 3, 100, 10000, 500000, 1, '连锁两店'),
(3, '旗舰版', 69800.00, '年', 8, 80, 5, 300, 30000, 2000000, 1, '总部 + 多门店');

-- ---------- 7. 演示租户 ----------
INSERT INTO `platform_tenant` (`id`, `tenant_code`, `tenant_name`, `industry`, `contact_name`, `contact_phone`, `package_id`,
  `status`, `trial_end_time`, `expire_time`, `seat_limit`, `mp_limit`, `storage_limit_gb`, `sms_limit`, `api_limit`) VALUES
(1, 'demo', '月子中心·演示门店', '母婴照护', '张店长', '13800000000', 2,
 'NORMAL', NULL, DATE_ADD(NOW(), INTERVAL 365 DAY), 30, 3, 100, 10000, 500000);

INSERT INTO `platform_tenant_admin` (`tenant_id`, `user_id`, `primary_admin`) VALUES (1, 1, 1);
INSERT INTO `platform_tenant_usage` (`tenant_id`, `used_seat`, `used_mp`, `used_storage_gb`, `used_sms`, `used_api`, `stat_date`)
VALUES (1, 1, 1, 0, 0, 0, CURDATE());

-- ---------- 8. 租户九个角色 ----------
INSERT INTO `sys_role` (`tenant_id`, `role_code`, `role_name`, `remark`) VALUES
(1, 'tenant_admin', '租户管理员', '开通默认角色'),
(1, 'store_manager', '店长', '开通默认角色'),
(1, 'sales', '销售顾问', '开通默认角色'),
(1, 'care_supervisor', '护理主管', '开通默认角色'),
(1, 'nurse', '护理师', '开通默认角色'),
(1, 'nutritionist', '营养师', '开通默认角色'),
(1, 'finance', '财务', '开通默认角色'),
(1, 'service', '客服专员', '开通默认角色'),
(1, 'content', '内容运营', '开通默认角色');

-- ---------- 9. 租户默认菜单 ----------
INSERT INTO `sys_menu` (`tenant_id`, `parent_id`, `title`, `path`, `icon`, `menu_type`, `sort`) VALUES
(1, 0, '工作台', '/tenant/workbench', 'workbench', '菜单', 10),
(1, 0, '房态与档期', '/tenant/room', 'room', '菜单', 20),
(1, 0, '套餐与商品', '/tenant/product', 'goods', '菜单', 30),
(1, 0, '线索CRM', '/tenant/crm', 'user-filled', '菜单', 40),
(1, 0, '订单管理', '/tenant/order', 'list', '菜单', 50),
(1, 0, '入馆与出馆', '/tenant/stay', 'house', '菜单', 60),
(1, 0, '护理服务', '/tenant/care', 'first-aid-kit', '菜单', 70),
(1, 0, '产后康复', '/tenant/rehab', 'bicycle', '菜单', 80),
(1, 0, '月子餐', '/tenant/meal', 'food', '菜单', 90),
(1, 0, '排班与人力', '/tenant/staff', 'calendar', '菜单', 100),
(1, 0, '会员营销', '/tenant/member', 'present', '菜单', 110),
(1, 0, '内容CMS', '/tenant/content', 'edit', '菜单', 120),
(1, 0, '报表分析', '/tenant/report', 'data-line', '菜单', 130),
(1, 0, '系统配置', '/tenant/system', 'setting', '菜单', 140);

-- 租户管理员默认拥有全部菜单
INSERT INTO `sys_role_menu` (`tenant_id`, `role_id`, `menu_id`)
SELECT m.tenant_id, r.id, m.id
FROM `sys_menu` m
JOIN `sys_role` r ON r.tenant_id = m.tenant_id AND r.role_code = 'tenant_admin'
WHERE m.tenant_id = 1;

-- ---------- 10. 租户默认系统参数 ----------
INSERT INTO `sys_config` (`tenant_id`, `config_group`, `config_key`, `config_value`, `value_type`, `remark`) VALUES
(1, '订单规则', 'order.deposit.ratio', '30', 'number', '定金比例（%）'),
(1, '订单规则', 'order.tail.payment.days', '3', 'number', '尾款支付提前天数'),
(1, '订单规则', 'order.cancel.penalty.ratio', '20', 'number', '退订违约金比例（%）'),
(1, '订单规则', 'order.schedule.hold.minutes', '30', 'number', '销售预留档期保留时长（分钟）'),
(1, '订单规则', 'order.deposit.amount', '5000', 'number', '押金金额（元）'),
(1, '护理规则', 'care.ratio.max', '1', 'number', '一对一护理配比上限'),
(1, '护理规则', 'care.warning.temperature', '37.5', 'number', '体温预警阈值（℃）'),
(1, '营销规则', 'marketing.point.ratio', '1', 'number', '消费积分比例（元/分）'),
(1, '站点基础', 'site.name', '月子中心·演示门店', 'string', '站点名称'),
(1, '站点基础', 'site.icp', '', 'string', 'ICP 备案号'),
(1, '多语言', 'i18n.default', 'zh-CN', 'string', '默认语言'),
(1, '消息', 'sms.enabled', 'false', 'boolean', '短信渠道是否启用');

-- ---------- 11. 演示租户主管理员 ----------
INSERT INTO `sys_user` (`tenant_id`, `username`, `password`, `real_name`, `role_id`, `status`)
SELECT 1, 'admin', '$2a$10$J83QpEfchT.8zn7yPQ55tO6Yp97XJ.z4bghs8r4cgeWixzJ0Ld1qG', '演示管理员', r.id, 'ENABLED'
FROM `sys_role` r WHERE r.tenant_id = 1 AND r.role_code = 'tenant_admin';

-- ---------- 12. 消息通道（4 通道 + 6 短信渠道，默认全部停用） ----------
INSERT INTO `msg_channel_config` (`tenant_id`, `channel_code`, `channel_name`, `enabled`) VALUES
(1, 'mp_template', '公众号模板消息', 0),
(1, 'mini_subscribe', '小程序订阅消息', 0),
(1, 'app_push', 'App 推送', 0),
(1, 'site_message', '站内信', 1),
(1, 'aliyun_sms', '阿里云短信', 0),
(1, 'tencent_sms', '腾讯云短信', 0),
(1, 'huawei_sms', '华为云短信', 0),
(1, 'jd_sms', '京东云短信', 0),
(1, 'aws_sns', 'Amazon SNS', 0),
(1, 'local_log', '本地日志（调试）', 1);

-- ---------- 13. 系统参数补充：门店信息 / 客服 / 营销规则 ----------
INSERT INTO `sys_config` (`tenant_id`, `config_group`, `config_key`, `config_value`, `value_type`, `remark`) VALUES
(1, '门店信息', 'store.name', '月子中心·西湖店', 'string', '门店名称'),
(1, '门店信息', 'store.phone', '0571-88886666', 'string', '门店电话'),
(1, '门店信息', 'store.address', '杭州市西湖区文三路 100 号', 'string', '门店地址'),
(1, '门店信息', 'store.business.hours', '09:00-18:00', 'string', '营业时间'),
(1, '客服', 'service.phone', '400-800-1234', 'string', '客服热线'),
(1, '客服', 'service.work.time', '09:00-21:00', 'string', '在线客服工作时间'),
(1, '营销规则', 'marketing.referral.points', '500', 'number', '转介绍奖励积分'),
(1, '营销规则', 'marketing.coupon.expire.days', '30', 'number', '优惠券有效天数'),
(1, '护理规则', 'care.record.editable.hours', '24', 'number', '护理记录可修改时限（小时）'),
(1, '订单规则', 'order.number.prefix', 'YZ', 'string', '订单号前缀');

-- ---------- 14. 界面语言包样例（后台与前台共用词条） ----------
INSERT INTO `sys_i18n_message` (`lang_code`, `msg_key`, `msg_value`) VALUES
('zh-CN', 'common.confirm', '确定'),
('zh-CN', 'common.cancel', '取消'),
('zh-CN', 'common.save', '保存'),
('zh-CN', 'common.search', '查询'),
('zh-CN', 'common.reset', '重置'),
('zh-CN', 'common.export', '导出'),
('zh-CN', 'nav.dashboard', '工作台'),
('zh-CN', 'nav.room', '房型房间'),
('zh-CN', 'nav.order', '订单管理'),
('zh-CN', 'nav.care', '护理管理'),
('zh-CN', 'nav.member', '会员营销'),
('zh-CN', 'nav.report', '经营报表'),
('zh-CN', 'site.book.visit', '预约参观'),
('zh-CN', 'site.package', '房型套餐'),
('en-US', 'common.confirm', 'Confirm'),
('en-US', 'common.cancel', 'Cancel'),
('en-US', 'common.save', 'Save'),
('en-US', 'common.search', 'Search'),
('en-US', 'common.reset', 'Reset'),
('en-US', 'common.export', 'Export'),
('en-US', 'nav.dashboard', 'Dashboard'),
('en-US', 'nav.room', 'Rooms'),
('en-US', 'nav.order', 'Orders'),
('en-US', 'nav.care', 'Care'),
('en-US', 'nav.member', 'Members'),
('en-US', 'nav.report', 'Reports'),
('en-US', 'site.book.visit', 'Book a Visit'),
('en-US', 'site.package', 'Packages');
