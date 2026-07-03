package com.object.ai.auth.user.controller;

import com.object.ai.auth.user.model.request.UserLoginDTO;
import com.object.ai.auth.user.model.request.UserRegisterDTO;
import com.object.ai.auth.user.model.request.WechatOpenidLoginDTO;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.auth.user.service.UserAuthService;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@Tag(name = "用户认证接口")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/register")
    public BaseResponse<UserVO> register(@RequestBody UserRegisterDTO request) {
        return ResultUtil.ok(userAuthService.register(request));
    }

    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginDTO request) {
        return ResultUtil.ok(userAuthService.login(request));
    }

    @PostMapping("/wechat/login-or-register")
    public BaseResponse<UserVO> loginOrRegisterByWxOpenid(@RequestBody WechatOpenidLoginDTO request) {
        return ResultUtil.ok(userAuthService.loginOrRegisterByWxOpenid(request.getWxOpenid()));
    }

}
