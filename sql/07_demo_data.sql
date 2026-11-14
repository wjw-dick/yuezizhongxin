-- =============================================================
-- 演示数据（仅用于 demo 租户联调与验收演示）
-- 说明：
--   1. 依赖 06_seed_data.sql 已执行（租户 demo = 1，套餐/字典/菜单已就绪）
--   2. 手机号按 SecretCipher 算法（SHA-256 派生密钥 + AES/GCM）加密后落库
--   3. 日期均以 CURDATE() 为基准，任何时间导入都能看到"今天"的数据
--   4. 可重复执行：先按幂等顺序清理本脚本涉及的表
-- =============================================================
USE yzz_platform;
SET NAMES utf8mb4;
SET @t = 1;

DELETE FROM care_record      WHERE tenant_id = @t;
DELETE FROM care_plan_item   WHERE tenant_id = @t;
DELETE FROM care_plan        WHERE tenant_id = @t;
DELETE FROM care_abnormal    WHERE tenant_id = @t;
DELETE FROM care_handover    WHERE tenant_id = @t;
DELETE FROM meal_order       WHERE tenant_id = @t;
DELETE FROM meal_restrict    WHERE tenant_id = @t;
DELETE FROM meal_template    WHERE tenant_id = @t;
DELETE FROM rehab_record     WHERE tenant_id = @t;
DELETE FROM rehab_appointment WHERE tenant_id = @t;
DELETE FROM rehab_course     WHERE tenant_id = @t;
DELETE FROM rehab_item       WHERE tenant_id = @t;
DELETE FROM stay_checkout    WHERE tenant_id = @t;
DELETE FROM stay_checkin     WHERE tenant_id = @t;
DELETE FROM biz_payment      WHERE tenant_id = @t;
DELETE FROM biz_order_item   WHERE tenant_id = @t;
DELETE FROM biz_order        WHERE tenant_id = @t;
DELETE FROM crm_visit        WHERE tenant_id = @t;
DELETE FROM crm_follow       WHERE tenant_id = @t;
DELETE FROM crm_lead         WHERE tenant_id = @t;
DELETE FROM member_coupon    WHERE tenant_id = @t;
DELETE FROM mkt_coupon       WHERE tenant_id = @t;
DELETE FROM member_point_log WHERE tenant_id = @t;
DELETE FROM member_recharge  WHERE tenant_id = @t;
DELETE FROM member_info      WHERE tenant_id = @t;
DELETE FROM cms_content      WHERE tenant_id = @t;
DELETE FROM staff_attendance WHERE tenant_id = @t;
DELETE FROM staff_schedule   WHERE tenant_id = @t;
DELETE FROM staff_info       WHERE tenant_id = @t;
DELETE FROM room_schedule    WHERE tenant_id = @t;
DELETE FROM room             WHERE tenant_id = @t;
DELETE FROM room_type        WHERE tenant_id = @t;
DELETE FROM pkg_item         WHERE tenant_id = @t;
DELETE FROM pkg_package      WHERE tenant_id = @t;
DELETE FROM prod_product     WHERE tenant_id = @t;

-- -------------------------------------------------------------
-- 一、房型与房间
-- -------------------------------------------------------------
INSERT INTO room_type (id, tenant_id, name, level, area, max_guest, bed_type, facilities, images, description, status, sort) VALUES
(1, @t, '舒适单间', '舒适', 28, 2, '1.5m 双人床',
 '独立卫浴,恒温空调,空气净化器,哺乳灯,智能马桶,婴儿床',
 JSON_ARRAY('/files/tenants/demo/rooms/room-1.png'),
 '朝南落地窗，日照充足；配备哺乳灯与婴儿护理台，适合一位妈妈加一位陪护。', 'ENABLED', 1),
(2, @t, '尊享套房', '尊享', 42, 3, '1.8m 大床',
 '独立卫浴,客厅分区,恒温空调,空气净化器,婴儿护理台,冰箱,微波炉',
 JSON_ARRAY('/files/tenants/demo/rooms/room-2.png'),
 '一室一厅布局，客厅可接待家属；独立婴儿护理台与温奶设备，护理师 24 小时值守。', 'ENABLED', 2),
(3, @t, '家庭双床房', '家庭', 36, 3, '双 1.35m 床',
 '独立卫浴,双床,陪护休憩区,恒温空调,空气净化器,婴儿床',
 JSON_ARRAY('/files/tenants/demo/rooms/room-3.png'),
 '双床设计适合家属全程陪护，另设独立休憩区，夜间护理不打扰妈妈休息。', 'ENABLED', 3),
(4, @t, '豪华套房', '豪华', 56, 4, '2.0m 大床',
 '双卫,观景阳台,客厅分区,婴儿房,恒温空调,空气净化器,冰箱,微波炉,按摩椅',
 JSON_ARRAY('/files/tenants/demo/rooms/room-4.png'),
 '双卫加观景阳台，独立婴儿房；配备按摩椅与产后修复设备，适合二胎家庭。', 'ENABLED', 4);

INSERT INTO room (id, tenant_id, room_no, room_type_id, floor, status, remark) VALUES
(1, @t, '801', 1, 8, 'OCCUPIED',  '在住'),
(2, @t, '802', 1, 8, 'LOCKED',    '已锁定，等待入住'),
(3, @t, '803', 1, 8, 'AVAILABLE', NULL),
(4, @t, '805', 1, 8, 'CLEANING',  '退房清洁中'),
(5, @t, '806', 2, 8, 'OCCUPIED',  '在住'),
(6, @t, '807', 2, 8, 'OCCUPIED',  '今日出馆'),
(7, @t, '808', 2, 8, 'AVAILABLE', NULL),
(8, @t, '809', 3, 9, 'AVAILABLE', NULL),
(9, @t, '810', 3, 9, 'MAINTENANCE', '空调检修'),
(10, @t, '811', 4, 9, 'AVAILABLE', NULL),
(11, @t, '812', 4, 9, 'AVAILABLE', NULL);

