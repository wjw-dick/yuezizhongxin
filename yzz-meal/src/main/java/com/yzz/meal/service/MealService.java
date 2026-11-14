package com.yzz.meal.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.meal.entity.MealOrder;
import com.yzz.meal.entity.MealRestrict;
import com.yzz.meal.entity.MealTemplate;
import com.yzz.meal.mapper.MealOrderMapper;
import com.yzz.meal.mapper.MealRestrictMapper;
import com.yzz.meal.mapper.MealTemplateMapper;
import com.yzz.order.entity.BizOrder;
import com.yzz.order.mapper.BizOrderMapper;
import com.yzz.room.entity.Room;
import com.yzz.room.mapper.RoomMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 月子餐（T45）。
 *
 * <p>按入住天数自动匹配阶段餐单；忌口与过敏原做强制校验，命中即标记特殊餐并在出餐表提示。
 * 出餐按房间汇总，后厨按份数核对；加餐单独计费。</p>
 */
@Service
public class MealService {

    private static final Logger log = LoggerFactory.getLogger(MealService.class);
    private static final List<String> MEAL_TYPES =
            List.of("早餐", "上午加餐", "午餐", "下午加餐", "晚餐", "晚间加餐");

    private final MealTemplateMapper templateMapper;
    private final MealOrderMapper mealOrderMapper;
    private final MealRestrictMapper restrictMapper;
    private final BizOrderMapper orderMapper;
    private final RoomMapper roomMapper;

    public MealService(MealTemplateMapper templateMapper, MealOrderMapper mealOrderMapper,
                       MealRestrictMapper restrictMapper, BizOrderMapper orderMapper,
                       RoomMapper roomMapper) {
        this.templateMapper = templateMapper;
        this.mealOrderMapper = mealOrderMapper;
        this.restrictMapper = restrictMapper;
        this.orderMapper = orderMapper;
        this.roomMapper = roomMapper;
    }

    public List<MealTemplate> templates(String stage) {
        return templateMapper.selectList(Wrappers.<MealTemplate>lambdaQuery()
                .eq(stage != null && !stage.isBlank(), MealTemplate::getStage, stage)
                .orderByAsc(MealTemplate::getStage).orderByAsc(MealTemplate::getDayIndex)
                .orderByAsc(MealTemplate::getMealType));
    }

