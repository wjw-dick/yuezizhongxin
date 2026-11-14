package com.yzz.room.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.room.entity.Room;
import com.yzz.room.entity.RoomTask;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.room.mapper.RoomTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 房间清洁与维修任务（T30）。
 *
 * <p>出馆后自动生成清洁任务并把房态置为「待清洁」；清洁完成房态回到「可预订」。
 * 维修任务创建即把房态置为「维修中」，完成或取消后释放。</p>
 */
@Service
public class RoomTaskService {

    private final RoomTaskMapper taskMapper;
    private final RoomMapper roomMapper;

    public RoomTaskService(RoomTaskMapper taskMapper, RoomMapper roomMapper) {
        this.taskMapper = taskMapper;
        this.roomMapper = roomMapper;
    }

    public List<RoomTask> list(String status, String taskType) {
        return taskMapper.selectList(Wrappers.<RoomTask>lambdaQuery()
                .eq(status != null && !status.isBlank(), RoomTask::getStatus, status)
                .eq(taskType != null && !taskType.isBlank(), RoomTask::getTaskType, taskType)
                .orderByAsc(RoomTask::getStatus).orderByDesc(RoomTask::getId));
    }

    @Transactional
    public Long create(RoomTask input) {
        Room room = roomMapper.selectById(input.getRoomId());
        if (room == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
        }
        if (input.getTaskType() == null) {
            input.setTaskType("清洁");
        }
        if (input.getStatus() == null) {
            input.setStatus("待处理");
        }
        input.setTenantId(TenantContext.requireTenantId());
        taskMapper.insert(input);
        if ("维修".equals(input.getTaskType())) {
            room.setStatus("MAINTENANCE");
            roomMapper.updateById(room);
        } else if ("AVAILABLE".equals(room.getStatus())) {
            room.setStatus("CLEANING");
            roomMapper.updateById(room);
        }
        return input.getId();
    }

    @Transactional
    public void finish(Long id) {
        RoomTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if ("已完成".equals(task.getStatus())) {
            return;
        }
        task.setStatus("已完成");
        task.setFinishedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        Room room = roomMapper.selectById(task.getRoomId());
        if (room == null) {
            return;
        }
        long pending = taskMapper.selectCount(Wrappers.<RoomTask>lambdaQuery()
                .eq(RoomTask::getRoomId, task.getRoomId())
                .ne(RoomTask::getStatus, "已完成"));
        if (pending == 0) {
            room.setStatus("AVAILABLE");
            roomMapper.updateById(room);
        }
    }

    /** 出馆联动：门房生成清洁任务并把房态转为待清洁。 */
    @Transactional
    public void createCleaningForCheckout(Long roomId, String roomNo) {
        RoomTask task = new RoomTask();
        task.setTenantId(TenantContext.requireTenantId());
        task.setRoomId(roomId);
        task.setTaskType("清洁");
        task.setStatus("待处理");
        task.setRemark("客户出馆，房间 " + roomNo + " 待清洁与物品补充");
        task.setDueTime(LocalDateTime.now().plusHours(2));
        taskMapper.insert(task);
        Room room = roomMapper.selectById(roomId);
        if (room != null) {
            room.setStatus("CLEANING");
            roomMapper.updateById(room);
        }
    }
}