-- 档期：在住三间铺满本月，供房态日历展示
INSERT INTO room_schedule (tenant_id, room_id, biz_date, state, order_id, remark)
SELECT @t, 1, DATE_ADD(CURDATE(), INTERVAL n DAY), 'OCCUPIED', 1, '在住' FROM
 (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d1;
INSERT INTO room_schedule (tenant_id, room_id, biz_date, state, order_id, remark)
SELECT @t, 5, DATE_ADD(CURDATE(), INTERVAL n DAY), 'OCCUPIED', 2, '在住' FROM
 (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d2;
INSERT INTO room_schedule (tenant_id, room_id, biz_date, state, order_id, remark)
SELECT @t, 6, DATE_ADD(CURDATE(), INTERVAL n DAY), 'OCCUPIED', 3, '今日出馆' FROM
 (SELECT 0 n) d3;
INSERT INTO room_schedule (tenant_id, room_id, biz_date, state, order_id, remark)
SELECT @t, 2, DATE_ADD(CURDATE(), INTERVAL n DAY), 'LOCKED', 4, '待入住' FROM
 (SELECT 3 n UNION ALL SELECT 4 UNION ALL SELECT 5) d4;

-- -------------------------------------------------------------
-- 二、套餐与增值商品
-- -------------------------------------------------------------
INSERT INTO pkg_package (id, tenant_id, name, days, room_type_level, care_mode, meal_standard, rehab_items, price, deposit_ratio, store_ids, status, sort, detail_html) VALUES
(1, @t, '28 天标准月子套餐', 28, '舒适', '一对一', '三正三辅', '产后修复 10 次,盆底评估 1 次', 39800, 30, '1', 'ON_SHELF', 1,
 '<p>含 28 天住宿、一对一母婴护理、三正三辅月子餐、产后修复 10 次、42 天复查一次。</p>'),
(2, @t, '42 天尊享月子套餐', 42, '尊享', '一对一', '三正三辅+定制', '产后修复 20 次,盆底修复 5 次,中医调理 4 次', 59800, 30, '1', 'ON_SHELF', 2,
 '<p>含 42 天住宿、专属护理师、营养师定制餐单、产后修复 20 次、中医调理 4 次。</p>'),
(3, @t, '14 天轻享月子套餐', 14, '舒适', '一对二', '三正两辅', '产后修复 5 次', 22800, 30, '1', 'ON_SHELF', 3,
 '<p>短周期轻享方案，适合居家为主、阶段性入住调理的家庭。</p>');

INSERT INTO pkg_item (tenant_id, package_id, item_type, item_name, quantity, price) VALUES
(@t, 1, '住宿', '舒适单间 28 天', 28, 39800),
(@t, 1, '护理', '一对一母婴护理', 28, 0),
(@t, 1, '餐饮', '三正三辅月子餐', 28, 0),
(@t, 1, '康复', '产后修复', 10, 0),
(@t, 2, '住宿', '尊享套房 42 天', 42, 59800),
(@t, 2, '护理', '一对一母婴护理 + 专属护理师', 42, 0),
(@t, 2, '餐饮', '三正三辅 + 营养师定制', 42, 0),
(@t, 2, '康复', '产后修复 + 中医调理', 24, 0),
(@t, 3, '住宿', '舒适单间 14 天', 14, 22800),
(@t, 3, '护理', '一对二母婴护理', 14, 0),
(@t, 3, '餐饮', '三正两辅月子餐', 14, 0);

INSERT INTO prod_product (id, tenant_id, name, category, price, stock, images, detail_html, status) VALUES
(1, @t, '满月照拍摄（精修 20 张）', '增值服务', 1280, 50, JSON_ARRAY('/files/tenants/demo/content/case-1.png'), '<p>专业摄影师入馆拍摄，含服装造型与 20 张精修。</p>', 'ENABLED'),
(2, @t, '婴儿游泳 + 抚触 10 次卡', '增值服务', 980, 40, JSON_ARRAY(), '<p>恒温水育，护士全程看护。</p>', 'ENABLED'),
(3, @t, '催乳通乳单次', '护理服务', 380, 100, JSON_ARRAY(), '<p>持证催乳师上门，单次 60 分钟。</p>', 'ENABLED'),
(4, @t, '产后中药足浴包（7 日装）', '零售商品', 268, 200, JSON_ARRAY(), '<p>中医师配方，驱寒祛湿。</p>', 'ENABLED'),
(5, @t, '月子餐外送（每日三正三辅）', '餐饮', 398, 999, JSON_ARRAY(), '<p>居家阶段继续配送，按日计费。</p>', 'ENABLED');

-- -------------------------------------------------------------
-- 三、员工
-- -------------------------------------------------------------
INSERT INTO staff_info (id, tenant_id, name, phone, job_type, status) VALUES
(1, @t, '周敏', 'BwGBLC18/ko4WOsMc7lWhuEnxANndLFXqaP562INMSN6viyaMgOs', '护士长', 'ENABLED'),
(2, @t, '林小雨', 'k/QO0iblQqusV57zkFW+fGvShI+/7i5u0pG+EGXUoCwvErIQ01wF', '护理师', 'ENABLED'),
(3, @t, '何佳', '0xuuVAmh7hzAUcIp0UsIP/YUC960kOuZ35lowyZxhowylqMQnem1', '护理师', 'ENABLED'),
(4, @t, '吴倩', 'SxGHQWRu1IscCJ9G1z9rlGGFsCbuAkBtoWBRzwDc2E4x4njZR6Xw', '护理师', 'ENABLED'),
(5, @t, '郑好', 'C5imSsv9Q/N4Whj1v5K9oFUdwRivH4my/1QVF+Hib8NzejYpE2Qf', '康复师', 'ENABLED'),
(6, @t, '孙悦', 'q5tWmpjN2BXvaVhd88yo3vHry35AYYUD22zPJOXRAiA3ODEBocAi', '营养师', 'ENABLED'),
(7, @t, '李婷', 'lnMTyxW7u6C6ZxExbiqwi7LcnX/4TDfonYo59k7iHDNPk7/hwW16', '客服顾问', 'ENABLED'),
(8, @t, '张倩', 'zakIn597INNA5Nn0YZqNCkyPBka0Dbjt6+FrDEuY3gtui8O/TDhy', '保洁', 'ENABLED');

INSERT INTO staff_schedule (tenant_id, staff_id, schedule_date, shift_type, floor) VALUES
(@t, 1, CURDATE(), '白班', '8F'), (@t, 2, CURDATE(), '白班', '8F'),
(@t, 3, CURDATE(), '夜班', '8F'), (@t, 4, CURDATE(), '白班', '9F'),
(@t, 5, CURDATE(), '白班', '8F'), (@t, 6, CURDATE(), '白班', '9F'),
(@t, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '白班', '8F'),
(@t, 2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '夜班', '8F'),
(@t, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '白班', '8F'),
(@t, 4, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '白班', '9F');

INSERT INTO staff_attendance (tenant_id, staff_id, attendance_date, check_in_time, check_out_time, location) VALUES
(@t, 1, CURDATE(), CONCAT(CURDATE(), ' 07:52:00'), NULL, '门店前台'),
(@t, 2, CURDATE(), CONCAT(CURDATE(), ' 07:58:00'), NULL, '门店前台'),
(@t, 3, CURDATE(), CONCAT(CURDATE(), ' 19:55:00'), NULL, '门店前台'),
(@t, 4, CURDATE(), CONCAT(CURDATE(), ' 08:05:00'), NULL, '门店前台');

-- -------------------------------------------------------------
-- 四、CRM 线索（覆盖五个阶段，含逾期跟进）
-- -------------------------------------------------------------
INSERT INTO crm_lead (id, tenant_id, customer_name, phone, due_date, intend_room_type, budget_min, budget_max, source_code, stage, advisor_id, next_follow_time, lost_reason, remark, created_time) VALUES
(1, @t, '张雅雯', 'BwGBLC18/ko4WOsMc7lWhuEnxANndLFXqaP562INMSN6viyaMgOs', DATE_ADD(CURDATE(), INTERVAL 10 DAY), '舒适单间', 35000, 45000, '官网', '已签约', 7, NULL, NULL, '已签 28 天套餐，10 月 28 日入住', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(2, @t, '李静怡', 'k/QO0iblQqusV57zkFW+fGvShI+/7i5u0pG+EGXUoCwvErIQ01wF', DATE_ADD(CURDATE(), INTERVAL 25 DAY), '尊享套房', 55000, 65000, '小程序', '已签约', 7, NULL, NULL, '42 天尊享套餐', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(3, @t, '王思琪', '0xuuVAmh7hzAUcIp0UsIP/YUC960kOuZ35lowyZxhowylqMQnem1', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '舒适单间', 35000, 42000, '公众号', '已到店', 7, DATE_ADD(NOW(), INTERVAL 2 DAY), NULL, '已交定金锁定 802 房', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4, @t, '刘佳', 'q5tWmpjN2BXvaVhd88yo3vHry35AYYUD22zPJOXRAiA3ODEBocAi', DATE_ADD(CURDATE(), INTERVAL 60 DAY), '家庭双床房', 30000, 40000, 'H5', '已联系', 7, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, '关注陪护床位，已发房型资料', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, @t, '周雨桐', 'SxGHQWRu1IscCJ9G1z9rlGGFsCbuAkBtoWBRzwDc2E4x4njZR6Xw', DATE_ADD(CURDATE(), INTERVAL 45 DAY), '豪华套房', 60000, 80000, '官网', '已联系', 7, DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, '二胎家庭，关注双卫与婴儿房', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, @t, '陈美玲', 'C5imSsv9Q/N4Whj1v5K9oFUdwRivH4my/1QVF+Hib8NzejYpE2Qf', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '舒适单间', 30000, 38000, '转介绍', '已联系', 7, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, '朋友推荐，预算偏紧', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(7, @t, '赵晓彤', 'lnMTyxW7u6C6ZxExbiqwi7LcnX/4TDfonYo59k7iHDNPk7/hwW16', DATE_SUB(CURDATE(), INTERVAL 20 DAY), '舒适单间', 20000, 30000, '官网', '已流失', 7, NULL, '比较后选择了离家更近的机构', '已 3 次跟进未回访', DATE_SUB(NOW(), INTERVAL 55 DAY)),
(8, @t, '黄婷', 'zakIn597INNA5Nn0YZqNCkyPBka0Dbjt6+FrDEuY3gtui8O/TDhy', DATE_ADD(CURDATE(), INTERVAL 70 DAY), '尊享套房', 50000, 70000, '小程序', '新线索', NULL, DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, '夜间咨询，尚未分配顾问', DATE_SUB(NOW(), INTERVAL 6 HOUR));

INSERT INTO crm_follow (tenant_id, lead_id, follow_time, content, result, next_time, operator_id) VALUES
(@t, 1, DATE_SUB(NOW(), INTERVAL 38 DAY), '电话沟通，介绍 28 天套餐与房型', '有意向', DATE_SUB(NOW(), INTERVAL 30 DAY), 7),
(@t, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), '到店参观 806 房，家属一同前来', '已到店', DATE_SUB(NOW(), INTERVAL 20 DAY), 7),
(@t, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), '确认套餐并签署合同，收定金 12810 元', '已签约', NULL, 7),
(@t, 3, DATE_SUB(NOW(), INTERVAL 10 DAY), '公众号留资，发送房型与价格表', '有意向', DATE_SUB(NOW(), INTERVAL 5 DAY), 7),
(@t, 3, DATE_SUB(NOW(), INTERVAL 4 DAY), '到店参观，锁定 802 房，收定金 11940 元', '已到店', DATE_ADD(NOW(), INTERVAL 2 DAY), 7),
(@t, 4, DATE_SUB(NOW(), INTERVAL 4 DAY), '小程序留资，关注家庭双床房', '待跟进', DATE_SUB(NOW(), INTERVAL 1 DAY), 7),
(@t, 5, DATE_SUB(NOW(), INTERVAL 7 DAY), '官网留资，二胎家庭，需要双卫', '待跟进', DATE_SUB(NOW(), INTERVAL 3 DAY), 7),
(@t, 7, DATE_SUB(NOW(), INTERVAL 24 DAY), '第三次回访未接通', '无进展', NULL, 7);

INSERT INTO crm_visit (tenant_id, lead_id, visit_date, time_slot, person_count, advisor_id, checked_in, feedback) VALUES
(@t, 1, DATE_SUB(CURDATE(), INTERVAL 30 DAY), '10:00-11:00', 3, 7, 1, '家属满意采光与护理台'),
(@t, 3, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '14:00-15:00', 2, 7, 1, '现场签约，锁定 802');

-- -------------------------------------------------------------
-- 五、订单（覆盖待支付/待入住/在住/待结算/已完成/已退订）
-- -------------------------------------------------------------
INSERT INTO biz_order (id, tenant_id, order_no, lead_id, customer_name, phone, package_id, room_type_id, room_id, checkin_date, checkout_date,
 care_mode, companion_count, package_amount, extra_amount, product_amount, discount_amount, total_amount,
 deposit_amount, tail_amount, bond_amount, paid_amount, status, source, sign_type, remark, created_by, created_time) VALUES
(1, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 18 DAY), '%Y%m%d'), '0001'), 1, '张雅雯',
 'BwGBLC18/ko4WOsMc7lWhuEnxANndLFXqaP562INMSN6viyaMgOs', 1, 1, 1,
 DATE_SUB(CURDATE(), INTERVAL 18 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY),
 '一对一', 1, 39800, 2800, 600, 500, 42700, 12810, 29890, 2000, 12810, '在住', '官网', '电子签约', '产后修复加购 5 次', 7, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '%Y%m%d'), '0002'), 2, '李静怡',
 'k/QO0iblQqusV57zkFW+fGvShI+/7i5u0pG+EGXUoCwvErIQ01wF', 2, 2, 5,
 DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 36 DAY),
 '一对一', 2, 59800, 3600, 1200, 1000, 63600, 17940, 45660, 3000, 63600, '在住', '小程序', '电子签约', '全款已结清', 7, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(3, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y%m%d'), '0003'), 3, '王思琪',
 '0xuuVAmh7hzAUcIp0UsIP/YUC960kOuZ35lowyZxhowylqMQnem1', 1, 1, 2,
 DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 31 DAY),
 '一对一', 1, 39800, 0, 0, 0, 39800, 11940, 27860, 2000, 11940, '待入住', '公众号', '电子签约', '已锁定 802 房', 7, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 28 DAY), '%Y%m%d'), '0004'), 6, '陈美玲',
 'C5imSsv9Q/N4Whj1v5K9oFUdwRivH4my/1QVF+Hib8NzejYpE2Qf', 1, 2, 6,
 DATE_SUB(CURDATE(), INTERVAL 28 DAY), CURDATE(),
 '一对一', 1, 39800, 2800, 300, 0, 42900, 11940, 30960, 2000, 39800, '待结算', '转介绍', '电子签约', '今日出馆，尾款待结', 7, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(5, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 70 DAY), '%Y%m%d'), '0005'), 7, '赵晓彤',
 'lnMTyxW7u6C6ZxExbiqwi7LcnX/4TDfonYo59k7iHDNPk7/hwW16', 3, 1, 3,
 DATE_SUB(CURDATE(), INTERVAL 56 DAY), DATE_SUB(CURDATE(), INTERVAL 42 DAY),
 '一对二', 1, 22800, 0, 0, 0, 22800, 6840, 15960, 2000, 22800, '已完成', '官网', '纸质签约', '14 天轻享，已出馆', 7, DATE_SUB(NOW(), INTERVAL 70 DAY)),
