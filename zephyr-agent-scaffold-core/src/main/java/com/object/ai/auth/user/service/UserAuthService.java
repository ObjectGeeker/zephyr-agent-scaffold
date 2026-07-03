package com.object.ai.auth.user.service;

import com.object.ai.auth.user.model.request.UserLoginDTO;
import com.object.ai.auth.user.model.request.UserRegisterDTO;
import com.object.ai.auth.user.model.vo.UserVO;

/**
 * 用户认证服务
 */
public interface UserAuthService {

    /**
     * 按微信 openid 登录或注册（后续 Sa-Token 接入时实现）
     */
    UserVO loginOrRegisterByWxOpenid(String wxOpenid);

    UserVO register(UserRegisterDTO request);

    UserVO login(UserLoginDTO request);

}