    @Transactional
    public Long saveTemplate(MealTemplate input) {
        if (input.getStage() == null || input.getDishes() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "阶段与菜品清单必填");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getDayIndex() == null) {
            input.setDayIndex(1);
        }
        if (input.getId() == null) {
            templateMapper.insert(input);
        } else {
            templateMapper.updateById(input);
        }
        return input.getId();
    }

    public List<MealRestrict> restricts(Long orderId) {
        return restrictMapper.selectList(Wrappers.<MealRestrict>lambdaQuery()
                .eq(MealRestrict::getOrderId, orderId));
    }

    @Transactional
    public Long addRestrict(MealRestrict input) {
        if (input.getOrderId() == null || input.getContent() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "订单与忌口内容必填");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getRestrictType() == null) {
            input.setRestrictType("忌口");
        }
        restrictMapper.insert(input);
        return input.getId();
    }

    @Transactional
    public void removeRestrict(Long id) {
        restrictMapper.deleteById(id);
    }

    /**
     * 为某日全部在住客户生成出餐表。重复执行会跳过已存在的餐次。
     */
    @Transactional
    public int generate(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<BizOrder> orders = orderMapper.selectList(Wrappers.<BizOrder>lambdaQuery()
                .eq(BizOrder::getStatus, "在住")
                .le(BizOrder::getCheckinDate, day)
                .ge(BizOrder::getCheckoutDate, day));
        int created = 0;
        for (BizOrder order : orders) {
            long dayIndex = day.toEpochDay() - order.getCheckinDate().toEpochDay() + 1;
            String stage = stageOf((int) dayIndex);
            List<MealRestrict> restricts = restricts(order.getId());
            for (String mealType : MEAL_TYPES) {
                Long exists = mealOrderMapper.selectCount(Wrappers.<MealOrder>lambdaQuery()
                        .eq(MealOrder::getOrderId, order.getId())
                        .eq(MealOrder::getMealDate, day)
                        .eq(MealOrder::getMealType, mealType));
                if (exists > 0) {
                    continue;
                }
                MealTemplate template = pickTemplate(stage, (int) dayIndex, mealType);
                MealOrder meal = new MealOrder();
                meal.setTenantId(order.getTenantId());
                meal.setOrderId(order.getId());
                meal.setRoomId(order.getRoomId());
                meal.setMealDate(day);
                meal.setMealType(mealType);
                meal.setDishes(template == null ? "待定" : template.getDishes());
                meal.setExtraCharge(BigDecimal.ZERO);
                meal.setChecked(0);
                String conflict = conflictOf(template, restricts);
                if (conflict != null) {
                    meal.setSpecialTag("忌口冲突");
                    meal.setAbnormal(conflict);
                    log.warn("餐单忌口冲突 orderId={} date={} meal={} conflict={}",
                            order.getId(), day, mealType, conflict);
                }
                mealOrderMapper.insert(meal);
                created++;
            }
        }
        return created;
    }

    public List<MealOrder> meals(LocalDate date, Long orderId, Integer checked) {
        return mealOrderMapper.selectList(Wrappers.<MealOrder>lambdaQuery()
                .eq(date != null, MealOrder::getMealDate, date)
                .eq(orderId != null, MealOrder::getOrderId, orderId)
                .eq(checked != null, MealOrder::getChecked, checked)
                .orderByAsc(MealOrder::getRoomId).orderByAsc(MealOrder::getMealType));
    }

    /** 出餐汇总：按房间统计份数与特殊标记。 */
    public List<Map<String, Object>> roomSummary(LocalDate date) {
        List<MealOrder> rows = meals(date, null, null);
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (MealOrder row : rows) {
            Map<String, Object> item = grouped.computeIfAbsent(row.getRoomId(), key -> {
                Map<String, Object> map = new LinkedHashMap<>();
                Room room = key == null ? null : roomMapper.selectById(key);
                map.put("roomId", key);
                map.put("roomNo", room == null ? "未分配" : room.getRoomNo());
                map.put("count", 0);
                map.put("special", new ArrayList<String>());
                return map;
            });
            item.put("count", (int) item.get("count") + 1);
            if (row.getSpecialTag() != null) {
                @SuppressWarnings("unchecked")
                List<String> special = (List<String>) item.get("special");
                special.add(row.getSpecialTag() + "(" + row.getMealType() + ")");
            }
        }
        return new ArrayList<>(grouped.values());
    }

    /** 加餐申请：单独计费并写入出餐表。 */
    @Transactional
    public Long addOn(Long orderId, LocalDate date, String mealType, String dishes, BigDecimal charge) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "订单不存在");
        }
        MealOrder meal = new MealOrder();
        meal.setTenantId(TenantContext.requireTenantId());
        meal.setOrderId(orderId);
        meal.setRoomId(order.getRoomId());
        meal.setMealDate(date == null ? LocalDate.now() : date);
        meal.setMealType(mealType == null ? "加餐" : mealType);
        meal.setDishes(dishes);
        meal.setExtraCharge(charge == null ? BigDecimal.ZERO : charge);
        meal.setChecked(0);
        mealOrderMapper.insert(meal);
        return meal.getId();
    }

    @Transactional
    public void check(Long id, String abnormal) {
        MealOrder meal = mealOrderMapper.selectById(id);
        if (meal == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "出餐记录不存在");
        }
        meal.setChecked(1);
        if (abnormal != null && !abnormal.isBlank()) {
            meal.setAbnormal(abnormal);
        }
        mealOrderMapper.updateById(meal);
    }

    static String stageOf(int dayIndex) {
        if (dayIndex <= 7) {
            return "排恶露期";
        }
        if (dayIndex <= 14) {
            return "调理期";
        }
        if (dayIndex <= 28) {
            return "滋补期";
        }
        return "塑形期";
    }

    private MealTemplate pickTemplate(String stage, int dayIndex, String mealType) {
        List<MealTemplate> rows = templateMapper.selectList(Wrappers.<MealTemplate>lambdaQuery()
                .eq(MealTemplate::getStage, stage)
                .eq(MealTemplate::getMealType, mealType)
                .orderByAsc(MealTemplate::getDayIndex));
        if (rows.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(dayIndex - 1, rows.size());
        return rows.get(index);
    }

    private String conflictOf(MealTemplate template, List<MealRestrict> restricts) {
        if (restricts.isEmpty()) {
            return null;
        }
        StringBuilder hits = new StringBuilder();
        String dishes = template == null ? "" : template.getDishes();
        String avoid = template == null ? "" : template.getAvoidTags();
        for (MealRestrict restrict : restricts) {
            String content = restrict.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            for (String token : content.split("[、,，\\s]+")) {
                if (token.isBlank()) {
                    continue;
                }
                if (dishes.contains(token) || (avoid != null && avoid.contains(token))) {
                    hits.append(token).append("、");
                }
            }
        }
        return hits.isEmpty() ? null : "命中忌口/过敏原：" + hits.substring(0, hits.length() - 1);
    }
}
