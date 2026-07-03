package com.object.ai.auth.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.auth.user.model.request.UserQueryDTO;
import com.object.ai.auth.user.model.request.UserStatusUpdateDTO;
import com.object.ai.auth.user.model.request.UserUpdateDTO;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.auth.user.service.UserManagerService;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user/manager")
@Tag(name = "用户管理接口")
@RequiredArgsConstructor
@SaCheckLogin
@SaCheckRole(value = {"ADMIN"})
public class UserManagerController {

    private final UserManagerService userManagerService;

    @Operation(summary = "查询所有用户")
    @PostMapping("findAllUser")
    public BaseResponse<Page<UserVO>> findAllUser(@RequestBody PageRequest<UserQueryDTO> pageRequest) {
        return ResultUtil.ok(userManagerService.findAllUser(pageRequest));
    }

    @Operation(summary = "更新用户信息")
    @PostMapping("batchSave")
    public BaseResponse<Boolean> batchSave(@RequestBody DataContainer<UserUpdateDTO> dataContainer) {
        return ResultUtil.ok(userManagerService.batchSave(dataContainer));
    }

    @Operation(summary = "更新用户状态")
    @PostMapping("changeStatus")
    public BaseResponse<Boolean> changeStatus(@RequestBody UserStatusUpdateDTO request) {
        return ResultUtil.ok(userManagerService.changeStatus(request));
    }

}