(6, @t, CONCAT('YZZ', DATE_FORMAT(CURDATE(), '%Y%m%d'), '0006'), 4, '刘佳',
 'q5tWmpjN2BXvaVhd88yo3vHry35AYYUD22zPJOXRAiA3ODEBocAi', 1, 3, NULL,
 DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 58 DAY),
 '一对一', 2, 39800, 0, 0, 800, 39000, 11700, 27300, 2000, 0, '待支付', 'H5', '电子签约', '小程序下单未支付', 7, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(7, @t, CONCAT('YZZ', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '%Y%m%d'), '0007'), 5, '周雨桐',
 'SxGHQWRu1IscCJ9G1z9rlGGFsCbuAkBtoWBRzwDc2E4x4njZR6Xw', 2, 4, NULL,
 DATE_ADD(CURDATE(), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 87 DAY),
 '一对一', 2, 59800, 0, 0, 0, 59800, 17940, 41860, 3000, 0, '已退订', '官网', '电子签约', '家庭原因取消，定金已退', 7, DATE_SUB(NOW(), INTERVAL 15 DAY));

INSERT INTO biz_order_item (tenant_id, order_id, item_type, item_name, quantity, price, amount) VALUES
(@t, 1, '套餐', '28 天标准月子套餐', 1, 39800, 39800),
(@t, 1, '增值', '产后修复加购 5 次', 5, 560, 2800),
(@t, 1, '商品', '产后中药足浴包（7 日装）', 2, 300, 600),
(@t, 2, '套餐', '42 天尊享月子套餐', 1, 59800, 59800),
(@t, 2, '增值', '中医调理 4 次', 4, 900, 3600),
(@t, 2, '商品', '满月照拍摄（精修 20 张）', 1, 1200, 1200),
(@t, 3, '套餐', '28 天标准月子套餐', 1, 39800, 39800),
(@t, 4, '套餐', '28 天标准月子套餐', 1, 39800, 39800),
(@t, 4, '增值', '催乳通乳', 4, 700, 2800),
(@t, 4, '商品', '婴儿游泳 + 抚触 10 次卡', 1, 300, 300),
(@t, 5, '套餐', '14 天轻享月子套餐', 1, 22800, 22800),
(@t, 6, '套餐', '28 天标准月子套餐', 1, 39800, 39800),
(@t, 7, '套餐', '42 天尊享月子套餐', 1, 59800, 59800);

