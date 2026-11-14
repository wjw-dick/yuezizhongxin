package com.yzz.member.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.DesensitizeUtil;
import com.yzz.common.util.SecretCipher;
import com.yzz.member.entity.MemberAddress;
import com.yzz.member.entity.MemberCoupon;
import com.yzz.member.entity.MemberInfo;
import com.yzz.member.entity.MemberPointLog;
import com.yzz.member.entity.MemberRecharge;
import com.yzz.member.entity.MemberTag;
import com.yzz.member.entity.MemberTagRel;
import com.yzz.member.entity.MktCoupon;
import com.yzz.member.mapper.MemberAddressMapper;
import com.yzz.member.mapper.MemberCouponMapper;
import com.yzz.member.mapper.MemberInfoMapper;
import com.yzz.member.mapper.MemberPointLogMapper;
import com.yzz.member.mapper.MemberRechargeMapper;
import com.yzz.member.mapper.MemberTagMapper;
import com.yzz.member.mapper.MemberTagRelMapper;
import com.yzz.member.mapper.MktCouponMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员、积分、优惠券与储值（T47、T78）。
 *
 * <p>UnionID 是多端唯一口径：同一 UnionID 只建一条会员档案；积分与储值余额变更
 * 必须写流水，储值须展示预付卡规则并留痕。</p>
 */
@Service
public class MemberService {

    private final MemberInfoMapper memberMapper;
    private final MemberTagMapper tagMapper;
    private final MemberTagRelMapper tagRelMapper;
    private final MemberPointLogMapper pointLogMapper;
    private final MktCouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final MemberRechargeMapper rechargeMapper;
    private final MemberAddressMapper addressMapper;
    private final SecretCipher secretCipher;

    public MemberService(MemberInfoMapper memberMapper, MemberTagMapper tagMapper,
                         MemberTagRelMapper tagRelMapper, MemberPointLogMapper pointLogMapper,
                         MktCouponMapper couponMapper, MemberCouponMapper memberCouponMapper,
                         MemberRechargeMapper rechargeMapper, MemberAddressMapper addressMapper,
                         SecretCipher secretCipher) {
        this.memberMapper = memberMapper;
        this.tagMapper = tagMapper;
        this.tagRelMapper = tagRelMapper;
        this.pointLogMapper = pointLogMapper;
        this.couponMapper = couponMapper;
        this.memberCouponMapper = memberCouponMapper;
        this.rechargeMapper = rechargeMapper;
        this.addressMapper = addressMapper;
        this.secretCipher = secretCipher;
    }

    public List<MemberInfo> members(String keyword, Long tagId) {
        List<Long> memberIds = null;
        if (tagId != null) {
            memberIds = tagRelMapper.selectList(Wrappers.<MemberTagRel>lambdaQuery()
                            .eq(MemberTagRel::getTagId, tagId))
                    .stream().map(MemberTagRel::getMemberId).toList();
            if (memberIds.isEmpty()) {
                return List.of();
            }
        }
        List<MemberInfo> rows = memberMapper.selectList(Wrappers.<MemberInfo>lambdaQuery()
                .in(memberIds != null, MemberInfo::getId, memberIds)
                .like(keyword != null && !keyword.isBlank(), MemberInfo::getNickname, keyword)
                .orderByDesc(MemberInfo::getId));
        rows.forEach(row -> row.setPhone(mask(row.getPhone())));
        return rows;
    }

