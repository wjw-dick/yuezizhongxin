#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按表结构批量生成 MyBatis-Plus 实体与 Mapper。

用法：python scripts/gen_backend.py
说明：字段顺序与 sql/0*.sql 一致；JSON 列使用 JacksonTypeHandler，其余列走默认
      下划线转驼峰映射。生成结果直接写入各 Maven 模块的 src/main/java。
只生成「数据结构」，业务逻辑仍在 service 中手写，避免把规则藏进模板。
"""
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE_PKG = "com.yzz"

ID = ("id", "Long", "主键", "")
TENANT = ("tenantId", "Long", "租户标识", "")
CREATED = ("createdTime", "LocalDateTime", "创建时间", "")
UPDATED = ("updatedTime", "LocalDateTime", "更新时间", "")
DELETED = ("deleted", "Integer", "逻辑删除", "logic")

# (模块, 表名, 类名, 类注释, 字段列表)
SPEC = [
    # ---------------- 房态（T30-T32） ----------------
    ("room", "room_type", "RoomType", "房型。图片、配置与富文本详情用于官网与小程序展示。", [
        ID, TENANT, ("name", "String", "房型名称", ""),
        ("level", "String", "房型档位：温馨单间/豪华套间/尊享套房", ""),
        ("area", "BigDecimal", "面积（㎡）", ""),
        ("maxGuest", "Integer", "可住人数", ""),
        ("bedType", "String", "床型", ""),
        ("facilities", "String", "房间配置", ""),
        ("images", "List<String>", "实景图集（对象存储键）", "json"),
        ("description", "String", "房型介绍（富文本）", ""),
        ("status", "String", "状态：ENABLED/DISABLED", ""),
        ("sort", "Integer", "排序", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("room", "room", "Room", "房间台账。房态五态由档期与入馆出馆联动维护。", [
        ID, TENANT, ("roomNo", "String", "房间编号", ""),
        ("roomTypeId", "Long", "房型标识", ""),
        ("floor", "String", "楼层", ""),
        ("status", "String", "房态：AVAILABLE/LOCKED/OCCUPIED/CLEANING/MAINTENANCE", ""),
        ("remark", "String", "备注", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("room", "room_schedule", "RoomSchedule", "房间档期。同租户同房间同日期唯一，作为并发抢房的数据库兜底。", [
        ID, TENANT, ("roomId", "Long", "房间标识", ""),
        ("bizDate", "LocalDate", "档期日期", ""),
        ("state", "String", "档期状态：LOCKED/OCCUPIED/CLEANING/MAINTENANCE", ""),
        ("orderId", "Long", "占用订单", ""),
        ("holdExpireTime", "LocalDateTime", "销售预留过期时间", ""),
        ("remark", "String", "备注（换房、延期留痕）", ""),
        CREATED, UPDATED,
    ]),
    ("room", "room_task", "RoomTask", "房间清洁与维修任务。", [
        ID, TENANT, ("roomId", "Long", "房间标识", ""),
        ("taskType", "String", "类型：清洁/维修", ""),
        ("status", "String", "状态：待处理/进行中/已完成", ""),
        ("assigneeId", "Long", "执行人", ""),
        ("dueTime", "LocalDateTime", "要求完成时间", ""),
        ("remark", "String", "任务说明", ""),
        ("finishedTime", "LocalDateTime", "完成时间", ""),
        CREATED,
    ]),
    # ---------------- 套餐与商品（T33） ----------------
    ("product", "pkg_package", "PkgPackage", "套餐。价格为套餐总价，定金按比例计算。", [
        ID, TENANT, ("name", "String", "套餐名称", ""),
        ("days", "Integer", "套餐天数", ""),
        ("roomTypeLevel", "String", "适用房型档位", ""),
        ("careMode", "String", "护理模式", ""),
        ("mealStandard", "String", "月子餐标准", ""),
        ("rehabItems", "String", "含康复项目清单", ""),
        ("price", "BigDecimal", "套餐价格（元）", ""),
        ("depositRatio", "BigDecimal", "定金比例（%）", ""),
        ("storeIds", "String", "可售门店标识，逗号分隔", ""),
        ("status", "String", "状态：ON_SHELF/OFF_SHELF", ""),
        ("sort", "Integer", "排序", ""),
        ("detailHtml", "String", "套餐详情富文本", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("product", "pkg_item", "PkgItem", "套餐包含项。", [
        ID, TENANT, ("packageId", "Long", "套餐标识", ""),
        ("itemType", "String", "类型：康复项目/增值服务", ""),
        ("itemName", "String", "项目名称", ""),
        ("quantity", "Integer", "含数量", ""),
        ("price", "BigDecimal", "单价（元）", ""),
    ]),
    ("product", "prod_product", "ProdProduct", "商品。", [
        ID, TENANT, ("name", "String", "商品名称", ""),
        ("category", "String", "分类", ""),
        ("price", "BigDecimal", "售价（元）", ""),
        ("stock", "Integer", "库存", ""),
        ("images", "List<String>", "商品图", "json"),
        ("detailHtml", "String", "详情富文本", ""),
        ("status", "String", "状态：ON_SHELF/OFF_SHELF", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("product", "prod_sku", "ProdSku", "商品规格。", [
        ID, TENANT, ("productId", "Long", "商品标识", ""),
        ("skuName", "String", "规格名称", ""),
        ("price", "BigDecimal", "规格售价", ""),
        ("stock", "Integer", "规格库存", ""),
    ]),
    # ---------------- 线索 CRM（T35） ----------------
    ("crm", "crm_lead", "CrmLead", "销售线索。手机号加密存储，列表展示脱敏。", [
        ID, TENANT, ("customerName", "String", "联系人", ""),
        ("phone", "String", "手机号（加密存储）", ""),
        ("dueDate", "LocalDate", "预产期", ""),
        ("storeId", "Long", "意向门店", ""),
        ("intendRoomType", "String", "意向房型", ""),
        ("budgetMin", "BigDecimal", "预算下限", ""),
        ("budgetMax", "BigDecimal", "预算上限", ""),
        ("sourceCode", "String", "线索来源（字典中文名）", ""),
        ("stage", "String", "跟进阶段", ""),
        ("advisorId", "Long", "归属销售顾问", ""),
        ("nextFollowTime", "LocalDateTime", "下次跟进时间", ""),
        ("lostReason", "String", "流失原因", ""),
        ("remark", "String", "备注", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("crm", "crm_follow", "CrmFollow", "线索跟进记录。", [
        ID, TENANT, ("leadId", "Long", "线索标识", ""),
        ("followTime", "LocalDateTime", "跟进时间", ""),
        ("content", "String", "沟通内容", ""),
        ("result", "String", "通话结果", ""),
        ("nextTime", "LocalDateTime", "下次提醒时间", ""),
        ("operatorId", "Long", "跟进人", ""),
        CREATED,
    ]),
    ("crm", "crm_visit", "CrmVisit", "参观预约。", [
        ID, TENANT, ("leadId", "Long", "线索标识", ""),
        ("visitDate", "LocalDate", "参观日期", ""),
        ("timeSlot", "String", "时段", ""),
        ("personCount", "Integer", "到店人数", ""),
        ("advisorId", "Long", "接待顾问", ""),
        ("checkedIn", "Integer", "是否到店签到", ""),
        ("feedback", "String", "参观后评价", ""),
        CREATED,
    ]),
    # ---------------- 订单与支付（T36-T37） ----------------
    ("order", "biz_order", "BizOrder", "订单。金额构成拆为套餐/增值/商品/优惠四段，便于对账。", [
        ID, TENANT, ("orderNo", "String", "订单编号", ""),
        ("leadId", "Long", "来源线索", ""),
        ("customerName", "String", "客户姓名", ""),
        ("phone", "String", "客户手机号（加密）", ""),
        ("packageId", "Long", "套餐标识", ""),
        ("roomTypeId", "Long", "房型标识", ""),
        ("roomId", "Long", "房间标识", ""),
        ("checkinDate", "LocalDate", "入住日期", ""),
        ("checkoutDate", "LocalDate", "出馆日期", ""),
        ("careMode", "String", "护理模式", ""),
        ("companionCount", "Integer", "陪护人数", ""),
        ("packageAmount", "BigDecimal", "套餐费", ""),
        ("extraAmount", "BigDecimal", "增值服务费", ""),
        ("productAmount", "BigDecimal", "商品费", ""),
        ("discountAmount", "BigDecimal", "优惠金额", ""),
        ("totalAmount", "BigDecimal", "应收总额", ""),
        ("depositAmount", "BigDecimal", "定金", ""),
        ("tailAmount", "BigDecimal", "尾款", ""),
        ("bondAmount", "BigDecimal", "押金", ""),
        ("paidAmount", "BigDecimal", "已收金额", ""),
        ("status", "String", "订单状态", ""),
        ("source", "String", "订单来源", ""),
        ("signType", "String", "签约方式", ""),
        ("remark", "String", "备注", ""),
        ("createdBy", "Long", "创建人", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("order", "biz_order_item", "BizOrderItem", "订单明细。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("itemType", "String", "类型：套餐/增值服务/商品/康复项目", ""),
        ("itemName", "String", "项目名称", ""),
        ("quantity", "Integer", "数量", ""),
        ("price", "BigDecimal", "单价", ""),
        ("amount", "BigDecimal", "金额", ""),
    ]),
    ("order", "biz_payment", "BizPayment", "收款单。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("paymentNo", "String", "收款单号", ""),
        ("payType", "String", "款项类型：定金/尾款/押金", ""),
        ("method", "String", "支付方式", ""),
        ("amount", "BigDecimal", "金额", ""),
        ("status", "String", "状态：待支付/已支付/支付异常/已退款", ""),
        ("tradeNo", "String", "微信支付交易号", ""),
        ("paidTime", "LocalDateTime", "支付时间", ""),
        ("operatorId", "Long", "收款人", ""),
        ("remark", "String", "备注", ""),
        CREATED,
    ]),
    ("order", "biz_refund", "BizRefund", "退款单。违约金与可退金额由规则计算，审批后执行。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("refundNo", "String", "退款单号", ""),
        ("amount", "BigDecimal", "退款金额", ""),
        ("penaltyAmount", "BigDecimal", "违约金", ""),
        ("reasonCode", "String", "退款原因（字典中文名）", ""),
        ("reason", "String", "补充说明", ""),
        ("status", "String", "状态：待审核/已通过/已驳回/已退款", ""),
        ("auditBy", "Long", "审核人", ""),
        ("auditTime", "LocalDateTime", "审核时间", ""),
        CREATED,
    ]),
    ("order", "biz_order_contract", "BizOrderContract", "订单合同附件。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("fileKey", "String", "合同附件键", ""),
        ("fileName", "String", "文件名", ""),
        ("signedTime", "LocalDateTime", "签署时间", ""),
        CREATED,
    ]),
    ("order", "biz_order_change", "BizOrderChange", "订单变更留痕（换房/延期/加项/改价/退订）。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("changeType", "String", "变更类型", ""),
        ("beforeJson", "String", "变更前快照", ""),
        ("afterJson", "String", "变更后快照", ""),
        ("amountDiff", "BigDecimal", "金额差额", ""),
        ("reason", "String", "变更原因", ""),
        ("operatorId", "Long", "操作人", ""),
        CREATED,
    ]),
    # ---------------- 入出馆（T38-T39） ----------------
    ("stay", "stay_checkin", "StayCheckin", "入馆登记。证件号加密存储，健康告知必须签署。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("roomId", "Long", "房间标识", ""),
        ("motherName", "String", "产妇姓名", ""),
        ("motherAge", "Integer", "产妇年龄", ""),
        ("gestationalWeek", "Integer", "孕周", ""),
        ("deliveryType", "String", "分娩方式", ""),
        ("deliveryDate", "LocalDate", "分娩日期", ""),
        ("babyBirthDate", "LocalDate", "新生儿出生日期", ""),
        ("babyWeight", "BigDecimal", "新生儿体重（kg）", ""),
        ("feedingType", "String", "喂养方式", ""),
        ("familyInfo", "String", "家属与陪护人", ""),
        ("emergencyContact", "String", "紧急联系人", ""),
        ("idCardNo", "String", "证件号（加密）", ""),
        ("healthNoticeSigned", "Integer", "健康告知是否签署", ""),
        ("depositAmount", "BigDecimal", "押金金额", ""),
        ("checkinTime", "LocalDateTime", "入馆时间", ""),
        ("status", "String", "状态：在住/已出馆", ""),
        CREATED,
    ]),
    ("stay", "stay_checkout", "StayCheckout", "出馆结算。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("checkinId", "Long", "入馆登记标识", ""),
        ("tailAmount", "BigDecimal", "尾款", ""),
        ("extraAmount", "BigDecimal", "增值消费", ""),
        ("depositRefund", "BigDecimal", "退还押金", ""),
        ("itemReturned", "Integer", "物品是否归还齐全", ""),
        ("summary", "String", "出馆小结", ""),
        ("satisfaction", "Integer", "满意度评分", ""),
        ("reviewAppointment", "LocalDate", "42 天复查预约日期", ""),
        ("checkoutTime", "LocalDateTime", "出馆时间", ""),
        ("operatorId", "Long", "经办人", ""),
        CREATED,
    ]),
    # ---------------- 护理（T40-T43） ----------------
    ("care", "care_plan", "CarePlan", "护理计划。按套餐与分娩方式生成默认模板后下发护理师端。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("customerName", "String", "客户姓名", ""),
        ("roomId", "Long", "房间标识", ""),
        ("careMode", "String", "护理模式", ""),
        ("templateCode", "String", "模板编码", ""),
        ("status", "String", "状态：执行中/已结束", ""),
        CREATED, UPDATED,
    ]),
    ("care", "care_plan_item", "CarePlanItem", "护理计划项。完成时间由服务端写入，不接受客户端时间。", [
        ID, TENANT, ("planId", "Long", "护理计划标识", ""),
        ("itemName", "String", "护理项目", ""),
        ("targetType", "String", "对象：产妇/新生儿", ""),
        ("planDate", "LocalDate", "计划日期", ""),
        ("timeSlot", "String", "时段", ""),
        ("nurseId", "Long", "执行护理师", ""),
        ("status", "String", "状态：待执行/已完成/已跳过", ""),
        ("finishedTime", "LocalDateTime", "完成时间", ""),
    ]),
    ("care", "care_record", "CareRecord", "护理记录。提交后 24 小时内可改并留痕，超时需主管审批。", [
        ID, TENANT, ("planItemId", "Long", "护理计划项标识", ""),
        ("orderId", "Long", "订单标识", ""),
        ("targetType", "String", "对象：产妇/新生儿", ""),
        ("itemName", "String", "护理项目", ""),
        ("temperature", "BigDecimal", "体温（℃）", ""),
        ("bloodPressure", "String", "血压", ""),
        ("pulse", "Integer", "脉搏", ""),
        ("weight", "BigDecimal", "体重（kg）", ""),
        ("feedingAmount", "BigDecimal", "喂养量（ml）", ""),
        ("jaundiceValue", "BigDecimal", "黄疸测值", ""),
        ("stoolCount", "Integer", "大小便次数", ""),
        ("content", "String", "记录内容", ""),
        ("photos", "List<String>", "照片", "json"),
        ("nurseId", "Long", "提交人", ""),
        ("submitTime", "LocalDateTime", "提交时间（服务端）", ""),
        ("backfillReason", "String", "补录原因", ""),
        ("status", "String", "状态：已提交/待审批", ""),
        CREATED,
    ]),
    ("care", "care_handover", "CareHandover", "交接班。双方签名后生效，未完成事项自动延续。", [
        ID, TENANT, ("shiftDate", "LocalDate", "班次日期", ""),
        ("shiftType", "String", "班次：早班/中班/夜班", ""),
        ("fromNurseId", "Long", "交班人", ""),
        ("toNurseId", "Long", "接班人", ""),
        ("watchPoints", "String", "重点观察对象", ""),
        ("unfinished", "String", "未完成事项", ""),
        ("abnormalNotes", "String", "异常延续", ""),
        ("fromSign", "String", "交班签名图键", ""),
        ("toSign", "String", "接班签名图键", ""),
        ("confirmedTime", "LocalDateTime", "接班确认时间", ""),
        ("status", "String", "状态：待确认/已确认", ""),
        CREATED,
    ]),
    ("care", "care_abnormal", "CareAbnormal", "护理异常单。特急即时推送护理主管。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("abnormalType", "String", "异常类型（字典中文名）", ""),
        ("urgency", "String", "紧急程度：一般/紧急/特急", ""),
        ("description", "String", "描述", ""),
        ("photos", "List<String>", "照片", "json"),
        ("reporterId", "Long", "上报人", ""),
        ("supervisorId", "Long", "护理主管", ""),
        ("handleResult", "String", "处置记录", ""),
        ("familyNotified", "Integer", "是否告知家属", ""),
        ("status", "String", "状态：待处理/处理中/已闭环", ""),
        CREATED, ("closedTime", "LocalDateTime", "闭环时间", ""),
    ]),
    # ---------------- 康复（T34/T44） ----------------
    ("rehab", "rehab_item", "RehabItem", "康复项目库。", [
        ID, TENANT, ("name", "String", "项目名称", ""),
        ("stage", "String", "适用阶段（字典中文名）", ""),
        ("durationMin", "Integer", "单次时长（分钟）", ""),
        ("courseTimes", "Integer", "疗程次数", ""),
        ("price", "BigDecimal", "价格", ""),
        ("qualification", "String", "人员资质要求", ""),
        ("status", "String", "状态：ENABLED/DISABLED", ""),
        CREATED,
    ]),
    ("rehab", "rehab_assess", "RehabAssess", "产后康复评估。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("pelvicScore", "Integer", "盆底肌评分", ""),
        ("diastasisCm", "BigDecimal", "腹直肌分离（cm）", ""),
        ("posture", "String", "体态评估", ""),
        ("moodScore", "Integer", "情绪量表得分", ""),
        ("report", "String", "评估报告与建议疗程", ""),
        ("assessorId", "Long", "评估师", ""),
        ("assessTime", "LocalDateTime", "评估时间", ""),
        CREATED,
    ]),
    ("rehab", "rehab_course", "RehabCourse", "疗程卡与余额。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("itemId", "Long", "康复项目", ""),
        ("totalTimes", "Integer", "总次数", ""),
        ("usedTimes", "Integer", "已用次数", ""),
        ("expireDate", "LocalDate", "有效期", ""),
        ("source", "String", "来源：套餐赠送/单独购买", ""),
        CREATED,
    ]),
    ("rehab", "rehab_appointment", "RehabAppointment", "康复预约。治疗师与时段冲突由服务层校验。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("courseId", "Long", "疗程卡标识", ""),
        ("roomId", "Long", "康复室", ""),
        ("therapistId", "Long", "治疗师", ""),
        ("appointDate", "LocalDate", "预约日期", ""),
        ("startTime", "String", "开始时间", ""),
        ("endTime", "String", "结束时间", ""),
        ("status", "String", "状态：已预约/已签到/已取消/已完成", ""),
        CREATED,
    ]),
    ("rehab", "rehab_record", "RehabRecord", "康复执行记录。", [
        ID, TENANT, ("appointmentId", "Long", "预约标识", ""),
        ("itemName", "String", "项目名称", ""),
        ("durationMin", "Integer", "实际时长", ""),
        ("therapistId", "Long", "治疗师", ""),
        ("feedback", "String", "客户反馈", ""),
        ("nextAppoint", "LocalDate", "下次预约日期", ""),
        CREATED,
    ]),
    # ---------------- 月子餐（T45） ----------------
    ("meal", "meal_template", "MealTemplate", "月子餐阶段模板。", [
        ID, TENANT, ("stage", "String", "餐食阶段", ""),
        ("dayIndex", "Integer", "阶段内第几天", ""),
        ("mealType", "String", "餐次", ""),
        ("dishes", "String", "菜品清单", ""),
        ("avoidTags", "String", "本餐忌口标签", ""),
        CREATED,
    ]),
    ("meal", "meal_order", "MealOrder", "每日出餐。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("roomId", "Long", "房间", ""),
        ("mealDate", "LocalDate", "用餐日期", ""),
        ("mealType", "String", "餐次", ""),
        ("dishes", "String", "实际菜品", ""),
        ("specialTag", "String", "特殊标记", ""),
        ("extraCharge", "BigDecimal", "加餐收费", ""),
        ("checked", "Integer", "是否出餐核对", ""),
        ("abnormal", "String", "异常回传", ""),
        CREATED,
    ]),
    ("meal", "meal_restrict", "MealRestrict", "忌口与过敏原。", [
        ID, TENANT, ("orderId", "Long", "订单标识", ""),
        ("restrictType", "String", "忌口类型", ""),
        ("content", "String", "忌口内容/过敏原", ""),
        CREATED,
    ]),
    # ---------------- 人力（T46） ----------------
    ("staff", "staff_info", "StaffInfo", "人员档案。", [
        ID, TENANT, ("name", "String", "姓名", ""),
        ("phone", "String", "手机号（加密）", ""),
        ("jobType", "String", "岗位", ""),
        ("userId", "Long", "关联登录账号", ""),
        ("status", "String", "状态：在职/离职", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("staff", "staff_cert", "StaffCert", "资质证件，到期前 60 天提醒。", [
        ID, TENANT, ("staffId", "Long", "人员标识", ""),
        ("certType", "String", "证书类型", ""),
        ("certNo", "String", "证书编号", ""),
        ("fileKey", "String", "扫描件键", ""),
        ("expireDate", "LocalDate", "到期日期", ""),
        CREATED,
    ]),
    ("staff", "staff_schedule", "StaffSchedule", "排班。", [
        ID, TENANT, ("staffId", "Long", "人员标识", ""),
        ("scheduleDate", "LocalDate", "排班日期", ""),
        ("shiftType", "String", "班次", ""),
        ("floor", "String", "楼层", ""),
        ("orderId", "Long", "一对一负责订单", ""),
        CREATED,
    ]),
    ("staff", "staff_attendance", "StaffAttendance", "考勤打卡。", [
        ID, TENANT, ("staffId", "Long", "人员标识", ""),
        ("attendanceDate", "LocalDate", "考勤日期", ""),
        ("checkInTime", "LocalDateTime", "上班打卡", ""),
        ("checkOutTime", "LocalDateTime", "下班打卡", ""),
        ("location", "String", "打卡定位", ""),
        ("leaveType", "String", "请假类型", ""),
        CREATED,
    ]),
    ("staff", "staff_shift", "StaffShift", "班次定义。", [
        ID, TENANT, ("shiftName", "String", "班次名称", ""),
        ("startTime", "String", "开始时间", ""),
        ("endTime", "String", "结束时间", ""),
        ("crossDay", "Integer", "是否跨天", ""),
        CREATED,
    ]),
    # ---------------- 会员与营销（T47） ----------------
    ("member", "member_info", "MemberInfo", "会员。UnionID 为多端唯一口径。", [
        ID, TENANT, ("unionId", "String", "微信 UnionID", ""),
        ("openId", "String", "OpenID", ""),
        ("phone", "String", "手机号（加密）", ""),
        ("nickname", "String", "昵称", ""),
        ("avatar", "String", "头像", ""),
        ("source", "String", "来源渠道", ""),
        ("points", "Integer", "积分余额", ""),
        ("balance", "BigDecimal", "储值余额", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("member", "member_tag", "MemberTag", "会员标签。", [
        ID, TENANT, ("name", "String", "标签名称", ""), CREATED,
    ]),
    ("member", "member_tag_rel", "MemberTagRel", "会员标签关联。", [
        ID, TENANT, ("memberId", "Long", "会员标识", ""), ("tagId", "Long", "标签标识", ""),
    ]),
    ("member", "member_point_log", "MemberPointLog", "积分流水。", [
        ID, TENANT, ("memberId", "Long", "会员标识", ""),
        ("changeValue", "Integer", "变动值", ""),
        ("reason", "String", "变动原因", ""),
        CREATED,
    ]),
    ("member", "mkt_coupon", "MktCoupon", "优惠券模板。", [
        ID, TENANT, ("name", "String", "优惠券名称", ""),
        ("couponType", "String", "类型", ""),
        ("amount", "BigDecimal", "优惠金额", ""),
        ("threshold", "BigDecimal", "使用门槛", ""),
        ("totalCount", "Integer", "发行总量", ""),
        ("issuedCount", "Integer", "已发放数量", ""),
        ("startTime", "LocalDateTime", "生效时间", ""),
        ("endTime", "LocalDateTime", "失效时间", ""),
        ("status", "String", "状态", ""),
        CREATED,
    ]),
    ("member", "member_coupon", "MemberCoupon", "会员优惠券。", [
        ID, TENANT, ("memberId", "Long", "会员标识", ""),
        ("couponId", "Long", "优惠券标识", ""),
        ("status", "String", "状态：未使用/已使用/已过期", ""),
        ("usedTime", "LocalDateTime", "使用时间", ""),
        ("orderId", "Long", "核销订单", ""),
        CREATED,
    ]),
    ("member", "member_recharge", "MemberRecharge", "储值记录，预付卡规则公示留痕。", [
        ID, TENANT, ("memberId", "Long", "会员标识", ""),
        ("rechargeNo", "String", "储值单号", ""),
        ("amount", "BigDecimal", "储值金额", ""),
        ("giftAmount", "BigDecimal", "赠送金额", ""),
        ("status", "String", "状态：已支付/已退款", ""),
        ("ruleShown", "Integer", "是否已展示规则", ""),
        CREATED,
    ]),
    ("member", "mkt_referral", "MktReferral", "转介绍奖励。", [
        ID, TENANT, ("referrerMember", "Long", "推荐人会员标识", ""),
        ("referredLead", "Long", "被推荐线索", ""),
        ("rewardPoints", "Integer", "奖励积分", ""),
        ("rewardAmount", "BigDecimal", "奖励金额", ""),
        ("status", "String", "状态：待结算/已结算", ""),
        CREATED,
    ]),
    ("member", "mkt_activity", "MktActivity", "营销活动（拼团/限时优惠）。", [
        ID, TENANT, ("name", "String", "活动名称", ""),
        ("activityType", "String", "类型", ""),
        ("ruleJson", "String", "活动规则", ""),
        ("startTime", "LocalDateTime", "开始时间", ""),
        ("endTime", "LocalDateTime", "结束时间", ""),
        ("status", "String", "状态", ""),
        ("joinCount", "Integer", "参与人数", ""),
        CREATED,
    ]),
    ("member", "member_address", "MemberAddress", "会员收货地址。", [
        ID, TENANT, ("memberId", "Long", "会员标识", ""),
        ("receiver", "String", "收货人", ""),
        ("phone", "String", "手机号（加密）", ""),
        ("region", "String", "省市区", ""),
        ("detail", "String", "详细地址", ""),
        ("isDefault", "Integer", "是否默认", ""),
        CREATED,
    ]),
    # ---------------- 内容 CMS（T48/T56-T61） ----------------
    ("content", "cms_content", "CmsContent", "内容。发布前做适用端校验与敏感词检测。", [
        ID, TENANT, ("contentType", "String", "类型", ""),
        ("title", "String", "标题", ""),
        ("cover", "String", "封面图键", ""),
        ("bodyHtml", "String", "富文本正文", ""),
        ("applyEnds", "String", "适用端", ""),
        ("seoTitle", "String", "SEO 标题", ""),
        ("seoKeywords", "String", "SEO 关键词", ""),
        ("seoDescription", "String", "SEO 描述", ""),
        ("authorized", "Integer", "案例是否已授权", ""),
        ("status", "String", "状态：草稿/待审核/已发布/已下线", ""),
        ("publishedTime", "LocalDateTime", "发布时间", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("content", "cms_content_i18n", "CmsContentI18n", "内容多语言。", [
        ID, TENANT, ("contentId", "Long", "内容标识", ""),
        ("langCode", "String", "语言编码", ""),
        ("title", "String", "标题", ""),
        ("bodyHtml", "String", "正文", ""),
        ("translated", "Integer", "是否已翻译", ""),
    ]),
    # ---------------- 公众号（T62-T70） ----------------
    ("mp", "mp_account", "MpAccount", "公众号台账与凭证。密钥加密，接口只回掩码。", [
        ID, TENANT, ("accountName", "String", "公众号名称", ""),
        ("wechatId", "String", "微信号", ""),
        ("appId", "String", "AppID", ""),
        ("appSecret", "String", "AppSecret（加密）", ""),
        ("serverUrl", "String", "服务器地址", ""),
        ("token", "String", "消息令牌（加密）", ""),
        ("aesKey", "String", "加解密密钥（加密）", ""),
        ("oauthDomain", "String", "网页授权域名", ""),
        ("jsSafeDomain", "String", "JS 安全域名", ""),
        ("accountType", "String", "公众号类型", ""),
        ("verified", "Integer", "是否已认证", ""),
        ("qrcode", "String", "二维码图键", ""),
        ("remark", "String", "备注", ""),
        CREATED, UPDATED, DELETED,
    ]),
    ("mp", "mp_tag", "MpTag", "公众号标签。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("tagName", "String", "标签名称", ""),
        ("fanCount", "Integer", "粉丝数", ""),
        ("syncStatus", "String", "同步状态", ""),
        CREATED,
    ]),
    ("mp", "mp_fan", "MpFan", "公众号粉丝。UnionID 去重。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("openId", "String", "用户标识", ""),
        ("unionId", "String", "UnionID", ""),
        ("nickname", "String", "昵称", ""),
        ("avatar", "String", "头像", ""),
        ("gender", "String", "性别", ""),
        ("region", "String", "地区", ""),
        ("remark", "String", "备注", ""),
        ("subscribeStatus", "String", "订阅状态", ""),
        ("subscribeTime", "LocalDateTime", "关注时间", ""),
        ("unsubscribeTime", "LocalDateTime", "取关时间", ""),
        CREATED,
    ]),
    ("mp", "mp_message", "MpMessage", "公众号消息流水。错误码原样落库。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("openId", "String", "用户标识", ""),
        ("msgType", "String", "消息类型", ""),
        ("sender", "String", "发送方", ""),
        ("content", "String", "内容", ""),
        ("rawPayload", "String", "原始报文", ""),
        ("errorCode", "String", "错误码", ""),
        ("errorMsg", "String", "错误描述", ""),
        CREATED,
    ]),
    ("mp", "mp_material", "MpMaterial", "公众号素材。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("mediaId", "String", "素材标识", ""),
        ("name", "String", "素材名称", ""),
        ("materialType", "String", "类型：图片/语音/视频", ""),
        ("sizeBytes", "Long", "大小（字节）", ""),
        ("permanent", "Integer", "是否永久素材", ""),
        ("thumbMediaId", "String", "视频缩略图标识", ""),
        ("fileKey", "String", "对象存储键", ""),
        CREATED,
    ]),
    ("mp", "mp_draft", "MpDraft", "图文草稿与发表记录。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("title", "String", "标题", ""),
        ("author", "String", "作者", ""),
        ("sourceUrl", "String", "原文地址", ""),
        ("coverKey", "String", "封面图键", ""),
        ("digest", "String", "摘要", ""),
        ("bodyHtml", "String", "富文本正文", ""),
        ("articleIndex", "Integer", "多图文序号", ""),
        ("draftMediaId", "String", "草稿标识", ""),
        ("status", "String", "状态", ""),
        ("publishTime", "LocalDateTime", "发表时间", ""),
        ("publishResult", "String", "发表结果", ""),
        ("failReason", "String", "失败原因", ""),
        CREATED,
    ]),
    ("mp", "mp_menu_config", "MpMenuConfig", "公众号菜单配置（一级≤3、二级≤5）。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("configName", "String", "配置名称", ""),
        ("treeJson", "String", "菜单树 JSON", ""),
        ("published", "Integer", "是否已发布", ""),
        ("publishTime", "LocalDateTime", "发布时间", ""),
        CREATED,
    ]),
    ("mp", "mp_auto_reply", "MpAutoReply", "自动回复。", [
        ID, TENANT, ("accountId", "Long", "公众号标识", ""),
        ("replyType", "String", "类型", ""),
        ("keyword", "String", "关键词", ""),
        ("matchType", "String", "匹配类型", ""),
        ("msgType", "String", "回复消息类型", ""),
        ("content", "String", "回复内容", ""),
        CREATED,
    ]),
    # ---------------- 系统补充（T50-T52） ----------------
    ("system", "sys_menu", "SysMenu", "租户端菜单与按钮权限。", [
        ID, TENANT, ("parentId", "Long", "上级菜单", ""),
        ("title", "String", "菜单标题", ""),
        ("menuType", "String", "类型：目录/菜单/按钮", ""),
        ("path", "String", "路由路径", ""),
        ("perm", "String", "权限标识", ""),
        ("icon", "String", "图标", ""),
        ("sort", "Integer", "排序", ""),
        ("visible", "Integer", "是否可见", ""),
    ]),
    ("system", "sys_role_menu", "SysRoleMenu", "角色菜单关联。", [
        ID, TENANT, ("roleId", "Long", "角色标识", ""), ("menuId", "Long", "菜单标识", ""),
    ]),
    ("system", "sys_entry", "SysEntry", "端入口与带参码。", [
        ID, TENANT, ("entryType", "String", "类型", ""),
        ("name", "String", "名称", ""),
        ("storeId", "Long", "门店", ""),
        ("channelCode", "String", "渠道参数", ""),
        ("imageKey", "String", "码图键", ""),
        ("url", "String", "链接地址", ""),
        ("scanCount", "Integer", "扫码次数", ""),
        CREATED,
    ]),
    ("system", "sys_export_log", "SysExportLog", "数据导出留痕。", [
        ID, TENANT, ("module", "String", "导出模块", ""),
        ("purpose", "String", "导出用途", ""),
        ("rowCount", "Integer", "导出行数", ""),
        ("watermark", "String", "水印内容", ""),
        ("operatorId", "Long", "操作人", ""),
        CREATED,
    ]),
]

TYPE_IMPORTS = {
    "BigDecimal": "java.math.BigDecimal",
    "LocalDate": "java.time.LocalDate",
    "LocalDateTime": "java.time.LocalDateTime",
    "List<String>": "java.util.List",
}


def render_entity(module, table, cls, doc, fields):
    has_json = any(f[3] == "json" for f in fields)
    imports = {"com.baomidou.mybatisplus.annotation.IdType",
               "com.baomidou.mybatisplus.annotation.TableId",
               "com.baomidou.mybatisplus.annotation.TableName"}
    if has_json:
        imports.add("com.baomidou.mybatisplus.annotation.TableField")
        imports.add("com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler")
    if any(f[3] == "logic" for f in fields):
        imports.add("com.baomidou.mybatisplus.annotation.TableLogic")
    imports.add("lombok.Data")
    for f in fields:
        imp = TYPE_IMPORTS.get(f[1])
        if imp:
            imports.add(imp)

    lines = [f"package {BASE_PKG}.{module}.entity;", ""]
    for imp in sorted(imports):
        lines.append(f"import {imp};")
    lines += ["", "/**", f" * {doc}", " */", "@Data"]
    if has_json:
        lines.append(f'@TableName(value = "{table}", autoResultMap = true)')
    else:
        lines.append(f'@TableName("{table}")')
    lines.append(f"public class {cls} {{")
    lines.append("")
    for name, typ, comment, extra in fields:
        if comment:
            lines.append(f"    /** {comment}。 */")
        if extra == "json":
            lines.append("    @TableField(typeHandler = JacksonTypeHandler.class)")
        if extra == "logic":
            lines.append("    @TableLogic")
        if name == "id":
            lines.append("    @TableId(type = IdType.AUTO)")
        lines.append(f"    private {typ} {name};")
        lines.append("")
    if lines[-1] == "":
        lines.pop()
    lines += ["}", ""]
    return "\n".join(lines)


def render_mapper(module, table, cls, doc):
    return "\n".join([
        f"package {BASE_PKG}.{module}.mapper;",
        "",
        "import com.baomidou.mybatisplus.core.mapper.BaseMapper;",
        f"import {BASE_PKG}.{module}.entity.{cls};",
        "import org.apache.ibatis.annotations.Mapper;",
        "",
        f"/** {cls} 数据访问。 */",
        "@Mapper",
        f"public interface {cls}Mapper extends BaseMapper<{cls}> {{",
        "}",
        "",
    ])


def main():
    count = 0
    for module, table, cls, doc, fields in SPEC:
        for kind, content in (
            (f"entity/{cls}.java", render_entity(module, table, cls, doc, fields)),
            (f"mapper/{cls}Mapper.java", render_mapper(module, table, cls, doc)),
        ):
            path = ROOT / f"yzz-{module}" / "src/main/java" / BASE_PKG.replace(".", "/") / module / kind
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
            count += 1
    print(f"generated {count} files for {len(SPEC)} tables")


if __name__ == "__main__":
    main()
