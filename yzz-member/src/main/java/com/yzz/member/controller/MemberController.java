package com.yzz.member.controller;

import com.yzz.common.api.R;
import com.yzz.common.tenant.AuthGuard;
import com.yzz.member.entity.MemberAddress;
import com.yzz.member.entity.MemberCoupon;
import com.yzz.member.entity.MemberInfo;
import com.yzz.member.entity.MemberPointLog;
import com.yzz.member.entity.MemberRecharge;
import com.yzz.member.entity.MemberTag;
import com.yzz.member.entity.MktActivity;
import com.yzz.member.entity.MktCoupon;
import com.yzz.member.service.MarketingService;
import com.yzz.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 会员、积分、优惠券、储值、地址、活动与触达（T47、T78）。
 */
@Tag(name = "租户-会员与营销")
@RestController
@RequestMapping("/api/tenant/members")
public class MemberController {

    private final MemberService memberService;
    private final MarketingService marketingService;

    public MemberController(MemberService memberService, MarketingService marketingService) {
        this.memberService = memberService;
        this.marketingService = marketingService;
    }

    @Operation(summary = "会员列表")
    @GetMapping
    public R<List<MemberInfo>> members(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long tagId) {
        AuthGuard.requireTenant();
        return R.ok(memberService.members(keyword, tagId));
    }

    @Operation(summary = "保存会员")
    @PostMapping
    public R<Long> save(@RequestBody MemberInfo request) {
        AuthGuard.requireTenant();
        return R.ok(memberService.save(request));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/{id}/points")
    public R<List<MemberPointLog>> points(@PathVariable Long id) {
        AuthGuard.requireTenant();
        return R.ok(memberService.pointLogs(id));
    }

    @Operation(summary = "调整积分")
    @PostMapping("/{id}/points")
    public R<Void> changePoints(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        memberService.changePoints(id, Integer.parseInt(String.valueOf(body.get("delta"))),
                String.valueOf(body.getOrDefault("reason", "人工调整")));
        return R.ok();
    }

    @Operation(summary = "标签列表")
    @GetMapping("/tags")
    public R<List<MemberTag>> tags() {
        AuthGuard.requireTenant();
        return R.ok(memberService.tags());
    }

    @Operation(summary = "保存标签")
    @PostMapping("/tags")
    public R<Long> saveTag(@RequestBody MemberTag request) {
        AuthGuard.requireTenant();
        return R.ok(memberService.saveTag(request));
    }

    @Operation(summary = "会员打标签")
    @PostMapping("/{memberId}/tags/{tagId}")
    public R<Void> bindTag(@PathVariable Long memberId, @PathVariable Long tagId) {
        AuthGuard.requireTenant();
        memberService.bindTag(memberId, tagId);
        return R.ok();
    }

    @Operation(summary = "优惠券模板列表")
    @GetMapping("/coupons")
    public R<List<MktCoupon>> coupons(@RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(memberService.coupons(status));
    }

    @Operation(summary = "保存优惠券模板")
    @PostMapping("/coupons")
    public R<Long> saveCoupon(@RequestBody MktCoupon request) {
        AuthGuard.requireTenant();
        return R.ok(memberService.saveCoupon(request));
    }

    @Operation(summary = "发放优惠券")
    @PostMapping("/{memberId}/coupons/{couponId}")
    public R<Long> issueCoupon(@PathVariable Long memberId, @PathVariable Long couponId) {
        AuthGuard.requireTenant();
        return R.ok(memberService.issueCoupon(memberId, couponId));
    }

    @Operation(summary = "会员持券列表")
    @GetMapping("/{memberId}/coupons")
    public R<List<Map<String, Object>>> memberCoupons(@PathVariable Long memberId) {
        AuthGuard.requireTenant();
        return R.ok(memberService.memberCoupons(memberId));
    }

    @Operation(summary = "储值")
    @PostMapping("/{memberId}/recharge")
    public R<Map<String, Object>> recharge(@PathVariable Long memberId, @RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        BigDecimal gift = body.get("giftAmount") == null ? null
                : new BigDecimal(String.valueOf(body.get("giftAmount")));
        boolean ruleShown = Boolean.parseBoolean(String.valueOf(body.getOrDefault("ruleShown", "true")));
        return R.ok(memberService.recharge(memberId, amount, gift, ruleShown));
    }

    @Operation(summary = "储值记录")
    @GetMapping("/{memberId}/recharges")
    public R<List<MemberRecharge>> recharges(@PathVariable Long memberId) {
        AuthGuard.requireTenant();
        return R.ok(memberService.recharges(memberId));
    }

    @Operation(summary = "收货地址")
    @GetMapping("/{memberId}/addresses")
    public R<List<MemberAddress>> addresses(@PathVariable Long memberId) {
        AuthGuard.requireTenant();
        return R.ok(memberService.addresses(memberId));
    }

    @Operation(summary = "保存收货地址")
    @PostMapping("/addresses")
    public R<Long> saveAddress(@RequestBody MemberAddress request) {
        AuthGuard.requireTenant();
        return R.ok(memberService.saveAddress(request));
    }

    @Operation(summary = "营销活动列表")
    @GetMapping("/activities")
    public R<List<MktActivity>> activities(@RequestParam(required = false) String status) {
        AuthGuard.requireTenant();
        return R.ok(marketingService.activities(status));
    }

    @Operation(summary = "保存营销活动")
    @PostMapping("/activities")
    public R<Long> saveActivity(@RequestBody MktActivity request) {
        AuthGuard.requireTenant();
        return R.ok(marketingService.saveActivity(request));
    }

    @Operation(summary = "转介绍记录")
    @GetMapping("/referrals")
    public R<List<Map<String, Object>>> referrals() {
        AuthGuard.requireTenant();
        return R.ok(marketingService.referrals());
    }

    @Operation(summary = "结算转介绍奖励")
    @PostMapping("/referrals/{id}/settle")
    public R<Void> settle(@PathVariable Long id) {
        AuthGuard.requireTenant();
        marketingService.settle(id);
        return R.ok();
    }

    @Operation(summary = "分群触达（频次限制每人每天 1 次）")
    @PostMapping("/touch")
    public R<Map<String, Object>> touch(@RequestBody Map<String, Object> body) {
        AuthGuard.requireTenant();
        Long tagId = body.get("tagId") == null ? null : Long.valueOf(String.valueOf(body.get("tagId")));
        return R.ok(marketingService.touch(tagId, String.valueOf(body.getOrDefault("content", ""))));
    }
}
