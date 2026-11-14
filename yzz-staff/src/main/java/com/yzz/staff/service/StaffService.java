package com.yzz.staff.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.DesensitizeUtil;
import com.yzz.common.util.SecretCipher;
import com.yzz.staff.entity.StaffAttendance;
import com.yzz.staff.entity.StaffCert;
import com.yzz.staff.entity.StaffInfo;
import com.yzz.staff.entity.StaffSchedule;
import com.yzz.staff.entity.StaffShift;
import com.yzz.staff.mapper.StaffAttendanceMapper;
import com.yzz.staff.mapper.StaffCertMapper;
import com.yzz.staff.mapper.StaffInfoMapper;
import com.yzz.staff.mapper.StaffScheduleMapper;
import com.yzz.staff.mapper.StaffShiftMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 人员档案、资质、排班与考勤（T46）。
 *
 * <p>一对一配比校验：同一护理师同一天同一班次只能挂一个订单；
 * 证件到期前 60 天进入提醒列表，过期证件禁止排班。</p>
 */
@Service
public class StaffService {

    private final StaffInfoMapper staffMapper;
    private final StaffCertMapper certMapper;
    private final StaffScheduleMapper scheduleMapper;
    private final StaffAttendanceMapper attendanceMapper;
    private final StaffShiftMapper shiftMapper;
    private final SecretCipher secretCipher;

    public StaffService(StaffInfoMapper staffMapper, StaffCertMapper certMapper,
                        StaffScheduleMapper scheduleMapper, StaffAttendanceMapper attendanceMapper,
                        StaffShiftMapper shiftMapper, SecretCipher secretCipher) {
        this.staffMapper = staffMapper;
        this.certMapper = certMapper;
        this.scheduleMapper = scheduleMapper;
        this.attendanceMapper = attendanceMapper;
        this.shiftMapper = shiftMapper;
        this.secretCipher = secretCipher;
    }

    public List<StaffInfo> staffs(String jobType, String status) {
        List<StaffInfo> rows = staffMapper.selectList(Wrappers.<StaffInfo>lambdaQuery()
                .eq(jobType != null && !jobType.isBlank(), StaffInfo::getJobType, jobType)
                .eq(status != null && !status.isBlank(), StaffInfo::getStatus, status)
                .orderByAsc(StaffInfo::getId));
        rows.forEach(row -> row.setPhone(mask(row.getPhone())));
        return rows;
    }

