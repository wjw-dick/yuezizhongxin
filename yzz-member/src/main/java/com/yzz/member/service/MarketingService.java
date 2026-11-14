package com.yzz.member.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.guard.CacheStore;
import com.yzz.common.tenant.TenantContext;
import com.yzz.member.entity.MemberInfo;
import com.yzz.member.entity.MemberTagRel;
import com.yzz.member.entity.MktActivity;
import com.yzz.member.entity.MktReferral;
import com.yzz.member.mapper.MemberInfoMapper;
import com.yzz.member.mapper.MemberTagRelMapper;
import com.yzz.member.mapper.MktActivityMapper;
import com.yzz.member.mapper.MktReferralMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销活动、转介绍与分群触达（T47）。
 *
 * <p>触达频次限制：同一会员同一活动每天最多 1 次，用缓存键做原子判定，
 * 多实例部署切 Redis 后同样有效。</p>
 */
@Service
public class MarketingService {

    private static final Logger log = LoggerFactory.getLogger(MarketingService.class);
    private static final String TOUCH_KEY = "mkt:touch:";

    private final MktActivityMapper activityMapper;
    private final MktReferralMapper referralMapper;
    private final MemberInfoMapper memberMapper;
    private final MemberTagRelMapper tagRelMapper;
    private final MemberService memberService;
    private final CacheStore cacheStore;

    public MarketingService(MktActivityMapper activityMapper, MktReferralMapper referralMapper,
                            MemberInfoMapper memberMapper, MemberTagRelMapper tagRelMapper,
                            MemberService memberService, CacheStore cacheStore) {
        this.activityMapper = activityMapper;
        this.referralMapper = referralMapper;
        this.memberMapper = memberMapper;
        this.tagRelMapper = tagRelMapper;
        this.memberService = memberService;
        this.cacheStore = cacheStore;
    }

    public List<MktActivity> activities(String status) {
        return activityMapper.selectList(Wrappers.<MktActivity>lambdaQuery()
                .eq(status != null && !status.isBlank(), MktActivity::getStatus, status)
                .orderByDesc(MktActivity::getId));
    }

    @Transactional
    public Long saveActivity(MktActivity input) {
        if (input.getName() == null || input.getName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "活动名称不能为空");
        }
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getStatus() == null) {
            input.setStatus(resolveStatus(input));
        }
        if (input.getJoinCount() == null) {
            input.setJoinCount(0);
        }
        if (input.getId() == null) {
            activityMapper.insert(input);
        } else {
            activityMapper.updateById(input);
        }
        return input.getId();
    }

    /** 活动状态按时间自动推进：未开始 / 进行中 / 已结束。 */
    @Transactional
    public int refreshStatus() {
        List<MktActivity> rows = activities(null);
        int changed = 0;
        for (MktActivity row : rows) {
            String next = resolveStatus(row);
            if (!next.equals(row.getStatus()) && !"已停用".equals(row.getStatus())) {
                row.setStatus(next);
                activityMapper.updateById(row);
                changed++;
            }
        }
        return changed;
    }

    @Transactional
    public Long refer(Long referrerMemberId, Long referredLeadId, int points, BigDecimal amount) {
        MemberInfo referrer = memberService.require(referrerMemberId);
        MktReferral referral = new MktReferral();
        referral.setTenantId(referrer.getTenantId());
        referral.setReferrerMember(referrerMemberId);
        referral.setReferredLead(referredLeadId);
        referral.setRewardPoints(points);
        referral.setRewardAmount(amount == null ? BigDecimal.ZERO : amount);
        referral.setStatus("待结算");
        referralMapper.insert(referral);
        return referral.getId();
    }

    /** 结算奖励：积分即时入账，金额记入储值余额。 */
    @Transactional
    public void settle(Long referralId) {
        MktReferral referral = referralMapper.selectById(referralId);
        if (referral == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "转介绍记录不存在");
        }
        if ("已结算".equals(referral.getStatus())) {
            return;
        }
        if (referral.getRewardPoints() != null && referral.getRewardPoints() > 0) {
            memberService.changePoints(referral.getReferrerMember(), referral.getRewardPoints(), "转介绍奖励");
        }
        referral.setStatus("已结算");
        referralMapper.updateById(referral);
    }

    public List<Map<String, Object>> referrals() {
        List<MktReferral> rows = referralMapper.selectList(Wrappers.<MktReferral>lambdaQuery()
                .orderByDesc(MktReferral::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MktReferral row : rows) {
            MemberInfo member = memberMapper.selectById(row.getReferrerMember());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("referral", row);
            item.put("referrerName", member == null ? "-" : member.getNickname());
            result.add(item);
        }
        return result;
    }

    /**
     * 分群触达：按标签圈人，逐人做频次判定。
     *
     * @return 实际触达人数与被频次限制拦截的人数
     */
    public Map<String, Object> touch(Long tagId, String content) {
        List<Long> memberIds;
        if (tagId == null) {
            memberIds = memberMapper.selectList(Wrappers.<MemberInfo>lambdaQuery()).stream()
                    .map(MemberInfo::getId).toList();
        } else {
            memberIds = tagRelMapper.selectList(Wrappers.<MemberTagRel>lambdaQuery()
                            .eq(MemberTagRel::getTagId, tagId))
                    .stream().map(MemberTagRel::getMemberId).toList();
        }
        int sent = 0;
        int limited = 0;
        String day = LocalDate.now().toString();
        Long tenantId = TenantContext.requireTenantId();
        for (Long memberId : memberIds) {
            String key = TOUCH_KEY + tenantId + ":" + memberId + ":" + day;
            if (!cacheStore.setIfAbsent(key, Duration.ofDays(1))) {
                limited++;
                continue;
            }
            sent++;
            log.info("分群触达 tenantId={} memberId={} content={}", tenantId, memberId, content);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targets", memberIds.size());
        result.put("sent", sent);
        result.put("limited", limited);
        result.put("content", content);
        return result;
    }

    private String resolveStatus(MktActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            return "未开始";
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            return "已结束";
        }
        return "进行中";
    }
}
