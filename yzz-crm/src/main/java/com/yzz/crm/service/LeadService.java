package com.yzz.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.util.DesensitizeUtil;
import com.yzz.common.util.SecretCipher;
import com.yzz.crm.entity.CrmFollow;
import com.yzz.crm.entity.CrmLead;
import com.yzz.crm.entity.CrmVisit;
import com.yzz.crm.mapper.CrmFollowMapper;
import com.yzz.crm.mapper.CrmLeadMapper;
import com.yzz.crm.mapper.CrmVisitMapper;
import com.yzz.order.dto.OrderCreateRequest;
import com.yzz.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 线索 CRM（T35）。
 *
 * <p>来源归因带参码落 sourceCode；跟进超时按 nextFollowTime 扫描；流失必须填原因；
 * 一键转订单复用订单服务，转单后线索标记为「已签约」。</p>
 */
@Service
public class LeadService {

    private final CrmLeadMapper leadMapper;
    private final CrmFollowMapper followMapper;
    private final CrmVisitMapper visitMapper;
    private final SecretCipher secretCipher;
    private final OrderService orderService;

    public LeadService(CrmLeadMapper leadMapper, CrmFollowMapper followMapper,
                       CrmVisitMapper visitMapper, SecretCipher secretCipher,
                       OrderService orderService) {
        this.leadMapper = leadMapper;
        this.followMapper = followMapper;
        this.visitMapper = visitMapper;
        this.secretCipher = secretCipher;
        this.orderService = orderService;
    }

    public List<CrmLead> list(String stage, String sourceCode, String keyword, Long advisorId) {
        List<CrmLead> rows = leadMapper.selectList(Wrappers.<CrmLead>lambdaQuery()
                .eq(stage != null && !stage.isBlank(), CrmLead::getStage, stage)
                .eq(sourceCode != null && !sourceCode.isBlank(), CrmLead::getSourceCode, sourceCode)
                .eq(advisorId != null, CrmLead::getAdvisorId, advisorId)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(CrmLead::getCustomerName, keyword))
                .orderByDesc(CrmLead::getId));
        rows.forEach(row -> row.setPhone(mask(row.getPhone())));
        return rows;
    }

    public CrmLead detail(Long id) {
        CrmLead lead = require(id);
        lead.setPhone(mask(lead.getPhone()));
        return lead;
    }

    @Transactional
    public Long create(CrmLead input) {
        if (input.getCustomerName() == null || input.getCustomerName().isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "联系人不能为空");
        }
        if (input.getPhone() == null || input.getPhone().length() < 6) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "联系电话不合法");
        }
        input.setTenantId(TenantContext.requireTenantId());
        input.setPhone(secretCipher.encrypt(input.getPhone().trim()));
        if (input.getStage() == null) {
            input.setStage("待联系");
        }
        if (input.getSourceCode() == null || input.getSourceCode().isBlank()) {
            input.setSourceCode("官网留资");
        }
        leadMapper.insert(input);
        return input.getId();
    }

    @Transactional
    public void update(Long id, CrmLead input) {
        CrmLead lead = require(id);
        if ("已流失".equals(input.getStage())
                && (input.getLostReason() == null || input.getLostReason().isBlank())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "标记流失时必须选择流失原因");
        }
        input.setId(lead.getId());
        input.setTenantId(lead.getTenantId());
        if (input.getPhone() != null && !DesensitizeUtil.isMasked(input.getPhone())) {
            input.setPhone(secretCipher.encrypt(input.getPhone()));
        } else {
            input.setPhone(lead.getPhone());
        }
        leadMapper.updateById(input);
    }

    @Transactional
    public void addFollow(Long leadId, CrmFollow follow) {
        CrmLead lead = require(leadId);
        follow.setTenantId(TenantContext.requireTenantId());
        follow.setLeadId(leadId);
        if (follow.getFollowTime() == null) {
            follow.setFollowTime(LocalDateTime.now());
        }
        follow.setOperatorId(TenantContext.getUserId());
        followMapper.insert(follow);
        lead.setNextFollowTime(follow.getNextTime());
        if ("已联系".equals(lead.getStage())) {
            lead.setStage("已联系");
        }
        leadMapper.updateById(lead);
    }

    public List<CrmFollow> follows(Long leadId) {
        return followMapper.selectList(Wrappers.<CrmFollow>lambdaQuery()
                .eq(CrmFollow::getLeadId, leadId).orderByDesc(CrmFollow::getId));
    }

    @Transactional
    public Long createVisit(CrmVisit visit) {
        if (visit.getVisitDate() == null || visit.getTimeSlot() == null) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "参观日期与时段必填");
        }
        visit.setTenantId(TenantContext.requireTenantId());
        if (visit.getPersonCount() == null) {
            visit.setPersonCount(1);
        }
        if (visit.getCheckedIn() == null) {
            visit.setCheckedIn(0);
        }
        visit.setAdvisorId(TenantContext.getUserId());
        visitMapper.insert(visit);
        return visit.getId();
    }

    public List<CrmVisit> visits(LocalDate date) {
        return visitMapper.selectList(Wrappers.<CrmVisit>lambdaQuery()
                .eq(date != null, CrmVisit::getVisitDate, date)
                .orderByAsc(CrmVisit::getVisitDate));
    }

    @Transactional
    public void checkin(Long visitId) {
        CrmVisit visit = visitMapper.selectById(visitId);
        if (visit == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "参观预约不存在");
        }
        visit.setCheckedIn(1);
        visitMapper.updateById(visit);
    }

    /** 跟进超时提醒：返回已超过下次跟进时间的线索。 */
    public List<CrmLead> overdueFollows() {
        List<CrmLead> rows = leadMapper.selectList(Wrappers.<CrmLead>lambdaQuery()
                .isNotNull(CrmLead::getNextFollowTime)
                .lt(CrmLead::getNextFollowTime, LocalDateTime.now())
                .notIn(CrmLead::getStage, List.of("已签约", "已流失"))
                .orderByAsc(CrmLead::getNextFollowTime));
        rows.forEach(row -> row.setPhone(mask(row.getPhone())));
        return rows;
    }

    /**
     * 一键转订单：线索信息带入订单，成功后线索置为已签约。
     */
    @Transactional
    public Map<String, Object> convert(Long leadId, OrderCreateRequest request) {
        CrmLead lead = require(leadId);
        String phone;
        try {
            phone = secretCipher.decrypt(lead.getPhone());
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "线索手机号解密失败，请重新录入");
        }
        OrderCreateRequest merged = new OrderCreateRequest(
                leadId,
                request.customerName() == null ? lead.getCustomerName() : request.customerName(),
                request.phone() == null ? phone : request.phone(),
                request.packageId(), request.roomTypeId(), request.roomId(),
                request.checkinDate(), request.checkoutDate(), request.careMode(),
                request.companionCount(), request.bondAmount(), request.discountAmount(),
                request.remark(), "线索转订单", request.extras(), request.products());
        Map<String, Object> order = orderService.create(merged);
        lead.setStage("已签约");
        leadMapper.updateById(lead);
        Map<String, Object> result = new LinkedHashMap<>(order);
        return result;
    }

    public CrmLead require(Long id) {
        CrmLead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "线索不存在");
        }
        return lead;
    }

    private String mask(String encrypted) {
        try {
            return DesensitizeUtil.phone(secretCipher.decrypt(encrypted));
        } catch (Exception e) {
            return encrypted;
        }
    }
}
