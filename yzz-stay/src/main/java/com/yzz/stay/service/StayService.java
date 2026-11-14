package com.yzz.stay.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.care.entity.CarePlan;
import com.yzz.care.mapper.CarePlanMapper;
import com.yzz.care.service.CareService;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.DesensitizeUtil;
import com.yzz.common.util.SecretCipher;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.service.OrderService;
import com.yzz.order.service.PaymentService;
import com.yzz.room.entity.Room;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.room.service.RoomTaskService;
import com.yzz.stay.entity.StayCheckin;
import com.yzz.stay.entity.StayCheckout;
import com.yzz.stay.mapper.StayCheckinMapper;
import com.yzz.stay.mapper.StayCheckoutMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 入馆与出馆（T38、T39）。
 *
 * <p>入馆：健康告知必须签署，证件号加密入库，房态转已入住，按分娩方式生成护理计划。
 * 出馆：结算尾款与增值消费、核对押金与物品、写小结与满意度、预约 42 天复查，
 * 房态转待清洁并生成清洁任务。</p>
 */
@Service
public class StayService {

    private final StayCheckinMapper checkinMapper;
    private final StayCheckoutMapper checkoutMapper;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RoomMapper roomMapper;
    private final RoomTaskService roomTaskService;
    private final CareService careService;
    private final CarePlanMapper carePlanMapper;
    private final SecretCipher secretCipher;

    public StayService(StayCheckinMapper checkinMapper, StayCheckoutMapper checkoutMapper,
                       OrderService orderService, PaymentService paymentService,
                       RoomMapper roomMapper, RoomTaskService roomTaskService,
                       CareService careService, CarePlanMapper carePlanMapper,
                       SecretCipher secretCipher) {
        this.checkinMapper = checkinMapper;
        this.checkoutMapper = checkoutMapper;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.roomMapper = roomMapper;
        this.roomTaskService = roomTaskService;
        this.careService = careService;
        this.carePlanMapper = carePlanMapper;
        this.secretCipher = secretCipher;
    }