INSERT INTO biz_payment (tenant_id, order_id, payment_no, pay_type, method, amount, status, trade_no, paid_time, operator_id, remark) VALUES
(@t, 1, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 20 DAY), '%Y%m%d'), '0001'), '定金', '微信', 12810, '已支付', 'WX202611140001', DATE_SUB(NOW(), INTERVAL 20 DAY), 7, '线上支付'),
(@t, 2, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '%Y%m%d'), '0002'), '全款', '转账', 63600, '已支付', 'BANK202611140002', DATE_SUB(NOW(), INTERVAL 10 DAY), 7, '对公转账'),
(@t, 3, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '%Y%m%d'), '0003'), '定金', '微信', 11940, '已支付', 'WX202611140003', DATE_SUB(NOW(), INTERVAL 2 HOUR), 7, '线上支付'),
(@t, 4, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 30 DAY), '%Y%m%d'), '0004'), '定金', '现金', 11940, '已支付', NULL, DATE_SUB(NOW(), INTERVAL 30 DAY), 7, '前台收现'),
(@t, 4, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 27 DAY), '%Y%m%d'), '0005'), '尾款', '微信', 27860, '已支付', 'WX202611140005', DATE_SUB(NOW(), INTERVAL 27 DAY), 7, '入住次日结清'),
(@t, 5, CONCAT('PAY', DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 70 DAY), '%Y%m%d'), '0006'), '全款', '微信', 22800, '已支付', 'WX202611140006', DATE_SUB(NOW(), INTERVAL 70 DAY), 7, '线上支付');

-- -------------------------------------------------------------
-- 六、入馆与出馆
-- -------------------------------------------------------------
INSERT INTO stay_checkin (id, tenant_id, order_id, room_id, mother_name, mother_age, gestational_week, delivery_type,
 delivery_date, baby_birth_date, baby_weight, feeding_type, family_info, emergency_contact, id_card_no,
 health_notice_signed, deposit_amount, checkin_time, status) VALUES
(1, @t, 1, 1, '张雅雯', 29, 39, '顺产', DATE_SUB(CURDATE(), INTERVAL 21 DAY), DATE_SUB(CURDATE(), INTERVAL 21 DAY),
 3.35, '混合喂养', '丈夫陪护，家中老人偶尔探望', '张先生 13800001001', 'BwGBLC18/ko4WOsMc7lWhuEnxANndLFXqaP562INMSN6viyaMgOs',
 1, 2000, DATE_SUB(NOW(), INTERVAL 18 DAY), '在住'),
(2, @t, 2, 5, '李静怡', 32, 38, '剖宫产', DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),
 3.10, '母乳喂养', '丈夫与月嫂轮流陪护', '李先生 13800001002', 'k/QO0iblQqusV57zkFW+fGvShI+/7i5u0pG+EGXUoCwvErIQ01wF',
 1, 3000, DATE_SUB(NOW(), INTERVAL 6 DAY), '在住'),
(3, @t, 4, 6, '陈美玲', 27, 40, '顺产', DATE_SUB(CURDATE(), INTERVAL 31 DAY), DATE_SUB(CURDATE(), INTERVAL 31 DAY),
 3.62, '母乳喂养', '母亲全程陪护', '陈女士 13800001004', 'SxGHQWRu1IscCJ9G1z9rlGGFsCbuAkBtoWBRzwDc2E4x4njZR6Xw',
 1, 2000, DATE_SUB(NOW(), INTERVAL 28 DAY), '在住');

INSERT INTO stay_checkout (tenant_id, order_id, checkin_id, tail_amount, extra_amount, deposit_refund, item_returned,
 summary, satisfaction, review_appointment, checkout_time, operator_id) VALUES
(@t, 5, 0, 15960, 0, 2000, 1, '14 天入住顺利，妈妈恢复良好，宝宝体重增长 0.6kg，已预约 42 天复查。', 5, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 42 DAY), 7);

-- -------------------------------------------------------------
-- 七、护理计划与执行记录
-- -------------------------------------------------------------
INSERT INTO care_plan (id, tenant_id, order_id, customer_name, room_id, care_mode, template_code, status) VALUES
(1, @t, 1, '张雅雯', 1, '一对一', 'STANDARD_28', '进行中'),
(2, @t, 2, '李静怡', 5, '一对一', 'PREMIUM_42', '进行中'),
(3, @t, 4, '陈美玲', 6, '一对一', 'STANDARD_28', '进行中');

-- 今日计划项：妈妈 4 项 + 宝宝 3 项（部分已完成）
INSERT INTO care_plan_item (tenant_id, plan_id, item_name, target_type, plan_date, time_slot, nurse_id, status, finished_time) VALUES
(@t, 1, '晨间体征测量（体温/血压）', '妈妈', CURDATE(), '07:30', 2, '已完成', CONCAT(CURDATE(), ' 07:42:00')),
(@t, 1, '恶露观察与记录', '妈妈', CURDATE(), '09:00', 2, '已完成', CONCAT(CURDATE(), ' 09:10:00')),
(@t, 1, '乳房护理与哺乳指导', '妈妈', CURDATE(), '10:30', 2, '已完成', CONCAT(CURDATE(), ' 10:45:00')),
(@t, 1, '产后康复操', '妈妈', CURDATE(), '15:00', 5, '待执行', NULL),
(@t, 1, '新生儿黄疸测量', '宝宝', CURDATE(), '08:00', 2, '已完成', CONCAT(CURDATE(), ' 08:12:00')),
(@t, 1, '脐部护理', '宝宝', CURDATE(), '10:00', 2, '已完成', CONCAT(CURDATE(), ' 10:08:00')),
(@t, 1, '沐浴与抚触', '宝宝', CURDATE(), '16:00', 3, '待执行', NULL),
(@t, 2, '剖宫产伤口换药观察', '妈妈', CURDATE(), '09:30', 3, '已完成', CONCAT(CURDATE(), ' 09:38:00')),
(@t, 2, '晨间体征测量（体温/血压）', '妈妈', CURDATE(), '07:30', 3, '已完成', CONCAT(CURDATE(), ' 07:40:00')),
(@t, 2, '下肢气压治疗（防血栓）', '妈妈', CURDATE(), '14:00', 5, '待执行', NULL),
(@t, 2, '母乳喂养指导', '妈妈', CURDATE(), '11:00', 3, '已完成', CONCAT(CURDATE(), ' 11:20:00')),
(@t, 2, '新生儿体重测量', '宝宝', CURDATE(), '08:30', 3, '已完成', CONCAT(CURDATE(), ' 08:36:00')),
(@t, 2, '黄疸测量', '宝宝', CURDATE(), '08:40', 3, '已完成', CONCAT(CURDATE(), ' 08:48:00')),
(@t, 2, '抚触按摩', '宝宝', CURDATE(), '17:00', 3, '待执行', NULL),
(@t, 3, '出馆前体征复核', '妈妈', CURDATE(), '08:00', 4, '已完成', CONCAT(CURDATE(), ' 08:15:00')),
(@t, 3, '出馆宣教与复查预约', '妈妈', CURDATE(), '10:00', 4, '待执行', NULL),
(@t, 3, '宝宝满月体重与身长测量', '宝宝', CURDATE(), '09:00', 4, '已完成', CONCAT(CURDATE(), ' 09:12:00'));

