package com.object.ai.auth.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.object.ai.auth.user.mapper.UserMapper;
import com.object.ai.auth.user.model.enums.LoginTypeEnum;
import com.object.ai.auth.user.model.enums.RegisterTypeEnum;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.auth.user.model.enums.UserStatusEnum;
import com.object.ai.auth.user.model.po.UserPO;
import com.object.ai.auth.user.model.request.UserLoginDTO;
import com.object.ai.auth.user.model.request.UserRegisterDTO;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.auth.user.service.UserAuthService;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UserAuthServiceImpl implements UserAuthService {

    @Resource
    private UserMapper userMapper;

    @Override
    public UserVO loginOrRegisterByWxOpenid(String wxOpenid) {
        if (StrUtil.isBlank(wxOpenid)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "微信 openid 不能为空");
        }
        //1. 根据openid查询是否存在用户
        UserVO existUser = userMapper.findUserByOpenid(wxOpenid);
        //2. 如果存在直接登录
        if (null != existUser) {
            // 直接登录
            StpUtil.login(existUser.getId());
            // 脱敏
            existUser.setPassword(null);
            return existUser;
        }
        //3. 不存在走注册逻辑，给一个默认账号，默认密码，默认用户名
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setRegisterType(RegisterTypeEnum.account.name());
        registerDTO.setRegisterAccount(UUID.fastUUID().toString());
        registerDTO.setRegisterCertificate(UUID.fastUUID().toString());
        registerDTO.setUsername("微信用户_" + UUID.fastUUID().toString().substring(0, 4));
        registerDTO.setWxOpenid(wxOpenid);
        return register(registerDTO);
    }

    @Override
    public UserVO register(UserRegisterDTO request) {
        //1. 判断注册类型
        String registerType = request.getRegisterType();
        if (RegisterTypeEnum.account.name().equals(registerType)) {
            return registerByAccount(request);
        }
        throw new BusinessException(BizErrorCode.FORBIDDEN_ERROR, "暂时没有实现的注册类型");
    }

    @Override
    public UserVO login(UserLoginDTO request) {
        String loginType = request.getLoginType();
        if (LoginTypeEnum.account.name().equals(loginType)) {
            return loginByAccount(request);
        }
        throw new BusinessException(BizErrorCode.FORBIDDEN_ERROR, "暂时没有实现的登录类型");
    }

    private UserVO loginByAccount(UserLoginDTO request) {
        String loginAccount = request.getLoginAccount();
        String loginCertificate = request.getLoginCertificate();
        if (StrUtil.isBlank(loginAccount) || StrUtil.isBlank(loginCertificate)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "登录账号或登录凭证不能为空");
        }

        UserVO userVO = userMapper.findUserByAccount(loginAccount);
        if (userVO == null || !BCrypt.checkpw(loginCertificate, userVO.getPassword())) {
            throw new BusinessException(BizErrorCode.UNAUTHORIZED_ERROR, "账号或密码错误");
        }

        if (UserStatusEnum.BAN.equals(userVO.getStatus())) {
            throw new BusinessException(BizErrorCode.FORBIDDEN_ERROR, "账号已被封禁");
        }
        StpUtil.checkDisable(userVO.getId());
        StpUtil.login(userVO.getId());
        userVO.setPassword(null);
        return userVO;
    }

    private UserVO registerByAccount(UserRegisterDTO request) {
        String registerAccount = request.getRegisterAccount();
        String registerCertificate = request.getRegisterCertificate();
        String username = request.getUsername();
        if (StrUtil.isBlank(registerAccount) || StrUtil.isBlank(registerCertificate) || StrUtil.isBlank(username)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "注册账号、注册凭证或用户名不能为空");
        }

        UserVO existUser = userMapper.findUserByAccount(registerAccount);
        if (existUser != null) {
            throw new BusinessException(BizErrorCode.DATA_EXIST_ERROR, "账号已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        UserPO userPO = new UserPO();
        userPO.setUserAccount(registerAccount);
        userPO.setWxOpenid(request.getWxOpenid());
        userPO.setPassword(BCrypt.hashpw(registerCertificate));
        userPO.setUsername(username);
        userPO.setRoles(List.of(UserRoleEnum.USER.name()));
        userPO.setStatus(UserStatusEnum.ACTIVE.name());
        userPO.setCreateTime(now);
        userPO.setUpdateTime(now);
        userPO.setDeleted(false);

        int affectedRows = userMapper.insert(userPO);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "注册失败");
        }

        StpUtil.login(userPO.getId());
        UserVO userVO = userPO.toUserVO();
        userVO.setPassword(null);
        return userVO;
    }

}
