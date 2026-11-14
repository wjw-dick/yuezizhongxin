package com.yzz.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.api.LoginVO;
import com.yzz.common.tenant.TenantContext;
import com.yzz.common.tenant.TenantState;
import com.yzz.common.tenant.TenantStatusProvider;
import com.yzz.common.util.TokenUtil;
import com.yzz.system.dto.TenantLoginRequest;
import com.yzz.system.entity.SysRole;
import com.yzz.system.entity.SysUser;
import com.yzz.system.mapper.SysRoleMapper;
import com.yzz.system.mapper.SysUserMapper;
import com.yzz.tenant.entity.PlatformTenant;
import com.yzz.tenant.mapper.PlatformTenantMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 租户后台登录。
 *
 * <p>链路：解析租户标识 → 校验租户五态 → 写入租户上下文 → 校验账号口令 →
 * 签发带 tenant_id 的令牌。令牌是唯一的租户授权来源，前端传参不做授权依据。</p>
 */
@Service
public class TenantAuthService {

    private final PlatformTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;
    private final TenantStatusProvider statusProvider;

    public TenantAuthService(PlatformTenantMapper tenantMapper, SysUserMapper userMapper,
                             SysRoleMapper roleMapper, PasswordEncoder passwordEncoder,
                             TokenUtil tokenUtil, TenantStatusProvider statusProvider) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
        this.statusProvider = statusProvider;
    }

    public LoginVO login(TenantLoginRequest request) {
        String tenantCode = request.tenantCode() != null && !request.tenantCode().isBlank()
                ? request.tenantCode().trim()
                : TenantContext.getTenantCode();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "请提供租户标识（子域名或请求头 X-Tenant-Code）");
        }
        TenantState state = statusProvider.stateOf(tenantCode);
        if (state == TenantState.UNKNOWN) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
        if (state == TenantState.DISABLED) {
            throw BusinessException.of(ErrorCode.TENANT_DISABLED);
        }
        if (state == TenantState.EXPIRED) {
            throw BusinessException.of(ErrorCode.TENANT_EXPIRED);
        }

        PlatformTenant tenant = tenantMapper.selectOne(Wrappers.<PlatformTenant>lambdaQuery()
                .eq(PlatformTenant::getTenantCode, tenantCode)
                .last("limit 1"));
        if (tenant == null) {
            throw BusinessException.of(ErrorCode.TENANT_NOT_FOUND);
        }
        // 登录即绑定租户上下文，后续查询自动带 tenant_id 条件
        TenantContext.initTenant(tenant.getId(), tenantCode);

        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.of(ErrorCode.BAD_CREDENTIALS);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已停用，请联系租户管理员");
        }

        String roleCode = null;
        String roleName = null;
        if (user.getRoleId() != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                roleCode = role.getRoleCode();
                roleName = role.getRoleName();
            }
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        String token = tokenUtil.issue(user.getId(), tenant.getId(), tenantCode, false, roleCode);
        return new LoginVO(token, user.getUsername(), user.getRealName(), roleCode, roleName, false, tenantCode);
    }
}