-- 昨日已完成项，用于趋势图与记录列表
INSERT INTO care_plan_item (tenant_id, plan_id, item_name, target_type, plan_date, time_slot, nurse_id, status, finished_time) VALUES
(@t, 1, '晨间体征测量（体温/血压）', '妈妈', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '07:30', 2, '已完成', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 07:38:00')),
(@t, 1, '新生儿黄疸测量', '宝宝', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:00', 2, '已完成', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 08:09:00')),
(@t, 2, '晨间体征测量（体温/血压）', '妈妈', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '07:30', 3, '已完成', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 07:41:00')),
(@t, 2, '黄疸测量', '宝宝', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:40', 3, '已完成', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 08:52:00'));

INSERT INTO care_record (tenant_id, plan_item_id, order_id, target_type, item_name, temperature, blood_pressure, pulse,
 weight, feeding_amount, jaundice_value, stool_count, content, nurse_id, submit_time, status) VALUES
(@t, 1, 1, '妈妈', '晨间体征测量（体温/血压）', 36.6, '112/72', 78, 62.4, NULL, NULL, NULL, '体温正常，血压平稳，恶露量中、色淡红。', 2, CONCAT(CURDATE(), ' 07:42:00'), '正常'),
(@t, 2, 1, '妈妈', '恶露观察与记录', 36.7, NULL, NULL, NULL, NULL, NULL, NULL, '恶露量中，无异味，会阴伤口愈合良好。', 2, CONCAT(CURDATE(), ' 09:10:00'), '正常'),
(@t, 3, 1, '妈妈', '乳房护理与哺乳指导', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '双侧乳汁通畅，指导卧位哺乳姿势，宝宝含接良好。', 2, CONCAT(CURDATE(), ' 10:45:00'), '正常'),
(@t, 5, 1, '宝宝', '新生儿黄疸测量', 36.5, NULL, 140, 3.62, 60, 8.2, 4, '经皮黄疸 8.2mg/dL，处于生理性范围，继续监测。', 2, CONCAT(CURDATE(), ' 08:12:00'), '正常'),
(@t, 6, 1, '宝宝', '脐部护理', 36.5, NULL, NULL, NULL, NULL, NULL, NULL, '脐部干燥，无渗液，75% 酒精消毒后暴露。', 2, CONCAT(CURDATE(), ' 10:08:00'), '正常'),
(@t, 8, 2, '妈妈', '剖宫产伤口换药观察', 36.8, '118/76', 82, 68.2, NULL, NULL, NULL, '切口对合良好，无红肿渗液，已换药。', 3, CONCAT(CURDATE(), ' 09:38:00'), '正常'),
(@t, 9, 2, '妈妈', '晨间体征测量（体温/血压）', 36.8, '118/76', 82, 68.2, NULL, NULL, NULL, '血压略高于昨日，已嘱多休息并复测。', 3, CONCAT(CURDATE(), ' 07:40:00'), '正常'),
(@t, 11, 2, '妈妈', '母乳喂养指导', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '指导侧卧喂哺，减少腹部切口牵拉。', 3, CONCAT(CURDATE(), ' 11:20:00'), '正常'),
(@t, 12, 2, '宝宝', '新生儿体重测量', NULL, NULL, NULL, 3.28, NULL, NULL, NULL, '体重较出生下降 4.6%，在生理性下降范围。', 3, CONCAT(CURDATE(), ' 08:36:00'), '正常'),
(@t, 13, 2, '宝宝', '黄疸测量', 36.6, NULL, 142, NULL, NULL, 9.1, 5, '黄疸 9.1mg/dL，建议增加喂养频次。', 3, CONCAT(CURDATE(), ' 08:48:00'), '正常'),
(@t, 15, 4, '妈妈', '出馆前体征复核', 36.5, '110/70', 76, 61.0, NULL, NULL, NULL, '体征平稳，子宫复旧良好，可按期出馆。', 4, CONCAT(CURDATE(), ' 08:15:00'), '正常'),
(@t, 17, 4, '宝宝', '宝宝满月体重与身长测量', NULL, NULL, NULL, 4.25, NULL, NULL, NULL, '体重 4.25kg，身长 54cm，增长达标。', 4, CONCAT(CURDATE(), ' 09:12:00'), '正常');

INSERT INTO care_handover (tenant_id, shift_date, shift_type, from_nurse_id, to_nurse_id, watch_points, unfinished, abnormal_notes, from_sign, to_sign, confirmed_time, status) VALUES
(@t, CURDATE(), '白班→夜班', 2, 3, '802 房李静怡剖宫产切口需继续观察；801 房宝宝黄疸 8.2 需晚班复测一次。',
 '801 房产后康复操（15:00）待执行；806 房沐浴抚触（16:00）待执行。', '无', '林小雨', '何佳', CONCAT(CURDATE(), ' 19:50:00'), '已交接'),
