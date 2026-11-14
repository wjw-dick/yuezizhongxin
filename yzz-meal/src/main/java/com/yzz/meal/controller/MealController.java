package com.yzz.meal.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.meal.entity.MealOrder;
import com.yzz.meal.entity.MealRestrict;
import com.yzz.meal.entity.MealTemplate;
import com.yzz.meal.service.MealService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 月子餐模板、忌口、出餐与加餐（T45）。
 */
@Tag(name = "租户-月子餐")
@RestController
@RequestMapping("/api/tenant/meals")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @Operation(summary = "阶段餐单模板")
    @GetMapping("/templates")
    public R<List<MealTemplate>> templates(@RequestParam(required = false) String stage) {
        AuthGuard.requireTenant();
        return R.ok(mealService.templates(stage));
    }

    @Operation(summary = "保存阶段餐单")
    @PostMapping("/templates")
    public R<Long> saveTemplate(@RequestBody MealTemplate request) {
        AuthGuard.requireTenant();
        return R.ok(mealService.saveTemplate(request));
    }

    @Operation(summary = "忌口与过敏原")
    @GetMapping("/restricts")
    public R<List<MealRestrict>> restricts(@RequestParam Long orderId) {
        AuthGuard.requireTenant();
        return R.ok(mealService.restricts(orderId));
    }

    @Operation(summary = "新增忌口")
    @PostMapping("/restricts")
    public R<Long> addRestrict(@RequestBody MealRestrict request) {
        AuthGuard.requireTenant();
        return R.ok(mealService.addRestrict(request));
    }

    @Operation(summary = "删除忌口")
    @DeleteMapping("/restricts/{id}")
    public R<Void> removeRestrict(@PathVariable Long id) {
        AuthGuard.requireTenant();
        mealService.removeRestrict(id);
        return R.ok();
    }

    @Operation(summary = "生成某日出餐表（忌口强校验）")
    @PostMapping("/generate")
    public R<Integer> generate(@RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(mealService.generate(date));
    }

    @Operation(summary = "出餐列表")
    @GetMapping("/orders")
    public R<List<MealOrder>> meals(@RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) Long orderId,
                                    @RequestParam(required = false) Integer checked) {
        AuthGuard.requireTenant();
        return R.ok(mealService.meals(date, orderId, checked));
    }

    @Operation(summary = "按房间出餐汇总")
    @GetMapping("/summary")
    public R<List<Map<String, Object>>> summary(@RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthGuard.requireTenant();
        return R.ok(mealService.roomSummary(date));
    }

    @Operation(summary = "加餐申请")
    @PostMapping("/add-on")
    public R<Long> addOn(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        LocalDate date = body.get("date") == null ? LocalDate.now()
                : LocalDate.parse(String.valueOf(body.get("date")));
        BigDecimal charge = body.get("charge") == null ? BigDecimal.ZERO
                : new BigDecimal(String.valueOf(body.get("charge")));
        return R.ok(mealService.addOn(orderId, date, (String) body.get("mealType"),
                (String) body.get("dishes"), charge));
    }

    @Operation(summary = "出餐核对与异常回传")
    @PutMapping("/orders/{id}/check")
    public R<Void> check(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        AuthGuard.requireTenant();
        mealService.check(id, body == null ? null : body.get("abnormal"));
        return R.ok();
    }
}