    /**
     * 按 UnionID 建档或更新，返回会员主键。
     */
    @Transactional
    public Long upsertByUnionId(String unionId, String openId, String phone, String nickname, String source) {
        if (unionId == null || unionId.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "UnionID 不能为空");
        }
        Long tenantId = TenantContext.requireTenantId();
        MemberInfo exist = memberMapper.selectOne(Wrappers.<MemberInfo>lambdaQuery()
                .eq(MemberInfo::getUnionId, unionId).last("limit 1"));
        if (exist != null) {
            if (openId != null) {
                exist.setOpenId(openId);
            }
            if (nickname != null) {
                exist.setNickname(nickname);
            }
            memberMapper.updateById(exist);
            return exist.getId();
        }
        MemberInfo member = new MemberInfo();
        member.setTenantId(tenantId);
        member.setUnionId(unionId);
        member.setOpenId(openId);
        member.setPhone(secretCipher.encrypt(phone));
        member.setNickname(nickname);
        member.setSource(source);
        member.setPoints(0);
        member.setBalance(BigDecimal.ZERO);
        memberMapper.insert(member);
        return member.getId();
    }

    @Transactional
    public Long save(MemberInfo input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getPhone() != null && !DesensitizeUtil.isMasked(input.getPhone())) {
            input.setPhone(secretCipher.encrypt(input.getPhone()));
        }
        if (input.getId() == null) {
            if (input.getPoints() == null) {
                input.setPoints(0);
            }
            if (input.getBalance() == null) {
                input.setBalance(BigDecimal.ZERO);
            }
            memberMapper.insert(input);
        } else {
            memberMapper.updateById(input);
        }
        return input.getId();
    }

    public List<MemberTag> tags() {
        return tagMapper.selectList(Wrappers.<MemberTag>lambdaQuery().orderByAsc(MemberTag::getId));
    }

    @Transactional
    public Long saveTag(MemberTag tag) {
        tag.setTenantId(TenantContext.requireTenantId());
        Long dup = tagMapper.selectCount(Wrappers.<MemberTag>lambdaQuery()
                .eq(MemberTag::getName, tag.getName())
                .ne(tag.getId() != null, MemberTag::getId, tag.getId()));
        if (dup > 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "标签名称已存在");
        }
        if (tag.getId() == null) {
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        return tag.getId();
    }

    @Transactional
    public void bindTag(Long memberId, Long tagId) {
        Long dup = tagRelMapper.selectCount(Wrappers.<MemberTagRel>lambdaQuery()
                .eq(MemberTagRel::getMemberId, memberId).eq(MemberTagRel::getTagId, tagId));
        if (dup > 0) {
            return;
        }
        MemberTagRel rel = new MemberTagRel();
        rel.setTenantId(TenantContext.requireTenantId());
        rel.setMemberId(memberId);
        rel.setTagId(tagId);
        tagRelMapper.insert(rel);
    }

    /**
     * 积分变动。正增负减，余额不允许为负。
     */
    @Transactional
    public void changePoints(Long memberId, int delta, String reason) {
        MemberInfo member = require(memberId);
        int next = (member.getPoints() == null ? 0 : member.getPoints()) + delta;
        if (next < 0) {
            throw BusinessException.of(ErrorCode.CONFLICT, "积分不足，当前可用 " + member.getPoints());
        }
        member.setPoints(next);
        memberMapper.updateById(member);
        MemberPointLog log = new MemberPointLog();
        log.setTenantId(member.getTenantId());
        log.setMemberId(memberId);
        log.setChangeValue(delta);
        log.setReason(reason);
        pointLogMapper.insert(log);
    }

    public List<MemberPointLog> pointLogs(Long memberId) {
        return pointLogMapper.selectList(Wrappers.<MemberPointLog>lambdaQuery()
                .eq(MemberPointLog::getMemberId, memberId).orderByDesc(MemberPointLog::getId));
    }

    public List<MktCoupon> coupons(String status) {
        return couponMapper.selectList(Wrappers.<MktCoupon>lambdaQuery()
                .eq(status != null && !status.isBlank(), MktCoupon::getStatus, status)
                .orderByDesc(MktCoupon::getId));
    }

    @Transactional
    public Long saveCoupon(MktCoupon input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getIssuedCount() == null) {
            input.setIssuedCount(0);
        }
        if (input.getId() == null) {
            couponMapper.insert(input);
        } else {
            couponMapper.updateById(input);
        }
        return input.getId();
    }

    /** 领券：校验库存与有效期，发行量自增。 */
    @Transactional
    public Long issueCoupon(Long memberId, Long couponId) {
        MktCoupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !"ENABLED".equals(coupon.getStatus())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "优惠券不可领取");
        }
        if (coupon.getIssuedCount() >= coupon.getTotalCount()) {
            throw BusinessException.of(ErrorCode.CONFLICT, "优惠券已领完");
        }
        if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.CONFLICT, "优惠券已过期");
        }
        MemberCoupon mc = new MemberCoupon();
        mc.setTenantId(TenantContext.requireTenantId());
        mc.setMemberId(memberId);
        mc.setCouponId(couponId);
        mc.setStatus("未使用");
        memberCouponMapper.insert(mc);
        coupon.setIssuedCount(coupon.getIssuedCount() + 1);
        couponMapper.updateById(coupon);
        return mc.getId();
    }

    public List<Map<String, Object>> memberCoupons(Long memberId) {
        List<MemberCoupon> rows = memberCouponMapper.selectList(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId).orderByDesc(MemberCoupon::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MemberCoupon row : rows) {
            MktCoupon coupon = couponMapper.selectById(row.getCouponId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("memberCoupon", row);
            item.put("coupon", coupon);
            result.add(item);
        }
        return result;
    }

    /**
     * 储值。赠送金额按系统参数的赠送规则计算（默认满 5000 赠 5%）。
     */
    @Transactional
    public Map<String, Object> recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount,
                                        boolean ruleShown) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "储值金额必须大于 0");
        }
        if (!ruleShown) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请先展示并确认预付卡规则");
        }
        MemberInfo member = require(memberId);
        BigDecimal gift = giftAmount == null ? defaultGift(amount) : giftAmount;
        MemberRecharge recharge = new MemberRecharge();
        recharge.setTenantId(member.getTenantId());
        recharge.setMemberId(memberId);
        recharge.setRechargeNo("CZ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        recharge.setAmount(amount);
        recharge.setGiftAmount(gift);
        recharge.setStatus("已支付");
        recharge.setRuleShown(1);
        rechargeMapper.insert(recharge);

        member.setBalance(member.getBalance().add(amount).add(gift).setScale(2, RoundingMode.HALF_UP));
        memberMapper.updateById(member);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rechargeNo", recharge.getRechargeNo());
        result.put("amount", amount);
        result.put("giftAmount", gift);
        result.put("balance", member.getBalance());
        return result;
    }

    public List<MemberRecharge> recharges(Long memberId) {
        return rechargeMapper.selectList(Wrappers.<MemberRecharge>lambdaQuery()
                .eq(MemberRecharge::getMemberId, memberId).orderByDesc(MemberRecharge::getId));
    }

    public List<MemberAddress> addresses(Long memberId) {
        return addressMapper.selectList(Wrappers.<MemberAddress>lambdaQuery()
                .eq(MemberAddress::getMemberId, memberId).orderByDesc(MemberAddress::getIsDefault));
    }

    @Transactional
    public Long saveAddress(MemberAddress input) {
        input.setTenantId(TenantContext.requireTenantId());
        if (input.getPhone() != null && !DesensitizeUtil.isMasked(input.getPhone())) {
            input.setPhone(secretCipher.encrypt(input.getPhone()));
        }
        if (Integer.valueOf(1).equals(input.getIsDefault())) {
            List<MemberAddress> rows = addresses(input.getMemberId());
            for (MemberAddress row : rows) {
                if (row.getIsDefault() == 1) {
                    row.setIsDefault(0);
                    addressMapper.updateById(row);
                }
            }
        }
        if (input.getId() == null) {
            addressMapper.insert(input);
        } else {
            addressMapper.updateById(input);
        }
        return input.getId();
    }

    public MemberInfo require(Long id) {
        MemberInfo member = memberMapper.selectById(id);
        if (member == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "会员不存在");
        }
        return member;
    }

    /**
     * 按手机号查会员。手机号密文带随机 IV，无法直接等值查询，
     * 这里对租户内会员解密比对（单租户会员量级为千级以内，可接受）。
     */
    public MemberInfo findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String target = phone.trim();
        List<MemberInfo> rows = memberMapper.selectList(Wrappers.<MemberInfo>lambdaQuery()
                .orderByDesc(MemberInfo::getId).last("limit 2000"));
        for (MemberInfo row : rows) {
            try {
                if (target.equals(secretCipher.decrypt(row.getPhone()))) {
                    return row;
                }
            } catch (Exception ignored) {
                // 历史脏数据跳过
            }
        }
        return null;
    }

    /** 会员手机号明文（仅服务端内部使用，不下发前端）。 */
    public String plainPhone(Long memberId) {
        MemberInfo member = require(memberId);
        try {
            return secretCipher.decrypt(member.getPhone());
        } catch (Exception e) {
            return null;
        }
    }

    /** 手机号首次登录自动建档。 */
    @Transactional
    public MemberInfo createByPhone(String phone, String nickname, String source) {
        MemberInfo member = new MemberInfo();
        member.setTenantId(TenantContext.requireTenantId());
        member.setPhone(secretCipher.encrypt(phone));
        member.setNickname(nickname == null || nickname.isBlank() ? DesensitizeUtil.phone(phone) : nickname);
        member.setSource(source == null ? "小程序" : source);
        member.setPoints(0);
        member.setBalance(BigDecimal.ZERO);
        memberMapper.insert(member);
        return member;
    }

    /** 会员自助资料（脱敏手机号 + 标签 + 可用权益），供小程序/H5 展示。 */
    public Map<String, Object> profile(Long memberId) {
        MemberInfo member = require(memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", member.getId());
        result.put("nickname", member.getNickname());
        result.put("avatar", member.getAvatar());
        result.put("phone", mask(member.getPhone()));
        result.put("points", member.getPoints() == null ? 0 : member.getPoints());
        result.put("balance", member.getBalance() == null ? BigDecimal.ZERO : member.getBalance());
        result.put("source", member.getSource());
        result.put("createdTime", member.getCreatedTime());
        List<MemberTagRel> rels = tagRelMapper.selectList(Wrappers.<MemberTagRel>lambdaQuery()
                .eq(MemberTagRel::getMemberId, memberId));
        if (rels.isEmpty()) {
            result.put("tags", List.of());
        } else {
            List<Long> tagIds = rels.stream().map(MemberTagRel::getTagId).toList();
            result.put("tags", tagMapper.selectList(Wrappers.<MemberTag>lambdaQuery()
                    .in(MemberTag::getId, tagIds)));
        }
        long usable = memberCouponMapper.selectCount(Wrappers.<MemberCoupon>lambdaQuery()
                .eq(MemberCoupon::getMemberId, memberId).eq(MemberCoupon::getStatus, "未使用"));
        result.put("couponCount", usable);
        return result;
    }

    /** 会员自助修改昵称/头像。 */
    @Transactional
    public void updateProfile(Long memberId, String nickname, String avatar) {
        MemberInfo member = require(memberId);
        if (nickname != null && !nickname.isBlank()) {
            member.setNickname(nickname.trim());
        }
        if (avatar != null && !avatar.isBlank()) {
            member.setAvatar(avatar);
        }
        memberMapper.updateById(member);
    }

    @Transactional
    public void removeAddress(Long memberId, Long addressId) {
        MemberAddress address = addressMapper.selectById(addressId);
        if (address == null || !memberId.equals(address.getMemberId())) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    /** 可领取的优惠券（上架、未过期、有库存）。 */
    public List<MktCoupon> claimableCoupons() {
        return couponMapper.selectList(Wrappers.<MktCoupon>lambdaQuery()
                .eq(MktCoupon::getStatus, "ENABLED")
                .and(w -> w.isNull(MktCoupon::getEndTime)
                        .or().gt(MktCoupon::getEndTime, LocalDateTime.now()))
                .apply("issued_count < total_count")
                .orderByDesc(MktCoupon::getId));
    }

    private BigDecimal defaultGift(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("5000")) >= 0) {
            return amount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private String mask(String encrypted) {
        try {
            return DesensitizeUtil.phone(secretCipher.decrypt(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }
}