    @Transactional
    public Long saveStaff(StaffInfo input) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "姓名不能为空");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getPhone() != null && !DesensitizeUtil.isMasked(input.getPhone())) {
            input.setPhone(secretCipher.encrypt(input.getPhone()));
        }
        if (input.getStatus() == null) {
            input.setStatus("在职");
        }
        if (input.getId() == null) {
            staffMapper.insert(input);
        } else {
            StaffInfo exist = staffMapper.selectById(input.getId());
            if (exist == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "人员不存在");
            }
            if (input.getPhone() == null || DesensitizeUtil.isMasked(input.getPhone())) {
                input.setPhone(exist.getPhone());
            }
            staffMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public Long addCert(StaffCert input) {
        if (input.getStaffId() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请选择人员");
        }
        input.setTenantId(TenantContext.requireTenantId());
        certMapper.insert(input);
        return input.getId();
    }

    public List<StaffCert> certs(Long staffId) {
        return certMapper.selectList(Wrappers.<StaffCert>lambdaQuery()
                .eq(staffId != null, StaffCert::getStaffId, staffId)
                .orderByAsc(StaffCert::getExpireDate));
    }

    /** 到期提醒：60 天内到期或已过期。 */
    public List<Map<String, Object>> expiringCerts(int days) {
        LocalDate limit = LocalDate.now().plusDays(days <= 0 ? 60 : days);
        List<StaffCert> rows = certMapper.selectList(Wrappers.<StaffCert>lambdaQuery()
                .le(StaffCert::getExpireDate, limit)
                .orderByAsc(StaffCert::getExpireDate));
        List<Map<String, Object>> result = new ArrayList<>();
        for (StaffCert cert : rows) {
            StaffInfo staff = staffMapper.selectById(cert.getStaffId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("cert", cert);
            item.put("staffName", staff == null ? "-" : staff.getName());
            item.put("expired", cert.getExpireDate() != null && cert.getExpireDate().isBefore(LocalDate.now()));
            result.add(item);
        }
        return result;
    }

    public List<StaffShift> shifts() {
        return shiftMapper.selectList(Wrappers.<StaffShift>lambdaQuery()
                .orderByAsc(StaffShift::getStartTime));
    }

    @Transactional
    public Long saveShift(StaffShift input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getId() == null) {
            shiftMapper.insert(input);
        } else {
            shiftMapper.updateById(input);
        }
        return input.getId();
    }

    public List<StaffSchedule> schedules(LocalDate from, LocalDate to, Long staffId) {
        return scheduleMapper.selectList(Wrappers.<StaffSchedule>lambdaQuery()
                .ge(from != null, StaffSchedule::getScheduleDate, from)
                .le(to != null, StaffSchedule::getScheduleDate, to)
                .eq(staffId != null, StaffSchedule::getStaffId, staffId)
                .orderByAsc(StaffSchedule::getScheduleDate));
    }

    /**
     * 批量排班。同人同日同班次只能一条；一对一订单在同一班次只能配一名护理师。
     */
    @Transactional
    public int saveSchedules(List<StaffSchedule> rows) {
        int saved = 0;
        for (StaffSchedule row : rows) {
            if (row.getStaffId() == null || row.getScheduleDate() == null || row.getShiftType() == null) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "排班需包含人员、日期与班次");
            }
            StaffInfo staff = staffMapper.selectById(row.getStaffId());
            if (staff == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "人员不存在：" + row.getStaffId());
            }
            if ("离职".equals(staff.getStatus())) {
                throw BusinessException.of(ErrorCode.CONFLICT, staff.getName() + " 已离职，不可排班");
            }
            List<StaffCert> certs = certs(row.getStaffId());
            boolean expired = certs.stream().anyMatch(c -> c.getExpireDate() != null
                    && c.getExpireDate().isBefore(LocalDate.now()));
            if (expired) {
                throw BusinessException.of(ErrorCode.CONFLICT, staff.getName() + " 存在过期证件，请先更新资质");
            }
            Long dup = scheduleMapper.selectCount(Wrappers.<StaffSchedule>lambdaQuery()
                    .eq(StaffSchedule::getStaffId, row.getStaffId())
                    .eq(StaffSchedule::getScheduleDate, row.getScheduleDate())
                    .eq(StaffSchedule::getShiftType, row.getShiftType()));
            if (dup > 0) {
                continue;
            }
            if (row.getOrderId() != null) {
                Long assigned = scheduleMapper.selectCount(Wrappers.<StaffSchedule>lambdaQuery()
                        .eq(StaffSchedule::getOrderId, row.getOrderId())
                        .eq(StaffSchedule::getScheduleDate, row.getScheduleDate())
                        .eq(StaffSchedule::getShiftType, row.getShiftType())
                        .ne(StaffSchedule::getStaffId, row.getStaffId()));
                if (assigned > 0) {
                    throw BusinessException.of(ErrorCode.CONFLICT,
                            "该订单在 " + row.getScheduleDate() + " " + row.getShiftType()
                                    + " 已配护理师，一对一配比不允许重复");
                }
            }
            row.setTenantId(TenantContext.requireTenantId());
            scheduleMapper.insert(row);
            saved++;
        }
        return saved;
    }

    @Transactional
    public void attendance(Long staffId, String type, String location) {
        LocalDate today = LocalDate.now();
        StaffAttendance record = attendanceMapper.selectOne(Wrappers.<StaffAttendance>lambdaQuery()
                .eq(StaffAttendance::getStaffId, staffId)
                .eq(StaffAttendance::getAttendanceDate, today)
                .last("limit 1"));
        if (record == null) {
            record = new StaffAttendance();
            record.setTenantId(TenantContext.requireTenantId());
            record.setStaffId(staffId);
            record.setAttendanceDate(today);
            record.setLocation(location);
            record.setCheckInTime(LocalDateTime.now());
            attendanceMapper.insert(record);
            return;
        }
        if ("out".equalsIgnoreCase(type)) {
            record.setCheckOutTime(LocalDateTime.now());
        } else {
            record.setCheckInTime(LocalDateTime.now());
        }
        if (location != null) {
            record.setLocation(location);
        }
        attendanceMapper.updateById(record);
    }

    public List<StaffAttendance> attendanceOf(LocalDate from, LocalDate to, Long staffId) {
        return attendanceMapper.selectList(Wrappers.<StaffAttendance>lambdaQuery()
                .ge(from != null, StaffAttendance::getAttendanceDate, from)
                .le(to != null, StaffAttendance::getAttendanceDate, to)
                .eq(staffId != null, StaffAttendance::getStaffId, staffId)
                .orderByDesc(StaffAttendance::getAttendanceDate));
    }

    /** 工作量统计：按人汇总班次数与出勤天数。 */
    public List<Map<String, Object>> workload(LocalDate from, LocalDate to) {
        List<StaffInfo> staffs = staffs(null, "在职");
        List<Map<String, Object>> result = new ArrayList<>();
        for (StaffInfo staff : staffs) {
            Long shifts = scheduleMapper.selectCount(Wrappers.<StaffSchedule>lambdaQuery()
                    .eq(StaffSchedule::getStaffId, staff.getId())
                    .ge(StaffSchedule::getScheduleDate, from)
                    .le(StaffSchedule::getScheduleDate, to));
            Long attendance = attendanceMapper.selectCount(Wrappers.<StaffAttendance>lambdaQuery()
                    .eq(StaffAttendance::getStaffId, staff.getId())
                    .ge(StaffAttendance::getAttendanceDate, from)
                    .le(StaffAttendance::getAttendanceDate, to)
                    .isNotNull(StaffAttendance::getCheckInTime));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("staffId", staff.getId());
            row.put("name", staff.getName());
            row.put("jobType", staff.getJobType());
            row.put("shiftCount", shifts);
            row.put("attendanceDays", attendance);
            result.add(row);
        }
        return result;
    }

    /**
     * 登录账号对应的在职员工档案（护理师端小程序用）。
     *
     * <p>员工档案未绑定登录账号时返回 null，由调用方提示「请联系管理员绑定账号」。</p>
     */
    public StaffInfo byUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return staffMapper.selectOne(Wrappers.<StaffInfo>lambdaQuery()
                .eq(StaffInfo::getUserId, userId)
                .eq(StaffInfo::getStatus, "在职")
                .last("limit 1"));
    }

    public StaffInfo requireStaff(Long id) {        StaffInfo staff = staffMapper.selectById(id);
        if (staff == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "人员不存在");
        }
        return staff;
    }

    private String mask(String encrypted) {
        try {
            return DesensitizeUtil.phone(secretCipher.decrypt(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }
}