(@t, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '夜班→白班', 3, 2, '夜间 801 房宝宝哭闹 2 次，已安抚；806 房妈妈主诉切口轻微疼痛。',
 '无', '806 房疼痛评分 3 分，已记录', '何佳', '林小雨', CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 07:55:00'), '已交接');

INSERT INTO care_abnormal (tenant_id, order_id, abnormal_type, urgency, description, reporter_id, supervisor_id,
 handle_result, family_notified, status, created_time, closed_time) VALUES
(@t, 2, '伤口异常', '中', '剖宫产切口末端有少量渗液，无红肿。', 3, 1, '已换药并嘱观察，护士长复核后无感染征象。', 1, '已闭环',
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- -------------------------------------------------------------
-- 八、月子餐
-- -------------------------------------------------------------
INSERT INTO meal_template (tenant_id, stage, day_index, meal_type, dishes, avoid_tags) VALUES
(@t, '第一周（排恶露）', 1, '早餐', '小米红枣粥、蒸蛋羹、清炒时蔬', '生冷,辛辣'),
(@t, '第一周（排恶露）', 1, '午餐', '生化汤、软米饭、清蒸鲈鱼、炒青菜', '油腻'),
(@t, '第一周（排恶露）', 1, '晚餐', '红豆汤、瘦肉粥、蒸南瓜', '生冷'),
(@t, '第二周（调气血）', 8, '早餐', '山药排骨粥、水煮蛋、凉拌木耳', '辛辣'),
(@t, '第二周（调气血）', 8, '午餐', '当归鸡汤、杂粮饭、清蒸虾、上汤娃娃菜', '海鲜'),
(@t, '第二周（调气血）', 8, '晚餐', '鲫鱼豆腐汤、软米饭、炒菠菜', NULL),
(@t, '第三周（催乳）', 15, '早餐', '花生猪蹄汤、全麦面包、牛奶', '花生'),
(@t, '第三周（催乳）', 15, '午餐', '通草鲫鱼汤、米饭、清炒西兰花、蒸鸡胸', NULL),
(@t, '第四周（固本）', 22, '早餐', '桂圆红枣粥、鸡蛋、小菜', NULL),
(@t, '第四周（固本）', 22, '午餐', '黄芪乌鸡汤、杂粮饭、清蒸鳕鱼', '海鲜');

INSERT INTO meal_order (tenant_id, order_id, room_id, meal_date, meal_type, dishes, special_tag, extra_charge, checked, abnormal) VALUES
(@t, 1, 1, CURDATE(), '早餐', '小米红枣粥、蒸蛋羹、清炒时蔬', NULL, 0, 1, NULL),
(@t, 1, 1, CURDATE(), '早加餐', '核桃芝麻糊', NULL, 0, 1, NULL),
(@t, 1, 1, CURDATE(), '午餐', '生化汤、软米饭、清蒸鲈鱼、炒青菜', '少盐', 0, 1, NULL),
(@t, 1, 1, CURDATE(), '午加餐', '木瓜牛奶', NULL, 0, 0, NULL),
(@t, 1, 1, CURDATE(), '晚餐', '红豆汤、瘦肉粥、蒸南瓜', NULL, 0, 0, NULL),
(@t, 2, 5, CURDATE(), '早餐', '山药排骨粥、水煮蛋、凉拌木耳', '忌花生', 0, 1, NULL),
(@t, 2, 5, CURDATE(), '早加餐', '红枣银耳羹', NULL, 0, 1, NULL),
(@t, 2, 5, CURDATE(), '午餐', '当归鸡汤、杂粮饭、清蒸虾、上汤娃娃菜', '忌花生,少油', 0, 1, '家属反馈汤偏咸，已更换'),
(@t, 2, 5, CURDATE(), '晚餐', '鲫鱼豆腐汤、软米饭、炒菠菜', '忌花生', 0, 0, NULL),
(@t, 4, 6, CURDATE(), '早餐', '桂圆红枣粥、鸡蛋、小菜', '素食', 0, 1, NULL),
(@t, 4, 6, CURDATE(), '午餐', '黄芪乌鸡汤、杂粮饭、清蒸鳕鱼', '素食', 0, 1, NULL),
(@t, 4, 6, CURDATE(), '午加餐', '红豆沙', NULL, 28, 0, NULL);

INSERT INTO meal_restrict (tenant_id, order_id, restrict_type, content) VALUES
(@t, 2, '忌口', '花生及花生制品'),
(@t, 4, '素食', '不吃荤腥，蛋奶可食'),
(@t, 1, '过敏', '芒果过敏');

-- -------------------------------------------------------------
-- 九、康复
-- -------------------------------------------------------------
INSERT INTO rehab_item (id, tenant_id, name, stage, duration_min, course_times, price, qualification, status) VALUES
(1, @t, '盆底肌修复', '产褥期', 45, 10, 3800, '康复治疗师资格证', 'ENABLED'),
(2, @t, '腹直肌分离修复', '恢复期', 40, 10, 3200, '康复治疗师资格证', 'ENABLED'),
(3, @t, '产后肩颈舒缓', '产后初期', 30, 5, 1200, '中医推拿师', 'ENABLED'),
(4, @t, '中医艾灸调理', '产褥期', 40, 6, 1800, '中医执业医师', 'ENABLED'),
(5, @t, '满月发汗', '恢复期', 60, 1, 680, '中医推拿师', 'ENABLED');

INSERT INTO rehab_course (tenant_id, order_id, item_id, total_times, used_times, expire_date, source) VALUES
(@t, 1, 1, 10, 4, DATE_ADD(CURDATE(), INTERVAL 60 DAY), '套餐赠送'),
(@t, 1, 2, 10, 2, DATE_ADD(CURDATE(), INTERVAL 60 DAY), '套餐赠送'),
(@t, 2, 1, 10, 3, DATE_ADD(CURDATE(), INTERVAL 90 DAY), '套餐赠送'),
(@t, 2, 4, 4, 1, DATE_ADD(CURDATE(), INTERVAL 90 DAY), '加购'),
(@t, 4, 5, 1, 0, DATE_ADD(CURDATE(), INTERVAL 30 DAY), '套餐赠送');

INSERT INTO rehab_appointment (tenant_id, order_id, course_id, room_id, therapist_id, appoint_date, start_time, end_time, status) VALUES
(@t, 1, 1, 1, 5, CURDATE(), '15:00:00', '15:45:00', '待执行'),
(@t, 2, 3, 5, 5, CURDATE(), '14:00:00', '14:40:00', '待执行'),
(@t, 1, 2, 1, 5, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:00:00', '10:40:00', '待执行'),
(@t, 2, 4, 5, 5, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '15:00:00', '15:40:00', '已完成');

INSERT INTO rehab_record (tenant_id, appointment_id, item_name, duration_min, therapist_id, feedback, next_appoint) VALUES
(@t, 4, '中医艾灸调理', 40, 5, '灸后微微出汗，睡眠改善，嘱多饮温水。', DATE_ADD(CURDATE(), INTERVAL 3 DAY)),
(@t, 1, '盆底肌修复', 45, 5, '肌力由 2 级提升至 3 级，继续电刺激配合凯格尔训练。', DATE_ADD(CURDATE(), INTERVAL 2 DAY));

-- -------------------------------------------------------------
-- 十、会员与营销
-- -------------------------------------------------------------
INSERT INTO member_info (id, tenant_id, phone, nickname, avatar, source, points, balance) VALUES
(1, @t, 'zakIn597INNA5Nn0YZqNCkyPBka0Dbjt6+FrDEuY3gtui8O/TDhy', '张雅雯', NULL, '官网', 1280, 2000),
(2, @t, 'QsefHb/hK/F2HNBdD97WWOsa7uIYx3MflZGdkr/sAUB9bxMwbrRy', '李静怡', NULL, '小程序', 2360, 5000),
(3, @t, 'pA0aZ5CKeZPBwoVnQHKPtkddNQpkFInWc20fNMvMM21l8dpLCjwA', '王思琪', NULL, '公众号', 320, 0),
(4, @t, '5n0c3if3xyO7eQ9zQGNCFlCc5O8AsP/ZXZyWqyK3WQ7rpdZ45feh', '陈美玲', NULL, '转介绍', 860, 1000),
(5, @t, 'BarOasrW+fRHzUmYBg+vjrrKQWsW6i1+c8XzfV4zZIBlcOyftb4Q', '周雨桐', NULL, '官网', 150, 0),
(6, @t, 'EpEcxukD+HZlBHD/nfmFKzyzrLRgNoRmVvP9xy35KM4wOrucKe0h', '刘佳', NULL, 'H5', 60, 0);

INSERT INTO member_point_log (tenant_id, member_id, change_value, reason) VALUES
(@t, 1, 1000, '签约赠送'), (@t, 1, 280, '月子餐评价'),
(@t, 2, 2000, '签约赠送'), (@t, 2, 360, '康复打卡'),
(@t, 3, 320, '预约参观'), (@t, 4, 860, '转介绍奖励'),
(@t, 5, 150, '注册赠送'), (@t, 6, 60, '注册赠送');

INSERT INTO member_recharge (tenant_id, member_id, recharge_no, amount, gift_amount, status, rule_shown) VALUES
(@t, 1, 'CZ20261114001', 2000, 0, '成功', 1),
(@t, 2, 'CZ20261114002', 5000, 300, '成功', 1),
(@t, 4, 'CZ20261114003', 1000, 0, '成功', 1);

INSERT INTO mkt_coupon (tenant_id, name, coupon_type, amount, threshold, total_count, issued_count, start_time, end_time, status) VALUES
(@t, '新客立减 1000 元', '满减', 1000, 30000, 200, 68, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 'ENABLED'),
(@t, '产后修复 8 折券', '折扣', 20, 0, 100, 23, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 50 DAY), 'ENABLED'),
(@t, '转介绍答谢 500 元', '满减', 500, 20000, 300, 41, DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_ADD(NOW(), INTERVAL 120 DAY), 'ENABLED');

INSERT INTO member_coupon (tenant_id, member_id, coupon_id, status, used_time, order_id) VALUES
(@t, 1, 1, '已使用', DATE_SUB(NOW(), INTERVAL 20 DAY), 1),
(@t, 2, 2, '未使用', NULL, NULL),
(@t, 3, 1, '未使用', NULL, NULL),
(@t, 6, 1, '未使用', NULL, NULL);

-- -------------------------------------------------------------
-- 十一、内容 CMS
-- -------------------------------------------------------------
INSERT INTO cms_content (tenant_id, content_type, title, cover, body_html, apply_ends, seo_title, seo_keywords,
 seo_description, authorized, status, published_time) VALUES
(@t, 'Banner', '专业照护，安心月子', '/files/tenants/demo/content/banner-1.png',
 '<h2>一对一母婴护理</h2><p>持证护理师 24 小时值守，每日体征监测、伤口护理、母乳喂养指导。</p>', '官网,小程序,H5', '专业照护，安心月子', '月子中心,母婴护理', '月子中心专业照护服务', 0, '已发布', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(@t, '品牌故事', '十二年，只做一件事', '/files/tenants/demo/content/banner-1.png',
 '<p>从一间 8 间房的会所开始，我们把护理流程拆成 168 个标准动作。</p><p>每一位护理师都要通过 3 个月带教与考核才能独立值守。</p>', '官网', '十二年只做一件事', '品牌故事,月子中心', '十二年专注母婴护理', 0, '已发布', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(@t, '房型展示', '舒适单间：朝南落地窗，日照充足', '/files/tenants/demo/rooms/room-1.png',
 '<p>28㎡，1.5m 双人床，独立卫浴，配备哺乳灯与婴儿护理台。</p>', '官网,小程序,H5', '舒适单间', '月子房,单间', '28㎡ 舒适单间', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '房型展示', '尊享套房：一室一厅，家属可陪住', '/files/tenants/demo/rooms/room-2.png',
 '<p>42㎡，客厅分区，可接待家属；独立婴儿护理台与温奶设备。</p>', '官网,小程序', '尊享套房', '月子套房', '42㎡ 尊享套房', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '康复项目', '盆底肌修复：从评估到居家训练', '/files/tenants/demo/rooms/room-4.png',
 '<p>先做盆底肌力评估，再制定电刺激 + 生物反馈 + 凯格尔训练方案。</p>', '官网,小程序', '盆底肌修复', '产后修复,盆底肌', '产后盆底肌修复方案', 0, '已发布', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(@t, '月子餐', '四阶段调理：排、调、催、固', '/files/tenants/demo/content/banner-1.png',
 '<p>第一周排恶露、第二周调气血、第三周催乳、第四周固本，营养师按体质调整。</p>', '官网,小程序,H5', '四阶段月子餐', '月子餐,营养', '四阶段月子餐调理', 0, '已发布', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(@t, '团队', '护理团队：护士长带队，全员持证', '/files/tenants/demo/content/banner-1.png',
 '<p>护士长周敏，12 年三甲医院产科经验；护理师均持母婴护理证。</p>', '官网', '护理团队', '护理团队,护士长', '专业护理团队', 0, '已发布', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@t, '案例', '42 天，一位剖宫产妈妈的恢复记录', '/files/tenants/demo/content/case-1.png',
 '<p>入院时切口疼痛评分 4 分，第 7 天降至 1 分；42 天复查子宫复旧良好。</p>', '官网,小程序', '42 天恢复记录', '客户案例,剖宫产', '剖宫产妈妈 42 天恢复案例', 1, '已发布', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(@t, '资讯', '产后第一周，这 5 件事比进补更重要', NULL,
 '<p>早开奶、勤排恶露、适度下床、情绪观察、伤口清洁。</p>', '官网,公众号', '产后第一周注意事项', '产后护理,新生儿', '产后第一周护理要点', 0, '已发布', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(@t, '资讯', '如何判断宝宝吃饱了？', NULL,
 '<p>看尿量、看体重、看吃奶后的满足感，比看时间更可靠。</p>', '官网,公众号,小程序', '判断宝宝吃饱', '母乳喂养,新生儿', '判断宝宝是否吃饱', 0, '草稿', NULL);

-- -------------------------------------------------------------
-- 十二、公众号台账
-- -------------------------------------------------------------
DELETE FROM mp_account WHERE tenant_id = @t;
INSERT INTO mp_account (tenant_id, account_name, wechat_id, app_id, app_secret, server_url, token, aes_key,
 oauth_domain, js_safe_domain, account_type, verified, qrcode, remark) VALUES
(@t, '月子中心·演示门店', 'yzz-demo', 'wx_demo_appid_0001', NULL,
 'https://demo.example.com/api/mp/callback', NULL, NULL, NULL, NULL, '服务号', 1, NULL, '演示台账，凭证未配置，统计走本地降级');

-- -------------------------------------------------------------
-- 十三、后加功能表的基础数据（room_task / staff_shift / mkt_activity / member_address）
-- -------------------------------------------------------------
DELETE FROM room_task   WHERE tenant_id = @t;
DELETE FROM staff_shift WHERE tenant_id = @t;
DELETE FROM mkt_activity WHERE tenant_id = @t;
DELETE FROM member_address WHERE tenant_id = @t;

INSERT INTO staff_shift (tenant_id, shift_name, start_time, end_time, cross_day) VALUES
(@t, '白班', '08:00', '20:00', 0),
(@t, '夜班', '20:00', '08:00', 1),
(@t, '中班', '12:00', '22:00', 0);

INSERT INTO room_task (tenant_id, room_id, task_type, status, assignee_id, due_time, remark, finished_time) VALUES
(@t, 4, '清洁', '待处理', 8, DATE_ADD(NOW(), INTERVAL 2 HOUR), '805 退房清洁 + 布草更换', NULL),
(@t, 9, '维修', '处理中', 8, DATE_ADD(NOW(), INTERVAL 6 HOUR), '810 空调不制冷，已联系售后', NULL),
(@t, 3, '清洁', '已完成', 8, DATE_SUB(NOW(), INTERVAL 1 DAY), '803 日常保洁', DATE_SUB(NOW(), INTERVAL 20 HOUR));

INSERT INTO mkt_activity (tenant_id, name, activity_type, rule_json, start_time, end_time, status, join_count) VALUES
(@t, '冬季签约立减 1000', '满减', '{"threshold":30000,"amount":1000}', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 40 DAY), '进行中', 12),
(@t, '老带新双方各得 500 元', '赠品', '{"referrer":500,"referee":500}', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_ADD(NOW(), INTERVAL 120 DAY), '进行中', 41),
(@t, '42 天套餐 9 折', '折扣', '{"packageId":2,"discount":0.9}', DATE_SUB(NOW(), INTERVAL 90 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), '已结束', 28);

INSERT INTO member_address (tenant_id, member_id, receiver, phone, region, detail, is_default) VALUES
(@t, 1, '张雅雯', 'zakIn597INNA5Nn0YZqNCkyPBka0Dbjt6+FrDEuY3gtui8O/TDhy', '上海市 徐汇区', '宜山路 700 号 3 号楼 502', 1),
(@t, 2, '李静怡', 'QsefHb/hK/F2HNBdD97WWOsa7uIYx3MflZGdkr/sAUB9bxMwbrRy', '上海市 浦东新区', '张江路 88 弄 12 号 1801', 1);

-- -------------------------------------------------------------
-- 十四、内容库（覆盖官网首页各区块与多端展示）
--     先清空再重建，保证重复执行结果一致
-- -------------------------------------------------------------
DELETE FROM cms_content WHERE tenant_id = @t;
INSERT INTO cms_content (tenant_id, content_type, title, cover, body_html, apply_ends, seo_title, seo_keywords,
 seo_description, authorized, status, published_time) VALUES
(@t, 'Banner', '专业照护，安心月子', '/files/tenants/demo/content/hero-1.png',
 '<h2>一对一母婴护理</h2><p>持证护理师 24 小时值守，每日体征监测、伤口护理与母乳喂养指导。</p>',
 '官网,小程序,H5', '专业照护，安心月子', '月子中心,母婴护理,产后修复', '月子中心专业照护服务，28/42 天套餐可选。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 40 DAY)),
(@t, '品牌故事', '十二年，只做一件事', '/files/tenants/demo/content/cover-2.png',
 '<p>从一间 8 间房的会所开始，我们把护理流程拆成 168 个标准动作：几点喂奶、体温有没有波动、伤口恢复得怎么样，都要记下来。</p><p>每一位护理师都要通过 3 个月带教与考核才能独立值守。</p>',
 '官网', '十二年只做一件事', '品牌故事,月子中心', '十二年专注母婴护理，把专业变成日常。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 35 DAY)),
(@t, '房型展示', '舒适单间：朝南落地窗，日照充足', '/files/tenants/demo/rooms/room-1.png',
 '<p>28㎡，1.5m 双人床，独立卫浴，配备哺乳灯与婴儿护理台。适合一位妈妈加一位陪护。</p>',
 '官网,小程序,H5', '舒适单间', '月子房,单间,28㎡', '28㎡ 舒适单间，朝南落地窗。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '房型展示', '尊享套房：一室一厅，家属可陪住', '/files/tenants/demo/rooms/room-2.png',
 '<p>42㎡，客厅分区，可接待家属；独立婴儿护理台与温奶设备，护理师 24 小时值守。</p>',
 '官网,小程序', '尊享套房', '月子套房,42㎡', '42㎡ 尊享套房，一室一厅。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '房型展示', '家庭双床房：双床设计，夜间护理不打扰', '/files/tenants/demo/rooms/room-3.png',
 '<p>36㎡，双 1.35m 床，另设独立休憩区，适合家属全程陪护。</p>',
 '官网,小程序', '家庭双床房', '月子房,双床,陪护', '36㎡ 家庭双床房，陪护友好。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '房型展示', '豪华套房：双卫加观景阳台', '/files/tenants/demo/rooms/room-4.png',
 '<p>56㎡，双卫、观景阳台与独立婴儿房，配备按摩椅与产后修复设备。</p>',
 '官网,小程序', '豪华套房', '月子套房,双卫,阳台', '56㎡ 豪华套房，二胎家庭首选。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@t, '康复项目', '盆底肌修复：从评估到居家训练', '/files/tenants/demo/content/cover-3.png',
 '<p>先做盆底肌力评估，再制定电刺激 + 生物反馈 + 凯格尔训练的组合方案，10 次为一疗程。</p>',
 '官网,小程序', '盆底肌修复', '产后修复,盆底肌,凯格尔', '产后盆底肌评估与修复方案。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(@t, '康复项目', '腹直肌分离修复', '/files/tenants/demo/content/cover-4.png',
 '<p>先测分离指宽，再通过呼吸训练与核心激活逐步闭合，避免盲目卷腹加重分离。</p>',
 '官网,小程序', '腹直肌修复', '腹直肌,产后修复', '腹直肌分离评估与修复。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 19 DAY)),
(@t, '康复项目', '产后肩颈舒缓', '/files/tenants/demo/content/cover-2.png',
 '<p>针对长时间抱娃与喂奶造成的肩颈紧张，手法放松配合热敷，单次 30 分钟。</p>',
 '官网,小程序', '肩颈舒缓', '肩颈,推拿,产后', '产后肩颈舒缓与推拿。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(@t, '康复项目', '中医艾灸调理', '/files/tenants/demo/content/cover-3.png',
 '<p>中医师辨证取穴，温经散寒、调理气血，适合怕冷、恶露不尽、睡眠差的妈妈。</p>',
 '官网,小程序', '艾灸调理', '艾灸,中医,产后调理', '产后中医艾灸调理。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 17 DAY)),
(@t, '月子餐', '四阶段调理：排、修、补、调', '/files/tenants/demo/content/hero-1.png',
 '<p>第一周排恶露、第二周修伤口、第三周补气血、第四周调体质，营养师按体质与忌口调整。</p>',
 '官网,小程序,H5', '四阶段月子餐', '月子餐,营养,四阶段', '四阶段月子餐调理方案。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 18 DAY)),
(@t, '月子餐', '三正三辅：每天六次出餐', '/files/tenants/demo/content/cover-4.png',
 '<p>三顿正餐加三次加餐，少食多餐稳定血糖，出餐前核对忌口与过敏原。</p>',
 '官网,小程序', '每天六次出餐', '月子餐,加餐,忌口', '三正三辅每天六次出餐。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(@t, '团队', '周敏 · 护士长', '/files/tenants/demo/content/cover-3.png',
 '<p>12 年三甲医院产科经验，负责护理方案制定、质量抽查与异常处置。</p>',
 '官网', '周敏 护士长', '护士长,护理团队', '护士长周敏，12 年产科经验。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@t, '团队', '林小雨 · 母婴护理师', '/files/tenants/demo/content/cover-2.png',
 '<p>持母婴护理证，擅长母乳喂养指导与新生儿抚触，累计服务 80+ 家庭。</p>',
 '官网', '林小雨 护理师', '护理师,母乳喂养', '母婴护理师林小雨。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@t, '团队', '郑好 · 康复治疗师', '/files/tenants/demo/content/cover-4.png',
 '<p>康复治疗师资格证，主做盆底肌与腹直肌修复，按评估结果调整疗程强度。</p>',
 '官网', '郑好 康复治疗师', '康复师,盆底肌', '康复治疗师郑好。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@t, '团队', '孙悦 · 营养师', '/files/tenants/demo/content/cover-3.png',
 '<p>注册营养师，按体质、忌口与哺乳需求定制餐单，每周复核体重与乳汁情况。</p>',
 '官网', '孙悦 营养师', '营养师,月子餐', '注册营养师孙悦。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@t, '案例', '42 天，一位剖宫产妈妈的恢复记录', '/files/tenants/demo/content/cover-3.png',
 '<p>入院时切口疼痛评分 4 分，第 7 天降至 1 分；42 天复查子宫复旧良好，盆底肌力由 2 级提升至 3 级。</p>',
 '官网,小程序', '42 天恢复记录', '客户案例,剖宫产', '剖宫产妈妈 42 天恢复案例。', 1, '已发布', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(@t, '案例', '顺产 + 纯母乳：从焦虑到从容', '/files/tenants/demo/content/cover-2.png',
 '<p>入院第 3 天实现纯母乳喂养，出院时宝宝体重已回到出生体重。</p>',
 '官网,小程序', '顺产母乳案例', '客户案例,顺产,母乳', '顺产妈妈纯母乳喂养案例。', 1, '已发布', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(@t, '案例', '双胎家庭：两个人手怎么排班', '/files/tenants/demo/content/cover-4.png',
 '<p>为双胎家庭配置两名护理师错峰值守，夜间喂奶记录同步到家属端。</p>',
 '官网,小程序', '双胎家庭照护', '客户案例,双胎', '双胎家庭护理排班案例。', 1, '已发布', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@t, '资讯', '产后第一周，这 5 件事比进补更重要', NULL,
 '<p>早开奶、勤排恶露、适度下床、情绪观察、伤口清洁。</p>',
 '官网,公众号', '产后第一周注意事项', '产后护理,新生儿', '产后第一周护理要点。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(@t, '资讯', '如何判断宝宝吃饱了？', NULL,
 '<p>看尿量、看体重、看吃奶后的满足感，比看时间更可靠。</p>',
 '官网,公众号,小程序', '判断宝宝吃饱', '母乳喂养,新生儿', '判断宝宝是否吃饱的三个信号。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(@t, '资讯', '剖宫产伤口怎么护理才不留疤', NULL,
 '<p>保持干燥、按期换药、避免牵拉，出现红肿渗液及时联系护士。</p>',
 '官网,公众号', '剖宫产伤口护理', '剖宫产,伤口护理', '剖宫产伤口护理要点。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(@t, '资讯', '月子期能不能洗头洗澡？', NULL,
 '<p>可以，但要注意水温、时长与保暖，洗完立即吹干。</p>',
 '官网,公众号', '月子期洗头洗澡', '月子,洗头,洗澡', '月子期清洁的正确做法。', 0, '已发布', DATE_SUB(NOW(), INTERVAL 2 DAY));
