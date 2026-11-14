package com.yzz.member.controller;

import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.R;
import com.yzz.common.tenant.TenantContext;
import com.yzz.member.entity.MemberAddress;
import com.yzz.member.entity.MemberInfo;
import com.yzz.member.entity.MemberPointLog;
import com.yzz.member.entity.MemberRecharge;
import com.yzz.member.entity.MktCoupon;
import com.yzz.member.service.MarketingService;
import com.yzz.member.service.MemberAuthService;
import com.yzz.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序 / H5 / App 会员中心开放接口（T74、T78、T85）。
 *
 * <p>无登录态接口：短信验证码、登录。其余接口须带 {@code X-Member-Token}，
 * 令牌内租户必须与请求租户一致，避免跨租户读取会员资产。</p>
 */
@Tag(name = "开放接口-会员中心")
@RestController
@RequestMapping("/api/open/member")
public class OpenMemberController {

    private final MemberAuthService authService;
    private final MemberService memberService;
    private final MarketingService marketingService;

    public OpenMemberController(MemberAuthService authService, MemberService memberService,
                                MarketingService marketingService) {
        this.authService = authService;
        this.memberService = memberService;
        this.marketingService = marketingService;
    }

    @Operation(summary = "发送登录验证码")
    @PostMapping("/sms")
    public R<Map<String, Object>> sms(@RequestBody Map<String, String> body) {
        requireTenant();
        return R.ok(authService.sendCode(body.get("phone")));
    }

    @Operation(summary = "手机号验证码登录（未建档自动注册）")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        requireTenant();
        return R.ok(authService.login(body.get("phone"), body.get("code"),
                body.getOrDefault("source", "小程序")));
    }

    @Operation(summary = "会员资料")
    @GetMapping("/profile")
    public R<Map<String, Object>> profile(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        return R.ok(memberService.profile(authService.resolve(token)));
    }

    @Operation(summary = "修改昵称/头像")
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestHeader("X-Member-Token") String token,
                                 @RequestBody Map<String, String> body) {
        requireTenant();
        memberService.updateProfile(authService.resolve(token), body.get("nickname"), body.get("avatar"));
        return R.ok();
    }

    @Operation(summary = "积分流水")
    @GetMapping("/points")
    public R<List<MemberPointLog>> points(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        return R.ok(memberService.pointLogs(authService.resolve(token)));
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/coupons")
    public R<List<Map<String, Object>>> coupons(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        return R.ok(memberService.memberCoupons(authService.resolve(token)));
    }

    @Operation(summary = "可领取优惠券")
    @GetMapping("/coupons/claimable")
    public R<List<MktCoupon>> claimable(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        authService.resolve(token);
        return R.ok(memberService.claimableCoupons());
    }

    @Operation(summary = "领取优惠券")
    @PostMapping("/coupons/{id}/claim")
    public R<Long> claim(@RequestHeader("X-Member-Token") String token, @PathVariable Long id) {
        requireTenant();
        return R.ok(memberService.issueCoupon(authService.resolve(token), id));
    }

    @Operation(summary = "储值（需已确认预付卡规则）")
    @PostMapping("/recharge")
    public R<Map<String, Object>> recharge(@RequestHeader("X-Member-Token") String token,
                                           @RequestBody Map<String, Object> body) {
        requireTenant();
        Long memberId = authService.resolve(token);
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        BigDecimal gift = body.get("giftAmount") == null ? null
                : new BigDecimal(String.valueOf(body.get("giftAmount")));
        boolean ruleShown = Boolean.parseBoolean(String.valueOf(body.getOrDefault("ruleShown", false)));
        return R.ok(memberService.recharge(memberId, amount, gift, ruleShown));
    }

    @Operation(summary = "储值记录")
    @GetMapping("/recharges")
    public R<List<MemberRecharge>> recharges(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        return R.ok(memberService.recharges(authService.resolve(token)));
    }

    @Operation(summary = "收货/联系地址列表")
    @GetMapping("/addresses")
    public R<List<MemberAddress>> addresses(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        return R.ok(memberService.addresses(authService.resolve(token)));
    }

    @Operation(summary = "保存地址")
    @PostMapping("/addresses")
    public R<Long> saveAddress(@RequestHeader("X-Member-Token") String token,
                               @RequestBody MemberAddress address) {
        requireTenant();
        address.setMemberId(authService.resolve(token));
        return R.ok(memberService.saveAddress(address));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/addresses/{id}")
    public R<Void> removeAddress(@RequestHeader("X-Member-Token") String token, @PathVariable Long id) {
        requireTenant();
        memberService.removeAddress(authService.resolve(token), id);
        return R.ok();
    }

    @Operation(summary = "我的转介绍记录")
    @GetMapping("/referrals")
    public R<List<Map<String, Object>>> referrals(@RequestHeader("X-Member-Token") String token) {
        requireTenant();
        Long memberId = authService.resolve(token);
        return R.ok(marketingService.referrals().stream()
                .filter(row -> {
                    Object referral = row.get("referral");
                    return referral instanceof com.yzz.member.entity.MktReferral m
                            && memberId.equals(m.getReferrerMember());
                })
                .toList());
    }

    @Operation(summary = "储值规则（预付卡公示文案）")
    @GetMapping("/recharge-rule")
    public R<Map<String, Object>> rechargeRule() {
        requireTenant();
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("threshold", 5000);
        rule.put("giftRate", 0.05);
        rule.put("items", List.of(
                "储值满 5000 元赠 5%，赠额与本金同账户，可用于房费、月子餐、康复项目结算",
                "储值余额不可提现、不可转让，退订时按实际消费结算后退还剩余本金",
                "储值本金与赠额消费顺序：先赠额后本金",
                "本规则依据单用途预付卡管理相关规定公示，充值前请确认知悉"));
        return R.ok(rule);
    }

    private void requireTenant() {
        if (TenantContext.getTenantId() == null) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
    }

    /** 供其它模块按会员主键取脱敏资料（避免直接暴露 MemberInfo）。 */
    @SuppressWarnings("unused")
    private MemberInfo require(Long memberId) {
        return memberService.require(memberId);
    }
}
