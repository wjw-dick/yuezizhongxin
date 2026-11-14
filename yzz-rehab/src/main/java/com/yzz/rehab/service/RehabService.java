package com.yzz.rehab.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.rehab.entity.RehabAppointment;
import com.yzz.rehab.entity.RehabAssess;
import com.yzz.rehab.entity.RehabCourse;
import com.yzz.rehab.entity.RehabItem;
import com.yzz.rehab.entity.RehabRecord;
import com.yzz.rehab.mapper.RehabAppointmentMapper;
import com.yzz.rehab.mapper.RehabAssessMapper;
import com.yzz.rehab.mapper.RehabCourseMapper;
import com.yzz.rehab.mapper.RehabItemMapper;
import com.yzz.rehab.mapper.RehabRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产后康复（T34、T44）。
 *
 * <p>疗程卡余额按「总次数 - 已用次数」实时计算；预约时校验治疗师同时段冲突与康复室冲突，
 * 冲突返回 40901，前端提示换时段。</p>
 */
@Service
public class RehabService {

    private final RehabItemMapper itemMapper;
    private final RehabAssessMapper assessMapper;
    private final RehabCourseMapper courseMapper;
    private final RehabAppointmentMapper appointmentMapper;
    private final RehabRecordMapper recordMapper;

    public RehabService(RehabItemMapper itemMapper, RehabAssessMapper assessMapper,
                        RehabCourseMapper courseMapper, RehabAppointmentMapper appointmentMapper,
                        RehabRecordMapper recordMapper) {
        this.itemMapper = itemMapper;
        this.assessMapper = assessMapper;
        this.courseMapper = courseMapper;
        this.appointmentMapper = appointmentMapper;
        this.recordMapper = recordMapper;
    }

    public List<RehabItem> items(String stage, String status) {
        return itemMapper.selectList(Wrappers.<RehabItem>lambdaQuery()
                .eq(stage != null && !stage.isBlank(), RehabItem::getStage, stage)
                .eq(status != null && !status.isBlank(), RehabItem::getStatus, status)
                .orderByAsc(RehabItem::getId));
    }