    @Transactional
    public Long checkin(StayCheckin input, int careDays) {
        BizOrder order = orderService.requireOrder(input.getOrderId());
        if (!List.of("待入住", "待支付").contains(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "订单状态不可入馆：" + order.getStatus());
        }
        if (input.getHealthNoticeSigned() == null || input.getHealthNoticeSigned() != 1) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "健康告知与知情同意书必须签署后才能入馆");
        }
        if (input.getMotherName() == null || input.getMotherName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "产妇姓名不能为空");
        }
        Long roomId = input.getRoomId() == null ? order.getRoomId() : input.getRoomId();
        if (roomId == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请先为订单分配房间");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
        }
        if (input.getIdCardNo() != null && !input.getIdCardNo().isBlank()) {
            input.setIdCardNo(secretCipher.encrypt(input.getIdCardNo()));
        }
        input.setTenantId(TenantContext.requireTenantId());
        input.setRoomId(roomId);
        input.setStatus("在住");
        if (input.getCheckinTime() == null) {
            input.setCheckinTime(LocalDateTime.now());
        }
        if (input.getDepositAmount() == null) {
            input.setDepositAmount(order.getBondAmount());
        }
        checkinMapper.insert(input);

        room.setStatus("OCCUPIED");
        roomMapper.updateById(room);
        order.setRoomId(roomId);
        orderService.markCheckin(order.getId());

        careService.createPlan(buildPlan(order, input, roomId), order.getCheckinDate(),
                careDays <= 0 ? 28 : careDays, input.getDeliveryType());
        return input.getId();
    }

    public Map<String, Object> detail(Long orderId) {
        StayCheckin checkin = checkinMapper.selectOne(Wrappers.<StayCheckin>lambdaQuery()
                .eq(StayCheckin::getOrderId, orderId).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkin", checkin == null ? null : mask(checkin));
        result.put("checkout", checkoutMapper.selectOne(Wrappers.<StayCheckout>lambdaQuery()
                .eq(StayCheckout::getOrderId, orderId).last("limit 1")));
        result.put("order", orderService.detail(orderId));
        return result;
    }

    public List<StayCheckin> checkins(String status) {
        List<StayCheckin> rows = checkinMapper.selectList(Wrappers.<StayCheckin>lambdaQuery()
                .eq(status != null && !status.isBlank(), StayCheckin::getStatus, status)
                .orderByDesc(StayCheckin::getId));
        rows.forEach(this::mask);
        return rows;
    }

    /**
     * 出馆结算。
     */
    @Transactional
    public Long checkout(StayCheckout input) {
        StayCheckin checkin = checkinMapper.selectById(input.getCheckinId());
        if (checkin == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "入馆登记不存在");
        }
        if (!"在住".equals(checkin.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "该客户已出馆");
        }
        BizOrder order = orderService.requireOrder(input.getOrderId());
        if (input.getItemReturned() == null) {
            input.setItemReturned(0);
        }
        if (input.getDepositRefund() == null) {
            input.setDepositRefund(checkin.getDepositAmount());
        }
        if (input.getDepositRefund().compareTo(checkin.getDepositAmount()) > 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "退还押金不能超过收取金额");
        }
        if (input.getSatisfaction() != null && (input.getSatisfaction() < 1 || input.getSatisfaction() > 5)) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "满意度评分需在 1~5 之间");
        }
        input.setTenantId(TenantContext.requireTenantId());
        input.setOperatorId(TenantContext.getUserId());
        if (input.getCheckoutTime() == null) {
            input.setCheckoutTime(LocalDateTime.now());
        }
        if (input.getReviewAppointment() == null) {
            input.setReviewAppointment(LocalDate.now().plusDays(42));
        }
        BigDecimal tail = order.getTotalAmount().subtract(order.getPaidAmount()).max(BigDecimal.ZERO);
        if (input.getTailAmount() == null) {
            input.setTailAmount(tail);
        }
        checkoutMapper.insert(input);

        checkin.setStatus("已出馆");
        checkinMapper.updateById(checkin);
        orderService.markCheckout(order.getId());

        Room room = roomMapper.selectById(checkin.getRoomId());
        if (room != null) {
            roomTaskService.createCleaningForCheckout(room.getId(), room.getRoomNo());
        }
        List<CarePlan> plans = carePlanMapper.selectList(Wrappers.<CarePlan>lambdaQuery()
                .eq(CarePlan::getOrderId, order.getId()));
        for (CarePlan plan : plans) {
            plan.setStatus("已结束");
            carePlanMapper.updateById(plan);
        }
        return input.getId();
    }

    /** 出馆结算收款并完成订单。 */
    @Transactional
    public Map<String, Object> settle(Long orderId, String method, BigDecimal amount) {
        BizOrder order = orderService.requireOrder(orderId);
        BigDecimal due = amount == null
                ? order.getTotalAmount().subtract(order.getPaidAmount()).max(BigDecimal.ZERO) : amount;
        Map<String, Object> payResult = paymentService.pay(orderId,
                new com.yzz.order.dto.OrderPayRequest("尾款", method, due, "出馆结算"));
        if (!"微信支付".equals(method)) {
            orderService.complete(orderId);
        }
        return payResult;
    }

    private CarePlan buildPlan(BizOrder order, StayCheckin checkin, Long roomId) {
        CarePlan plan = new CarePlan();
        plan.setOrderId(order.getId());
        plan.setCustomerName(checkin.getMotherName());
        plan.setRoomId(roomId);
        plan.setCareMode(order.getCareMode());
        return plan;
    }

    private StayCheckin mask(StayCheckin checkin) {
        if (checkin.getIdCardNo() != null) {
            try {
                checkin.setIdCardNo(DesensitizeUtil.idCard(secretCipher.decrypt(checkin.getIdCardNo())));
            } catch (Exception e) {
                checkin.setIdCardNo("******");
            }
        }
        return checkin;
    }
}
