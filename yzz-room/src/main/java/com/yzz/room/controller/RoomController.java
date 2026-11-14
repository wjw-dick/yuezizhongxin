package com.yzz.room.controller;

import com.yzz.common.api.PageResult;
import com.yzz.common.api.R;
import com.yzz.common.audit.AuditLog;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.room.entity.Room;
import com.yzz.room.entity.RoomTask;
import com.yzz.room.entity.RoomType;
import com.yzz.room.service.RoomService;
import com.yzz.room.service.RoomTaskService;
import com.yzz.room.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 房型、房间台账、档期日历与房间任务（T30–T32）。
 */
@Tag(name = "租户-房型与档期")
@RestController
@RequestMapping("/api/tenant/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ScheduleService scheduleService;
    private final RoomTaskService roomTaskService;

    public RoomController(RoomService roomService, ScheduleService scheduleService,
                          RoomTaskService roomTaskService) {
        this.roomService = roomService;
        this.scheduleService = scheduleService;
        this.roomTaskService = roomTaskService;
    }

    @Operation(summary = "房型列表")
    @GetMapping("/types")
    public R<List<RoomType>> roomTypes() {
        AuthGuard.requireTenant();
        return R.ok(roomService.roomTypes());
    }

    @Operation(summary = "保存房型（新增或修改）")
    @AuditLog(module = "房型管理", action = "保存房型")
    @PostMapping("/types")
    public R<Long> saveRoomType(@RequestBody RoomType request) {
        AuthGuard.requireTenant();
        return R.ok(roomService.saveRoomType(request));
    }

    @Operation(summary = "删除房型")
    @AuditLog(module = "房型管理", action = "删除房型")
    @DeleteMapping("/types/{id}")
    public R<Void> deleteRoomType(@PathVariable Long id) {
        AuthGuard.requireTenant();
        roomService.deleteRoomType(id);
        return R.ok();
    }

    @Operation(summary = "房间列表")
    @GetMapping
    public R<PageResult<Room>> rooms(@RequestParam(required = false) String floor,
                                     @RequestParam(required = false) Long roomTypeId,
                                     @RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        List<Room> rows = roomService.rooms(floor, roomTypeId, status);
        return R.ok(PageResult.of(rows, rows.size(), 1, rows.size()));
    }

    @Operation(summary = "保存房间")
    @AuditLog(module = "房间台账", action = "保存房间")
    @PostMapping
    public R<Long> saveRoom(@RequestBody Room request) {
        AuthGuard.requireTenant();
        return R.ok(roomService.saveRoom(request));
    }

    @Operation(summary = "删除房间")
    @AuditLog(module = "房间台账", action = "删除房间")
    @DeleteMapping("/{id}")
    public R<Void> deleteRoom(@PathVariable Long id) {
        AuthGuard.requireTenant();
        roomService.deleteRoom(id);
        return R.ok();
    }

    @Operation(summary = "调整房态")
    @AuditLog(module = "房间台账", action = "调整房态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AuthGuard.requireTenant();
        roomService.updateStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "档期日历（房间 × 日期矩阵）")
    @GetMapping("/calendar")
    public R<Map<String, Object>> calendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) Long roomTypeId) {
        AuthGuard.requireTenant();
        return R.ok(scheduleService.calendar(from, to, floor, roomTypeId));
    }

    @Operation(summary = "档期可售校验")
    @GetMapping("/available")
    public R<Boolean> available(@RequestParam Long roomId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkin,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkout) {
        AuthGuard.requireTenant();
        return R.ok(scheduleService.available(roomId, checkin, checkout));
    }

    @Operation(summary = "手动锁定档期（销售预留）")
    @AuditLog(module = "档期管理", action = "锁定档期")
    @PostMapping("/schedules/lock")
    public R<Void> lock(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        Long roomId = Long.valueOf(String.valueOf(body.get("roomId")));
        Long orderId = body.get("orderId") == null ? null : Long.valueOf(String.valueOf(body.get("orderId")));
        @SuppressWarnings("unchecked")
        List<String> raw = (List<String>) body.getOrDefault("dates", List.of());
        List<LocalDate> dates = raw.stream().map(LocalDate::parse).toList();
        Integer holdMinutes = body.get("holdMinutes") == null ? null
                : Integer.valueOf(String.valueOf(body.get("holdMinutes")));
        scheduleService.lock(roomId, orderId, dates, holdMinutes);
        return R.ok();
    }

    @Operation(summary = "释放订单档期")
    @AuditLog(module = "档期管理", action = "释放档期")
    @PostMapping("/schedules/{orderId}/release")
    public R<Integer> release(@PathVariable Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(scheduleService.release(orderId));
    }

    @Operation(summary = "房间任务列表")
    @GetMapping("/tasks")
    public R<List<RoomTask>> tasks(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String taskType) {
        AuthGuard.requireTenant();
        return R.ok(roomTaskService.list(status, taskType));
    }

    @Operation(summary = "新建房间任务")
    @AuditLog(module = "房态管理", action = "新建房间任务")
    @PostMapping("/tasks")
    public R<Long> createTask(@RequestBody RoomTask request) {
        AuthGuard.requireTenant();
        return R.ok(roomTaskService.create(request));
    }

    @Operation(summary = "完成房间任务")
    @AuditLog(module = "房态管理", action = "完成房间任务")
    @PutMapping("/tasks/{id}/finish")
    public R<Void> finishTask(@PathVariable Long id) {
        AuthGuard.requireTenant();
        roomTaskService.finish(id);
        return R.ok();
    }
}
