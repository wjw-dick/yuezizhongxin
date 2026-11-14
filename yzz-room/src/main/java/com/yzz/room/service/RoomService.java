package com.yzz.room.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.sensitive.SensitiveWordFilter;
import com.yzz.common.tenant.TenantContext;
import com.yzz.room.entity.Room;
import com.yzz.room.entity.RoomType;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.room.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 房型与房间台账（T30）。
 *
 * <p>房态五态：可预订 / 已锁定 / 已入住 / 待清洁 / 维修中。
 * 房间编号在同一租户内唯一，删除做逻辑删除，历史档期保留。</p>
 */
@Service
public class RoomService {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    public RoomService(RoomTypeMapper roomTypeMapper, RoomMapper roomMapper,
                       SensitiveWordFilter sensitiveWordFilter) {
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    public List<RoomType> roomTypes() {
        return roomTypeMapper.selectList(Wrappers.<RoomType>lambdaQuery()
                .orderByAsc(RoomType::getSort).orderByAsc(RoomType::getId));
    }

    @Transactional
    public Long saveRoomType(RoomType input) {
        validateRoomType(input);
        sensitiveWordFilter.assertClean(input.getName() + " " + (input.getDescription() == null ? "" : input.getDescription()));
        Long tenantId = TenantContext.requireTenantId();
        input.setTenantId(tenantId);
        if (input.getId() == null) {
            if (input.getStatus() == null) {
                input.setStatus("ENABLED");
            }
            roomTypeMapper.insert(input);
        } else {
            RoomType exist = requireRoomType(input.getId());
            input.setTenantId(exist.getTenantId());
            roomTypeMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void deleteRoomType(Long id) {
        requireRoomType(id);
        Long rooms = roomMapper.selectCount(Wrappers.<Room>lambdaQuery().eq(Room::getRoomTypeId, id));
        if (rooms > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该房型下仍有 " + rooms + " 个房间，请先调整房间归属");
        }
        roomTypeMapper.deleteById(id);
    }

    public List<Room> rooms(String floor, Long roomTypeId, String status) {
        return roomMapper.selectList(Wrappers.<Room>lambdaQuery()
                .eq(floor != null && !floor.isBlank(), Room::getFloor, floor)
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .eq(status != null && !status.isBlank(), Room::getStatus, status)
                .orderByAsc(Room::getFloor).orderByAsc(Room::getRoomNo));
    }

    @Transactional
    public Long saveRoom(Room input) {
        if (input.getRoomNo() == null || input.getRoomNo().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "房间编号不能为空");
        }
        if (input.getRoomTypeId() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请选择房型");
        }
        requireRoomType(input.getRoomTypeId());
        Long tenantId = TenantContext.requireTenantId();
        input.setTenantId(tenantId);
        if (input.getStatus() == null || input.getStatus().isBlank()) {
            input.setStatus("AVAILABLE");
        }
        if (input.getId() == null) {
            Long dup = roomMapper.selectCount(Wrappers.<Room>lambdaQuery()
                    .eq(Room::getRoomNo, input.getRoomNo()));
            if (dup > 0) {
                throw BusinessException.of(ErrorCode.CONFLICT, "房间编号已存在：" + input.getRoomNo());
            }
            roomMapper.insert(input);
        } else {
            Room exist = requireRoom(input.getId());
            input.setTenantId(exist.getTenantId());
            roomMapper.updateById(input);
        }
        return input.getId();
    }

    @Transactional
    public void deleteRoom(Long id) {
        requireRoom(id);
        roomMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long roomId, String status) {
        Room room = requireRoom(roomId);
        List<String> allowed = List.of("AVAILABLE", "LOCKED", "OCCUPIED", "CLEANING", "MAINTENANCE");
        if (!allowed.contains(status)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "非法房态：" + status);
        }
        room.setStatus(status);
        roomMapper.updateById(room);
    }

    public RoomType requireRoomType(Long id) {
        RoomType type = roomTypeMapper.selectById(id);
        if (type == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房型不存在");
        }
        return type;
    }

    public Room requireRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
        }
        return room;
    }

    private void validateRoomType(RoomType input) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "房型名称不能为空");
        }
        if (input.getMaxGuest() == null || input.getMaxGuest() < 1) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "可住人数至少为 1");
        }
    }
}
