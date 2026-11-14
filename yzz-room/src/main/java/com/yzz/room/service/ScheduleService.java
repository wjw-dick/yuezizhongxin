package com.yzz.room.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.room.entity.Room;
import com.yzz.room.entity.RoomSchedule;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.room.mapper.RoomScheduleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 档期日历、锁定与释放（T31、T32）。
 *
 * <p>并发抢房以数据库唯一键 `(tenant_id, room_id, biz_date)` 兜底：
 * 插入冲突直接抛 40901，绝不依赖「先查后写」。销售预留超时由定时任务释放。</p>
 */
@Service
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    /** 默认可配置的销售预留时长（分钟）。 */
    private static final int DEFAULT_HOLD_MINUTES = 30;

    private final RoomScheduleMapper scheduleMapper;
    private final RoomMapper roomMapper;

    public ScheduleService(RoomScheduleMapper scheduleMapper, RoomMapper roomMapper) {
        this.scheduleMapper = scheduleMapper;
        this.roomMapper = roomMapper;
    }

    /**
     * 房间 × 日期矩阵。
     *
     * @return 房间行 + 每行 dayStates（日期 → 状态），前端按五态色板渲染
     */
    public Map<String, Object> calendar(LocalDate from, LocalDate to, String floor, Long roomTypeId) {
        if (from == null) {
            from = LocalDate.now();
        }
        if (to == null) {
            to = from.plusDays(13);
        }
        if (to.isBefore(from) || from.plusDays(62).isBefore(to)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "档期查询范围需在 62 天以内");
        }
        List<Room> rooms = roomMapper.selectList(Wrappers.<Room>lambdaQuery()
                .eq(floor != null && !floor.isBlank(), Room::getFloor, floor)
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo));
        List<RoomSchedule> rows = scheduleMapper.selectList(Wrappers.<RoomSchedule>lambdaQuery()
                .between(RoomSchedule::getBizDate, from, to));
        Map<Long, Map<String, String>> byRoom = new LinkedHashMap<>();
        for (RoomSchedule row : rows) {
            byRoom.computeIfAbsent(row.getRoomId(), key -> new TreeMap<>())
                    .put(row.getBizDate().toString(), row.getState());
        }
        List<Map<String, Object>> items = new ArrayList<>(rooms.size());
        for (Room room : rooms) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("roomId", room.getId());
            item.put("roomNo", room.getRoomNo());
            item.put("floor", room.getFloor());
            item.put("roomTypeId", room.getRoomTypeId());
            item.put("roomStatus", room.getStatus());
            item.put("dayStates", byRoom.getOrDefault(room.getId(), Map.of()));
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("days", buildDays(from, to));
        result.put("rooms", items);
        return result;
    }

    /**
     * 销售预留：按房间与入住区间锁定档期。
     *
     * @param dates 需要锁定的日期（通常为 [入住, 出馆) 的每一天）
     */
    @Transactional
    public void lock(Long roomId, Long orderId, List<LocalDate> dates, Integer holdMinutes) {
        if (dates == null || dates.isEmpty()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "锁定档期至少需要一天");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
        }
        if ("MAINTENANCE".equals(room.getStatus())) {
            throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT, "房间维修中，不可锁定");
        }
        Integer realHold = holdMinutes == null || holdMinutes <= 0 ? DEFAULT_HOLD_MINUTES : holdMinutes;
        LocalDateTime expire = LocalDateTime.now().plusMinutes(realHold);
        for (LocalDate date : dates) {
            RoomSchedule schedule = new RoomSchedule();
            schedule.setTenantId(TenantContext.requireTenantId());
            schedule.setRoomId(roomId);
            schedule.setBizDate(date);
            schedule.setState("LOCKED");
            schedule.setOrderId(orderId);
            schedule.setHoldExpireTime(expire);
            try {
                scheduleMapper.insert(schedule);
            } catch (DuplicateKeyException e) {
                throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT,
                        "房间 " + room.getRoomNo() + " 在 " + date + " 已被占用，请重新选择日期或房间");
            }
        }
        room.setStatus("LOCKED");
        roomMapper.updateById(room);
        log.info("档期锁定成功 roomId={} orderId={} days={}", roomId, orderId, dates.size());
    }

    /** 支付成功后确认锁：置为已入住并清除预留过期时间。 */
    @Transactional
    public void confirm(Long orderId) {
        List<RoomSchedule> rows = scheduleMapper.selectList(Wrappers.<RoomSchedule>lambdaQuery()
                .eq(RoomSchedule::getOrderId, orderId));
        for (RoomSchedule row : rows) {
            row.setState("OCCUPIED");
            row.setHoldExpireTime(null);
            scheduleMapper.updateById(row);
            Room room = roomMapper.selectById(row.getRoomId());
            if (room != null) {
                room.setStatus("OCCUPIED");
                roomMapper.updateById(room);
            }
        }
    }

    /** 释放订单占用的全部档期。 */
    @Transactional
    public int release(Long orderId) {
        List<RoomSchedule> rows = scheduleMapper.selectList(Wrappers.<RoomSchedule>lambdaQuery()
                .eq(RoomSchedule::getOrderId, orderId));
        if (rows.isEmpty()) {
            return 0;
        }
        List<Long> roomIds = rows.stream().map(RoomSchedule::getRoomId).distinct().toList();
        scheduleMapper.delete(Wrappers.<RoomSchedule>lambdaQuery().eq(RoomSchedule::getOrderId, orderId));
        for (Long roomId : roomIds) {
            long occupied = scheduleMapper.selectCount(Wrappers.<RoomSchedule>lambdaQuery()
                    .eq(RoomSchedule::getRoomId, roomId)
                    .in(RoomSchedule::getState, List.of("LOCKED", "OCCUPIED")));
            Room room = roomMapper.selectById(roomId);
            if (room != null && occupied == 0 && !"MAINTENANCE".equals(room.getStatus())) {
                room.setStatus("AVAILABLE");
                roomMapper.updateById(room);
            }
        }
        return rows.size();
    }

    /** 延期：在原区间尾部追加日期。 */
    @Transactional
    public void extend(Long orderId, Long roomId, List<LocalDate> extraDates) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
        }
        Integer hold = DEFAULT_HOLD_MINUTES;
        for (LocalDate date : extraDates) {
            RoomSchedule schedule = new RoomSchedule();
            schedule.setTenantId(TenantContext.requireTenantId());
            schedule.setRoomId(roomId);
            schedule.setBizDate(date);
            schedule.setState("OCCUPIED");
            schedule.setOrderId(orderId);
            schedule.setRemark("延期追加");
            try {
                scheduleMapper.insert(schedule);
            } catch (DuplicateKeyException e) {
                throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT,
                        "房间 " + room.getRoomNo() + " 在 " + date + " 已有其他占用，无法延期");
            }
        }
        log.info("档期延期 orderId={} roomId={} extra={} hold={}", orderId, roomId, extraDates.size(), hold);
    }

    /** 换房：释放原房档期，锁定新房档期。 */
    @Transactional
    public void move(Long orderId, Long oldRoomId, Long newRoomId, List<LocalDate> dates) {
        scheduleMapper.delete(Wrappers.<RoomSchedule>lambdaQuery()
                .eq(RoomSchedule::getOrderId, orderId)
                .eq(RoomSchedule::getRoomId, oldRoomId));
        lock(newRoomId, orderId, dates, DEFAULT_HOLD_MINUTES);
        confirm(orderId);
    }

    /**
     * 释放超时未支付的销售预留（定时任务调用，建议每分钟一次）。
     *
     * @return 释放的档期条数
     */
    @Transactional
    public int releaseExpiredHolds() {
        List<RoomSchedule> expired = scheduleMapper.selectList(Wrappers.<RoomSchedule>lambdaQuery()
                .eq(RoomSchedule::getState, "LOCKED")
                .isNotNull(RoomSchedule::getHoldExpireTime)
                .lt(RoomSchedule::getHoldExpireTime, LocalDateTime.now()));
        for (RoomSchedule row : expired) {
            scheduleMapper.deleteById(row.getId());
            if (row.getOrderId() != null) {
                log.info("销售预留超时释放 orderId={} roomId={} date={}",
                        row.getOrderId(), row.getRoomId(), row.getBizDate());
            }
        }
        return expired.size();
    }

    /** 某房间某区间的可售判断，供下单前置校验。 */
    public boolean available(Long roomId, LocalDate checkin, LocalDate checkout) {
        List<LocalDate> dates = datesBetween(checkin, checkout);
        if (dates.isEmpty()) {
            return true;
        }
        Long occupied = scheduleMapper.selectCount(Wrappers.<RoomSchedule>lambdaQuery()
                .eq(RoomSchedule::getRoomId, roomId)
                .in(RoomSchedule::getBizDate, dates)
                .in(RoomSchedule::getState, List.of("LOCKED", "OCCUPIED", "MAINTENANCE")));
        return occupied == 0;
    }

    /** 入住区间覆盖的日期（含入住日，不含出馆日）。 */
    public static List<LocalDate> datesBetween(LocalDate checkin, LocalDate checkout) {
        List<LocalDate> dates = new ArrayList<>();
        if (checkin == null || checkout == null) {
            return dates;
        }
        for (LocalDate d = checkin; d.isBefore(checkout); d = d.plusDays(1)) {
            dates.add(d);
        }
        return dates;
    }

    private List<Map<String, String>> buildDays(LocalDate from, LocalDate to) {
        List<Map<String, String>> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Map<String, String> day = new LinkedHashMap<>();
            day.put("date", d.toString());
            day.put("day", String.valueOf(d.getDayOfMonth()));
            day.put("week", switch (d.getDayOfWeek()) {
                case MONDAY -> "一";
                case TUESDAY -> "二";
                case WEDNESDAY -> "三";
                case THURSDAY -> "四";
                case FRIDAY -> "五";
                case SATURDAY -> "六";
                default -> "日";
            });
            day.put("weekend", String.valueOf(d.getDayOfWeek().getValue() >= 6));
            days.add(day);
        }
        return days;
    }
}
