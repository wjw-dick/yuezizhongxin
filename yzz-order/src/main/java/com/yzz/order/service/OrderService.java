package com.yzz.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.PageResult;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.SecretCipher;
import com.yzz.order.dto.OrderChangeRequest;
import com.yzz.order.dto.OrderCreateRequest;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.entity.BizOrderChange;
import com.yzz.order.entity.BizOrderContract;
import com.yzz.order.entity.BizOrderItem;
import com.yzz.order.entity.BizPayment;
import com.yzz.order.entity.BizRefund;
import com.yzz.order.mapper.BizOrderChangeMapper;
import com.yzz.order.mapper.BizOrderContractMapper;
import com.yzz.order.mapper.BizOrderItemMapper;
import com.yzz.order.mapper.BizOrderMapper;
import com.yzz.order.mapper.BizPaymentMapper;
import com.yzz.order.mapper.BizRefundMapper;
import com.yzz.product.entity.PkgPackage;
import com.yzz.product.service.PackageService;
import com.yzz.room.entity.Room;
import com.yzz.room.mapper.RoomMapper;
import com.yzz.room.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单主流程（T36）。
 *
 * <p>状态机：待支付 → 待入住 → 在住 → 待结算 → 已完成，异常分支为已取消/已退订。
 * 定金支付成功才确认锁档；取消与退订必须释放档期，避免「僵尸房」。</p>
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BizOrderMapper orderMapper;
    private final BizOrderItemMapper itemMapper;
    private final BizPaymentMapper paymentMapper;
    private final BizRefundMapper refundMapper;
    private final BizOrderChangeMapper changeMapper;
    private final BizOrderContractMapper contractMapper;
    private final PackageService packageService;
    private final ScheduleService scheduleService;
    private final RoomMapper roomMapper;
    private final CacheStore cacheStore;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;

    public OrderService(BizOrderMapper orderMapper, BizOrderItemMapper itemMapper,
                        BizPaymentMapper paymentMapper, BizRefundMapper refundMapper,
                        BizOrderChangeMapper changeMapper, BizOrderContractMapper contractMapper,
                        PackageService packageService, ScheduleService scheduleService,
                        RoomMapper roomMapper, CacheStore cacheStore, SecretCipher secretCipher,
                        ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.paymentMapper = paymentMapper;
        this.refundMapper = refundMapper;
        this.changeMapper = changeMapper;
        this.contractMapper = contractMapper;
        this.packageService = packageService;
        this.scheduleService = scheduleService;
        this.roomMapper = roomMapper;
        this.cacheStore = cacheStore;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> create(OrderCreateRequest request) {
        validate(request);
        PkgPackage pkg = packageService.detail(request.packageId());
        if (!"ON_SHELF".equals(pkg.getStatus())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "套餐已下架，请重新选择");
        }
        Room room = resolveRoom(request, pkg);

        BigDecimal packageAmount = pkg.getPrice();
        BigDecimal extraAmount = sum(request.extras());
        BigDecimal productAmount = sum(request.products());
        BigDecimal discount = request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount();
        BigDecimal total = packageAmount.add(extraAmount).add(productAmount).subtract(discount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposit = packageService.depositOf(pkg).min(total);
        BigDecimal bond = request.bondAmount() == null ? BigDecimal.ZERO : request.bondAmount();

        BizOrder order = new BizOrder();
        order.setTenantId(TenantContext.requireTenantId());
        order.setOrderNo(nextOrderNo());
        order.setLeadId(request.leadId());
        order.setCustomerName(request.customerName());
        order.setPhone(secretCipher.encrypt(request.phone()));
        order.setPackageId(pkg.getId());
        order.setRoomTypeId(request.roomTypeId() == null ? (room == null ? null : room.getRoomTypeId())
                : request.roomTypeId());
        order.setRoomId(room == null ? null : room.getId());
        order.setCheckinDate(request.checkinDate());
        order.setCheckoutDate(request.checkoutDate());
        order.setCareMode(request.careMode() == null ? pkg.getCareMode() : request.careMode());
        order.setCompanionCount(request.companionCount() == null ? 1 : request.companionCount());
        order.setPackageAmount(packageAmount);
        order.setExtraAmount(extraAmount);
        order.setProductAmount(productAmount);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setDepositAmount(deposit);
        order.setTailAmount(total.subtract(deposit).max(BigDecimal.ZERO));
        order.setBondAmount(bond);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setStatus("待支付");
        order.setSource(request.source() == null ? "到店签约" : request.source());
        order.setRemark(request.remark());
        order.setCreatedBy(TenantContext.getUserId());
        orderMapper.insert(order);

        insertItem(order, "套餐", pkg.getName(), 1, packageAmount);
        insertExtras(order, request.extras(), "增值服务");
        insertExtras(order, request.products(), "商品");

        if (room != null) {
            scheduleService.lock(room.getId(), order.getId(),
                    ScheduleService.datesBetween(request.checkinDate(), request.checkoutDate()), 60);
        }
        log.info("订单创建成功 tenantId={} orderNo={} total={} deposit={}",
                order.getTenantId(), order.getOrderNo(), total, deposit);
        return detail(order.getId());
    }

    public PageResult<BizOrder> list(String status, String keyword, LocalDate from, LocalDate to,
                                     int pageNum, int pageSize) {
        Page<BizOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<BizOrder>lambdaQuery()
                        .eq(status != null && !status.isBlank(), BizOrder::getStatus, status)
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(BizOrder::getOrderNo, keyword)
                                .or().like(BizOrder::getCustomerName, keyword))
                        .ge(from != null, BizOrder::getCheckinDate, from)
                        .le(to != null, BizOrder::getCheckinDate, to)
                        .orderByDesc(BizOrder::getId));
        for (BizOrder row : page.getRecords()) {
            row.setPhone(maskPhone(row.getPhone()));
        }
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    public Map<String, Object> detail(Long id) {
        BizOrder order = requireOrder(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", itemMapper.selectList(Wrappers.<BizOrderItem>lambdaQuery()
                .eq(BizOrderItem::getOrderId, id)));
        result.put("payments", paymentMapper.selectList(Wrappers.<BizPayment>lambdaQuery()
                .eq(BizPayment::getOrderId, id).orderByDesc(BizPayment::getId)));
        result.put("refunds", refundMapper.selectList(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getOrderId, id).orderByDesc(BizRefund::getId)));
        result.put("changes", changeMapper.selectList(Wrappers.<BizOrderChange>lambdaQuery()
                .eq(BizOrderChange::getOrderId, id).orderByDesc(BizOrderChange::getId)));
        result.put("contracts", contractMapper.selectList(Wrappers.<BizOrderContract>lambdaQuery()
                .eq(BizOrderContract::getOrderId, id)));
        return result;
    }

    public BizOrder requireOrder(Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 收款成功后的状态推进（支付回调与线下收款共用）。
     */
    @Transactional
    public void markPaid(Long orderId, BigDecimal amount, String payType) {
        BizOrder order = requireOrder(orderId);
        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        order.setPaidAmount(paid.add(amount).setScale(2, RoundingMode.HALF_UP));
        if ("定金".equals(payType) && "待支付".equals(order.getStatus())) {
            order.setStatus("待入住");
            scheduleService.confirm(orderId);
        }
        if ("尾款".equals(payType) && "待结算".equals(order.getStatus())) {
            order.setStatus("已完成");
        }
        orderMapper.updateById(order);
    }

    /** 入馆：订单转在住。 */
    @Transactional
    public void markCheckin(Long orderId) {
        BizOrder order = requireOrder(orderId);
        if (!List.of("待入住", "待支付").contains(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "当前订单状态不可办理入馆：" + order.getStatus());
        }
        order.setStatus("在住");
        orderMapper.updateById(order);
    }

    /** 出馆：进入待结算，结算完成转已完成。 */
    @Transactional
    public void markCheckout(Long orderId) {
        BizOrder order = requireOrder(orderId);
        order.setStatus("待结算");
        orderMapper.updateById(order);
    }

    @Transactional
    public void complete(Long orderId) {
        BizOrder order = requireOrder(orderId);
        order.setStatus("已完成");
        orderMapper.updateById(order);
    }

    @Transactional
    public void markRefunded(Long orderId) {
        BizOrder order = requireOrder(orderId);
        order.setStatus("已退订");
        orderMapper.updateById(order);
        scheduleService.release(orderId);
    }

    /** 待支付订单取消：释放档期。 */
    @Transactional
    public void cancel(Long orderId, String reason) {
        BizOrder order = requireOrder(orderId);
        if (!"待支付".equals(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "仅待支付订单可直接取消，已收款订单请走退订流程");
        }
        order.setStatus("已取消");
        order.setRemark(reason == null ? order.getRemark() : reason);
        orderMapper.updateById(order);
        scheduleService.release(orderId);
        recordChange(order, "取消", "{}", "{\"reason\":\"" + (reason == null ? "" : reason) + "\"}",
                BigDecimal.ZERO, reason);
    }

    /**
     * 订单变更：换房、延期、改套餐。
     */
    @Transactional
    public Map<String, Object> change(Long orderId, OrderChangeRequest request) {
        BizOrder order = requireOrder(orderId);
        if (List.of("已取消", "已退订", "已完成").contains(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "当前状态不可变更：" + order.getStatus());
        }
        String before = snapshot(order);
        BigDecimal diff = BigDecimal.ZERO;
        List<LocalDate> dates = ScheduleService.datesBetween(order.getCheckinDate(), order.getCheckoutDate());

        if ("换房".equals(request.changeType())) {
            if (request.newRoomId() == null) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "请选择新房间");
            }
            Long oldRoomId = order.getRoomId();
            if (oldRoomId == null) {
                throw BusinessException.of(ErrorCode.CONFLICT, "订单尚未锁定房间，无法换房");
            }
            if (oldRoomId.equals(request.newRoomId())) {
                return detail(orderId);
            }
            scheduleService.move(orderId, oldRoomId, request.newRoomId(), dates);
            order.setRoomId(request.newRoomId());
            Room newRoom = roomMapper.selectById(request.newRoomId());
            if (newRoom != null) {
                order.setRoomTypeId(newRoom.getRoomTypeId());
            }
        } else if ("延期".equals(request.changeType())) {
            LocalDate newCheckout = request.newCheckoutDate();
            if (newCheckout == null || !newCheckout.isAfter(order.getCheckoutDate())) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "延期后的出馆日期必须晚于原出馆日期");
            }
            List<LocalDate> extra = ScheduleService.datesBetween(order.getCheckoutDate(), newCheckout);
            scheduleService.extend(orderId, order.getRoomId(), extra);
            int days = (int) (newCheckout.toEpochDay() - order.getCheckoutDate().toEpochDay());
            BigDecimal perDay = order.getPackageAmount()
                    .divide(BigDecimal.valueOf(Math.max(1, daysBetween(order))), 2, RoundingMode.HALF_UP);
            diff = perDay.multiply(BigDecimal.valueOf(days));
            order.setCheckoutDate(newCheckout);
            order.setPackageAmount(order.getPackageAmount().add(diff));
            order.setTotalAmount(order.getTotalAmount().add(diff));
            order.setTailAmount(order.getTailAmount().add(diff));
            insertItem(order, "套餐", "延期 " + days + " 天", days, perDay);
        } else if ("改套餐".equals(request.changeType())) {
            if (request.newPackageId() == null) {
                throw BusinessException.of(ErrorCode.BAD_REQUEST, "请选择新套餐");
            }
            PkgPackage pkg = packageService.detail(request.newPackageId());
            diff = pkg.getPrice().subtract(order.getPackageAmount());
            order.setPackageId(pkg.getId());
            order.setPackageAmount(pkg.getPrice());
            order.setTotalAmount(order.getTotalAmount().add(diff));
            order.setDepositAmount(packageService.depositOf(pkg));
            order.setTailAmount(order.getTotalAmount().subtract(order.getPaidAmount())
                    .max(BigDecimal.ZERO));
            insertItem(order, "套餐", pkg.getName() + "（改套餐）", 1, pkg.getPrice());
        } else {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "不支持的变更类型：" + request.changeType());
        }

        order.setRemark(request.reason() == null ? order.getRemark() : request.reason());
        orderMapper.updateById(order);
        recordChange(order, request.changeType(), before, snapshot(order), diff, request.reason());
        return detail(orderId);
    }

    @Transactional
    public void addContract(Long orderId, String fileKey, String fileName) {
        requireOrder(orderId);
        BizOrderContract contract = new BizOrderContract();
        contract.setTenantId(TenantContext.requireTenantId());
        contract.setOrderId(orderId);
        contract.setFileKey(fileKey);
        contract.setFileName(fileName);
        contract.setSignedTime(LocalDateTime.now());
        contractMapper.insert(contract);
    }

    public BizPayment createPaymentRecord(Long orderId, String payType, String method, BigDecimal amount) {
        BizPayment payment = new BizPayment();
        payment.setTenantId(TenantContext.requireTenantId());
        payment.setOrderId(orderId);
        payment.setPaymentNo("SK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", (int) (Math.random() * 1000)));
        payment.setPayType(payType);
        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus("待支付");
        payment.setOperatorId(TenantContext.getUserId());
        paymentMapper.insert(payment);
        return payment;
    }

    public BizPaymentMapper paymentMapper() {
        return paymentMapper;
    }

    public BizRefundMapper refundMapper() {
        return refundMapper;
    }

    private void insertItem(BizOrder order, String type, String name, Integer quantity, BigDecimal price) {
        BizOrderItem item = new BizOrderItem();
        item.setTenantId(order.getTenantId());
        item.setOrderId(order.getId());
        item.setItemType(type);
        item.setItemName(name);
        item.setQuantity(quantity == null ? 1 : quantity);
        item.setPrice(price);
        item.setAmount(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        itemMapper.insert(item);
    }

    private void insertExtras(BizOrder order, List<OrderCreateRequest.Item> items, String type) {
        if (items == null) {
            return;
        }
        for (OrderCreateRequest.Item row : items) {
            if (row.price() == null || row.itemName() == null) {
                continue;
            }
            insertItem(order, row.itemType() == null ? type : row.itemType(),
                    row.itemName(), row.quantity(), row.price());
        }
    }

    private BigDecimal sum(List<OrderCreateRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .filter(i -> i.price() != null)
                .map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity() == null ? 1 : i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Room resolveRoom(OrderCreateRequest request, PkgPackage pkg) {
        List<LocalDate> dates = ScheduleService.datesBetween(request.checkinDate(), request.checkoutDate());
        if (request.roomId() != null) {
            Room room = roomMapper.selectById(request.roomId());
            if (room == null) {
                throw BusinessException.of(ErrorCode.NOT_FOUND, "房间不存在");
            }
            if (!scheduleService.available(room.getId(), request.checkinDate(), request.checkoutDate())) {
                throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT,
                        "房间 " + room.getRoomNo() + " 在所选日期不可用");
            }
            return room;
        }
        if (request.roomTypeId() == null || dates.isEmpty()) {
            return null;
        }
        List<Room> candidates = roomMapper.selectList(Wrappers.<Room>lambdaQuery()
                .eq(Room::getRoomTypeId, request.roomTypeId())
                .eq(Room::getStatus, "AVAILABLE")
                .orderByAsc(Room::getRoomNo));
        for (Room room : candidates) {
            if (scheduleService.available(room.getId(), request.checkinDate(), request.checkoutDate())) {
                return room;
            }
        }
        throw BusinessException.of(ErrorCode.SCHEDULE_CONFLICT, "所选房型在入住区间内已订满，请更换日期或房型");
    }

    private void validate(OrderCreateRequest request) {
        if (request.customerName() == null || request.customerName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "客户姓名不能为空");
        }
        if (request.phone() == null || request.phone().length() < 6) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "联系电话不合法");
        }
        if (request.packageId() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请选择套餐");
        }
        if (request.checkinDate() == null || request.checkoutDate() == null
                || !request.checkoutDate().isAfter(request.checkinDate())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "入馆日期必须早于出馆日期");
        }
        if (request.checkinDate().isBefore(LocalDate.now().minusDays(1))) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "入馆日期不能早于昨天");
        }
    }

    private int daysBetween(BizOrder order) {
        return (int) (order.getCheckoutDate().toEpochDay() - order.getCheckinDate().toEpochDay());
    }

    private String nextOrderNo() {
        Long tenantId = TenantContext.requireTenantId();
        String day = LocalDate.now().format(DAY);
        long seq = cacheStore.increment("order:seq:" + tenantId + ":" + day, Duration.ofDays(2));
        return "YZ" + day + String.format("%06d", seq);
    }

    private void recordChange(BizOrder order, String type, String before, String after,
                              BigDecimal diff, String reason) {
        BizOrderChange change = new BizOrderChange();
        change.setTenantId(order.getTenantId());
        change.setOrderId(order.getId());
        change.setChangeType(type);
        change.setBeforeJson(before);
        change.setAfterJson(after);
        change.setAmountDiff(diff);
        change.setReason(reason);
        change.setOperatorId(TenantContext.getUserId());
        changeMapper.insert(change);
    }

    private String snapshot(BizOrder order) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "roomId", order.getRoomId() == null ? "" : order.getRoomId(),
                    "packageId", order.getPackageId(),
                    "checkin", order.getCheckinDate().toString(),
                    "checkout", order.getCheckoutDate().toString(),
                    "total", order.getTotalAmount()));
        } catch (Exception e) {
            return "{}";
        }
    }

    private String maskPhone(String encrypted) {
        try {
            String plain = secretCipher.decrypt(encrypted);
            return com.yzz.common.util.DesensitizeUtil.phone(plain);
        } catch (Exception e) {
            return encrypted;
        }
    }

    /** 后台代客下单要求登录且为租户上下文。 */
    public void requireTenantLogin() {
        AuthGuard.requireTenant();
    }

    /**
     * 客户端轻量查询：订单号 + 下单手机号双因子校验，只返回必要字段。
     */
    public Map<String, Object> queryForCustomer(String orderNo, String phone) {
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "订单不存在");
        }
        String stored;
        try {
            stored = secretCipher.decrypt(order.getPhone());
        } catch (Exception e) {
            stored = null;
        }
        if (phone == null || stored == null || !stored.equals(phone.trim())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "手机号与订单不匹配");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("customerName", order.getCustomerName());
        result.put("status", order.getStatus());
        result.put("totalAmount", order.getTotalAmount());
        result.put("paidAmount", order.getPaidAmount());
        result.put("depositAmount", order.getDepositAmount());
        result.put("tailAmount", order.getTailAmount());
        result.put("checkinDate", order.getCheckinDate());
        result.put("checkoutDate", order.getCheckoutDate());
        result.put("roomId", order.getRoomId());
        result.put("packageId", order.getPackageId());
        result.put("items", itemMapper.selectList(Wrappers.<BizOrderItem>lambdaQuery()
                .eq(BizOrderItem::getOrderId, order.getId())));
        return result;
    }

    /**
     * 会员「我的订单」：按登录会员手机号匹配（手机号密文随机 IV，无法等值查询，
     * 对租户内最近 500 条订单解密比对；单租户订单量级可接受，规模化时应加 phone_hash 索引列）。
     */
    public List<Map<String, Object>> listForCustomer(String phone, String status) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }
        List<BizOrder> rows = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(status != null && !status.isBlank(), BizOrder::getStatus, status)
                .orderByDesc(BizOrder::getId).last("limit 500"));
        String target = phone.trim();
        List<Map<String, Object>> result = new ArrayList<>();
        for (BizOrder row : rows) {
            try {
                if (!target.equals(secretCipher.decrypt(row.getPhone()))) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.getId());
            item.put("orderNo", row.getOrderNo());
            item.put("customerName", row.getCustomerName());
            item.put("status", row.getStatus());
            item.put("totalAmount", row.getTotalAmount());
            item.put("paidAmount", row.getPaidAmount());
            item.put("depositAmount", row.getDepositAmount());
            item.put("tailAmount", row.getTailAmount());
            item.put("checkinDate", row.getCheckinDate());
            item.put("checkoutDate", row.getCheckoutDate());
            item.put("roomId", row.getRoomId());
            item.put("packageId", row.getPackageId());
            item.put("careMode", row.getCareMode());
            item.put("createdTime", row.getCreatedTime());
            result.add(item);
        }
        return result;
    }

    /** 会员视角订单详情：手机号必须是订单下单人。 */
    public Map<String, Object> detailForCustomer(String orderNo, String phone) {
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "订单不存在");
        }
        String stored;
        try {
            stored = secretCipher.decrypt(order.getPhone());
        } catch (Exception e) {
            stored = null;
        }
        if (phone == null || stored == null || !stored.equals(phone.trim())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        Map<String, Object> result = detail(order.getId());
        BizOrder safe = order;
        safe.setPhone(com.yzz.common.util.DesensitizeUtil.phone(stored));
        result.put("order", safe);
        return result;
    }

    /** 会员端自助退订申请：仅待入住/待支付可申请，写入退款单待后台审核。 */
    @Transactional
    public Map<String, Object> applyRefundForCustomer(String orderNo, String phone, String reason) {
        BizOrder order = orderMapper.selectOne(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getOrderNo, orderNo).last("limit 1"));
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "订单不存在");
        }
        String stored;
        try {
            stored = secretCipher.decrypt(order.getPhone());
        } catch (Exception e) {
            stored = null;
        }
        if (phone == null || stored == null || !stored.equals(phone.trim())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (!List.of("待入住", "待支付", "待结算").contains(order.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "当前状态不可申请退订：" + order.getStatus());
        }
        Long pending = refundMapper.selectCount(Wrappers.<BizRefund>lambdaQuery()
                .eq(BizRefund::getOrderId, order.getId()).eq(BizRefund::getStatus, "待审核"));
        if (pending > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "已有退订申请在审核中，请勿重复提交");
        }
        BizRefund refund = new BizRefund();
        refund.setTenantId(order.getTenantId());
        refund.setOrderId(order.getId());
        refund.setRefundNo("TK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        refund.setAmount(paid);
        refund.setReason(reason == null || reason.isBlank() ? "客户自助申请退订" : reason);
        refund.setStatus("待审核");
        refundMapper.insert(refund);
        log.info("客户自助退订 tenantId={} orderNo={} amount={}", order.getTenantId(), orderNo, paid);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("refundNo", refund.getRefundNo());
        result.put("amount", paid);
        result.put("status", "待审核");
        return result;
    }
}
