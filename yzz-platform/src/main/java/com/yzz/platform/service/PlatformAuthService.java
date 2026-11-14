package com.yzz.platform.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yzz.common.api.BusinessException;
import com.yzz.common.api.ErrorCode;
import com.yzz.common.util.TokenUtil;
import com.yzz.platform.dto.PlatformLoginRequest;
import com.yzz.platform.entity.PlatformUser;
import com.yzz.platform.mapper.PlatformUserMapper;
import com.yzz.platform.support.PlatformRoles;
import com.yzz.common.api.LoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 平台端登录。签发的令牌带 platform 标记，只能访问 /api/platform 下的接口。
 */
@Service
public class PlatformAuthService {

    private final PlatformUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;

    public PlatformAuthService(PlatformUserMapper userMapper, PasswordEncoder passwordEncoder,
                               TokenUtil tokenUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
    }

    public LoginVO login(PlatformLoginRequest request) {
        PlatformUser user = userMapper.selectOne(Wrappers.<PlatformUser>lambdaQuery()
                .eq(PlatformUser::getUsername, request.username())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.of(ErrorCode.BAD_CREDENTIALS);
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "平台账号已停用，请联系平台超级管理员");
        }
        PlatformUser update = new PlatformUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        String token = tokenUtil.issue(user.getId(), null, null, true, user.getRoleCode());
        return new LoginVO(token, user.getUsername(), user.getRealName(),
                user.getRoleCode(), PlatformRoles.nameOf(user.getRoleCode()), true, null);
    }
}
