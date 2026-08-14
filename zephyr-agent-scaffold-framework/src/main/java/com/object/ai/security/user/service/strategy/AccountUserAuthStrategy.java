package com.object.ai.security.user.service.strategy;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.object.ai.common.base.BizException;
import com.object.ai.common.base.ErrorCode;
import com.object.ai.security.user.mapper.UserMapper;
import com.object.ai.security.user.model.enums.UserAuthTypeEnum;
import com.object.ai.security.user.model.po.UserPO;
import com.object.ai.security.user.model.request.UserLoginRequest;
import com.object.ai.security.user.model.request.UserRegisterRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 账号认证策略
 */
@Service
public class AccountUserAuthStrategy extends AbstractUserAuthStrategy {

    public AccountUserAuthStrategy(UserMapper userMapper) {
        super(userMapper);
    }

    @Override
    public boolean supportsLogin(String loginType) {
        return UserAuthTypeEnum.ACCOUNT.matches(loginType);
    }

    @Override
    public boolean supportsRegister(String registerType) {
        return UserAuthTypeEnum.ACCOUNT.matches(registerType);
    }

    @Override
    protected void validateLoginRequest(UserLoginRequest request) {
        super.validateLoginRequest(request);
        requireText(request.getAccount(), "账号不能为空");
    }

    @Override
    protected void validateRegisterRequest(UserRegisterRequest request) {
        super.validateRegisterRequest(request);
        requireText(request.getAccount(), "账号不能为空");
    }

    @Override
    public UserPO authenticate(UserLoginRequest request) {
        UserPO user = userMapper.selectOne(Wrappers.<UserPO>lambdaQuery()
                .eq(UserPO::getAccount, request.getAccount()));
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw BizException.of(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return user;
    }

    @Override
    public UserPO registerUser(UserRegisterRequest request) {
        Long accountCount = userMapper.selectCount(Wrappers.<UserPO>lambdaQuery()
                .eq(UserPO::getAccount, request.getAccount()));
        if (accountCount != null && accountCount > 0) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "账号已存在");
        }

        UserPO user = new UserPO();
        user.setUsername(request.getUsername());
        user.setAccount(request.getAccount());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setUserRoles(new java.util.ArrayList<>());
        int rows = userMapper.insert(user);
        if (rows != 1) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }
        return user;
    }
}
