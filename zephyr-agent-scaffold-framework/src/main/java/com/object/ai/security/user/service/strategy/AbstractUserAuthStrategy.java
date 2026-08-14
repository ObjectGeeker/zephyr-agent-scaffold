package com.object.ai.security.user.service.strategy;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.object.ai.common.base.BizException;
import com.object.ai.common.base.ErrorCode;
import com.object.ai.security.user.mapper.UserMapper;
import com.object.ai.security.user.model.po.UserPO;
import com.object.ai.security.user.model.request.UserLoginRequest;
import com.object.ai.security.user.model.request.UserRegisterRequest;
import com.object.ai.security.user.model.response.UserAuthResult;
import com.object.ai.security.user.model.vo.UserVO;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 用户认证策略抽象基类
 *
 * <p>通过模板方法统一处理参数校验、登录态创建、最后登录时间更新和结果组装，
 * 子类只负责不同认证渠道的凭证查询、校验和用户创建。</p>
 */
public abstract class AbstractUserAuthStrategy implements UserAuthStrategy {

    protected final UserMapper userMapper;

    protected AbstractUserAuthStrategy(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public final UserAuthResult login(UserLoginRequest request) {
        validateLoginRequest(request);
        UserPO user = authenticate(request);
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw BizException.of(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }

        updateLastLoginTime(user);
        return createLoginResult(user);
    }

    @Override
    public final UserAuthResult register(UserRegisterRequest request) {
        validateRegisterRequest(request);
        UserPO user = registerUser(request);
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "用户注册失败");
        }

        updateLastLoginTime(user);
        return createLoginResult(user);
    }

    /**
     * 校验登录请求的公共字段
     *
     * @param request 登录请求
     */
    protected void validateLoginRequest(UserLoginRequest request) {
        if (request == null) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "登录请求不能为空");
        }
        requireText(request.getLoginType(), "登录类型不能为空");
        requireText(request.getPassword(), "密码不能为空");
    }

    /**
     * 校验注册请求的公共字段
     *
     * @param request 注册请求
     */
    protected void validateRegisterRequest(UserRegisterRequest request) {
        if (request == null) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "注册请求不能为空");
        }
        requireText(request.getRegisterType(), "注册类型不能为空");
        requireText(request.getUsername(), "用户名不能为空");
        requireText(request.getPassword(), "密码不能为空");
    }

    /**
     * 校验文本字段
     *
     * @param value 字段值
     * @param message 错误消息
     */
    protected void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw BizException.of(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 更新最后登录时间
     *
     * @param user 用户
     */
    protected void updateLastLoginTime(UserPO user) {
        LocalDateTime loginTime = LocalDateTime.now();
        user.setLastLoginTime(loginTime);

        UserPO updateUser = new UserPO();
        updateUser.setId(user.getId());
        updateUser.setLastLoginTime(loginTime);
        userMapper.updateById(updateUser);
    }

    /**
     * 创建登录态并组装统一认证结果
     *
     * @param user 用户
     * @return 认证结果
     */
    protected UserAuthResult createLoginResult(UserPO user) {
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        return UserAuthResult.builder()
                .user(toUserVO(user))
                .tokenName(tokenInfo.getTokenName())
                .tokenValue(tokenInfo.getTokenValue())
                .build();
    }

    /**
     * 转换为用户视图对象
     *
     * @param user 用户持久化对象
     * @return 用户视图对象
     */
    protected UserVO toUserVO(UserPO user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setCreateUser(user.getCreateUser());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateUser(user.getUpdateUser());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setIsDelete(user.getIsDelete());
        vo.setUsername(user.getUsername());
        vo.setAccount(user.getAccount());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setWxOpenId(user.getWxOpenId());
        vo.setUserRoles(user.getUserRoles() == null ? new ArrayList<>() : new ArrayList<>(user.getUserRoles()));
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }
}