    @Transactional
    public Long saveItem(RehabItem input) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "项目名称不能为空");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getStatus() == null) {
            input.setStatus("ENABLED");
        }
        if (input.getId() == null) {
            itemMapper.insert(input);
        } else {
            itemMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public Long saveAssess(RehabAssess input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getAssessTime() == null) {
            input.setAssessTime(LocalDateTime.now());
        }
        input.setAssessorId(TenantContext.getUserId());
        assessMapper.insert(input);
        return input.getId();
    }

    public List<RehabAssess> assesses(Long orderId) {
        return assessMapper.selectList(Wrappers.<RehabAssess>lambdaQuery()
                .eq(orderId != null, RehabAssess::getOrderId, orderId)
                .orderByDesc(RehabAssess::getId));
    }

    /**
     * 生成疗程卡：套餐赠送按项目自带疗程次数，单独购买可指定次数。
     */
    @Transactional
    public Long createCourse(Long orderId, Long itemId, Integer totalTimes, String source) {
        RehabItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "康复项目不存在");
        }
        RehabCourse course = new RehabCourse();
        course.setTenantId(TenantContext.requireTenantId());
        course.setOrderId(orderId);
        course.setItemId(itemId);
        course.setTotalTimes(totalTimes == null ? item.getCourseTimes() : totalTimes);
        course.setUsedTimes(0);
        course.setExpireDate(LocalDate.now().plusMonths(6));
        course.setSource(source == null ? "套餐赠送" : source);
        courseMapper.insert(course);
        return course.getId();
    }

    public List<Map<String, Object>> courses(Long orderId) {
        List<RehabCourse> rows = courseMapper.selectList(Wrappers.<RehabCourse>lambdaQuery()
                .eq(orderId != null, RehabCourse::getOrderId, orderId)
                .orderByDesc(RehabCourse::getId));
        return rows.stream().map(row -> {
            RehabItem item = itemMapper.selectById(row.getItemId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("course", row);
            map.put("itemName", item == null ? "已删除项目" : item.getName());
            map.put("remain", row.getTotalTimes() - row.getUsedTimes());
            return map;
        }).toList();
    }

    /**
     * 预约。冲突校验：同治疗师同时段、同康复室同时段。
     */
    @Transactional
    public Long appoint(RehabAppointment input) {
        RehabCourse course = courseMapper.selectById(input.getCourseId());
        if (course == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "疗程卡不存在");
        }
        if (course.getUsedTimes() >= course.getTotalTimes()) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该疗程卡次数已用完，请先续购");
        }
        if (input.getAppointDate() == null || input.getStartTime() == null || input.getEndTime() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "预约日期与时段必填");
        }
        if (input.getStartTime().compareTo(input.getEndTime()) >= 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        List<RehabAppointment> sameDay = appointmentMapper.selectList(
                Wrappers.<RehabAppointment>lambdaQuery()
                        .eq(RehabAppointment::getAppointDate, input.getAppointDate())
                        .in(RehabAppointment::getStatus, List.of("已预约", "已签到")));
        for (RehabAppointment exist : sameDay) {
            boolean overlap = input.getStartTime().compareTo(exist.getEndTime()) < 0
                    && exist.getStartTime().compareTo(input.getEndTime()) < 0;
            if (!overlap) {
                continue;
            }
            if (input.getTherapistId() != null && input.getTherapistId().equals(exist.getTherapistId())) {
                throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT,
                        "治疗师 " + input.getStartTime() + "-" + input.getEndTime() + " 已有预约");
            }
            if (input.getRoomId() != null && input.getRoomId().equals(exist.getRoomId())) {
                throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT,
                        "康复室该时段已被占用，请更换时段");
            }
        }
        input.setTenantId(TenantContext.requireTenantId());
        input.setOrderId(course.getOrderId());
        input.setStatus("已预约");
        appointmentMapper.insert(input);
        return input.getId();
    }

    @Transactional
    public void cancelAppoint(Long id) {
        RehabAppointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "预约不存在");
        }
        if ("已完成".equals(appointment.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "已完成的预约不可取消");
        }
        appointment.setStatus("已取消");
        appointmentMapper.updateById(appointment);
    }

    /**
     * 客户自助预约（小程序/H5）：只能约套餐内、且疗程卡还有剩余次数的项目。
     *
     * <p>治疗师与康复室由门店排程时指派，落库状态为「待确认」，
     * 后台在预约排程页确认后转「已预约」；同一订单同时段不允许重复提交。</p>
     */
    @Transactional
    public Long requestAppoint(Long orderId, Long itemId, LocalDate date, String startTime, String endTime) {
        if (orderId == null || itemId == null || date == null || startTime == null || endTime == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "预约项目、日期与时段必填");
        }
        if (date.isBefore(LocalDate.now())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "预约日期不能早于今天");
        }
        if (startTime.compareTo(endTime) >= 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        RehabCourse course = courseMapper.selectOne(Wrappers.<RehabCourse>lambdaQuery()
                .eq(RehabCourse::getOrderId, orderId)
                .eq(RehabCourse::getItemId, itemId)
                .orderByDesc(RehabCourse::getId).last("limit 1"));
        if (course == null) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该订单未购买此康复项目，请联系前台顾问");
        }
        if (course.getUsedTimes() >= course.getTotalTimes()) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该项目次数已用完，请先续购");
        }
        Long dup = appointmentMapper.selectCount(Wrappers.<RehabAppointment>lambdaQuery()
                .eq(RehabAppointment::getOrderId, orderId)
                .eq(RehabAppointment::getAppointDate, date)
                .in(RehabAppointment::getStatus, List.of("待确认", "已预约", "已签到"))
                .le(RehabAppointment::getStartTime, startTime)
                .ge(RehabAppointment::getEndTime, endTime));
        if (dup > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该时段已提交过预约，请勿重复提交");
        }
        RehabAppointment appointment = new RehabAppointment();
        appointment.setTenantId(TenantContext.requireTenantId());
        appointment.setOrderId(orderId);
        appointment.setCourseId(course.getId());
        appointment.setAppointDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus("待确认");
        appointmentMapper.insert(appointment);
        return appointment.getId();
    }

    /** 客户自助取消预约：仅限本人订单的待确认/已预约。 */
    @Transactional
    public void cancelByCustomer(Long orderId, Long appointmentId) {
        RehabAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !orderId.equals(appointment.getOrderId())) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "预约不存在");
        }
        if (!List.of("待确认", "已预约").contains(appointment.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "当前状态不可取消：" + appointment.getStatus());
        }
        appointment.setStatus("已取消");
        appointmentMapper.updateById(appointment);
    }

    @Transactional
    public void checkin(Long id) {
        RehabAppointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "预约不存在");
        }
        appointment.setStatus("已签到");
        appointmentMapper.updateById(appointment);
    }

    /**
     * 执行并扣次，写执行记录。
     */
    @Transactional
    public Long execute(Long appointmentId, RehabRecord input) {
        RehabAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "预约不存在");
        }
        if ("已完成".equals(appointment.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该预约已完成");
        }
        RehabCourse course = courseMapper.selectById(appointment.getCourseId());
        if (course == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "疗程卡不存在");
        }
        if (course.getUsedTimes() >= course.getTotalTimes()) {
            throw BusinessException.of(ErrorCode.CONFLICT, "疗程次数已用完");
        }
        course.setUsedTimes(course.getUsedTimes() + 1);
        courseMapper.updateById(course);
        appointment.setStatus("已完成");
        appointmentMapper.updateById(appointment);

        RehabItem item = itemMapper.selectById(course.getItemId());
        input.setTenantId(TenantContext.requireTenantId());
        input.setAppointmentId(appointmentId);
        input.setItemName(item == null ? "康复项目" : item.getName());
        input.setTherapistId(appointment.getTherapistId());
        if (input.getDurationMin() == null) {
            input.setDurationMin(item == null ? 30 : item.getDurationMin());
        }
        recordMapper.insert(input);
        return input.getId();
    }

    public List<RehabAppointment> appointments(LocalDate date, Long therapistId) {
        return appointmentMapper.selectList(Wrappers.<RehabAppointment>lambdaQuery()
                .eq(date != null, RehabAppointment::getAppointDate, date)
                .eq(therapistId != null, RehabAppointment::getTherapistId, therapistId)
                .orderByAsc(RehabAppointment::getStartTime));
    }

    public List<RehabRecord> records(Long orderId) {
        List<RehabAppointment> appointments = appointmentMapper.selectList(
                Wrappers.<RehabAppointment>lambdaQuery().eq(RehabAppointment::getOrderId, orderId));
        if (appointments.isEmpty()) {
            return List.of();
        }
        List<Long> ids = appointments.stream().map(RehabAppointment::getId).toList();
        return recordMapper.selectList(Wrappers.<RehabRecord>lambdaQuery()
                .in(RehabRecord::getAppointmentId, ids).orderByDesc(RehabRecord::getId));
    }

    /** 某订单的康复预约（客户小程序展示用）。 */
    public List<RehabAppointment> appointmentsOfOrder(Long orderId) {
        return appointmentMapper.selectList(Wrappers.<RehabAppointment>lambdaQuery()
                .eq(RehabAppointment::getOrderId, orderId)
                .orderByDesc(RehabAppointment::getAppointDate));
    }
}
