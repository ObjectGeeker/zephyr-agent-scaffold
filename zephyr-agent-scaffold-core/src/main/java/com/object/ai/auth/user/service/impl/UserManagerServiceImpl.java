package com.object.ai.auth.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.auth.user.mapper.UserMapper;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.auth.user.model.enums.UserStatusEnum;
import com.object.ai.auth.user.model.po.UserPO;
import com.object.ai.auth.user.model.request.UserQueryDTO;
import com.object.ai.auth.user.model.request.UserStatusUpdateDTO;
import com.object.ai.auth.user.model.request.UserUpdateDTO;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.auth.user.service.UserManagerService;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理服务实现
 */
@Service
public class UserManagerServiceImpl implements UserManagerService {

    @Resource
    private UserMapper userMapper;

    @Override
    public Page<UserVO> findAllUser(PageRequest<UserQueryDTO> pageRequest) {
        if (pageRequest == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "分页参数不能为空");
        }
        if (pageRequest.getPageIndex() < 1 || pageRequest.getPageSize() < 1) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "分页参数错误");
        }

        LambdaQueryWrapper<UserPO> wrapper = buildQueryWrapper(pageRequest.getFilterInfo());
        Page<UserPO> userPage = userMapper.selectPage(new Page<>(pageRequest.getPageIndex(), pageRequest.getPageSize()), wrapper);
        Page<UserVO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userPage.getRecords().stream().map(this::toSafeUserVO).toList());
        return resultPage;
    }

    @Override
    public Boolean batchSave(DataContainer<UserUpdateDTO> dataContainer) {
        if (dataContainer == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "用户数据不能为空");
        }
        if (CollUtil.isNotEmpty(dataContainer.getAddedData())) {
            throw new BusinessException(BizErrorCode.FORBIDDEN_ERROR, "不支持新增用户");
        }

        if (CollUtil.isNotEmpty(dataContainer.getModifyData())) {
            for (UserUpdateDTO request : dataContainer.getModifyData()) {
                updateUser(request);
            }
        }
        if (CollUtil.isNotEmpty(dataContainer.getRemoveData())) {
            for (UserUpdateDTO request : dataContainer.getRemoveData()) {
                removeUser(request);
            }
        }
        return true;
    }

    @Override
    public Boolean changeStatus(UserStatusUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId()) || StrUtil.isBlank(request.getStatus())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "用户 id 或状态不能为空");
        }

        UserPO existUser = getExistingUser(request.getId());
        UserStatusEnum status = parseStatus(request.getStatus());

        UserPO updateUser = new UserPO();
        updateUser.setId(existUser.getId());
        updateUser.setStatus(status.name());
        updateUser.setUpdateTime(LocalDateTime.now());

        int affectedRows = userMapper.updateById(updateUser);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "更新用户状态失败");
        }
        if (UserStatusEnum.BAN.equals(status)) {
            StpUtil.kickout(existUser.getId());
        }
        return true;
    }

    private LambdaQueryWrapper<UserPO> buildQueryWrapper(UserQueryDTO query) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserPO::getCreateTime);
        if (query == null) {
            return wrapper;
        }

        wrapper.like(StrUtil.isNotBlank(query.getUserAccount()), UserPO::getUserAccount, query.getUserAccount());
        wrapper.like(StrUtil.isNotBlank(query.getUsername()), UserPO::getUsername, query.getUsername());
        wrapper.like(StrUtil.isNotBlank(query.getEmail()), UserPO::getEmail, query.getEmail());
        wrapper.like(StrUtil.isNotBlank(query.getPhone()), UserPO::getPhone, query.getPhone());
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(UserPO::getStatus, parseStatus(query.getStatus()).name());
        }
        if (StrUtil.isNotBlank(query.getRole())) {
            wrapper.like(UserPO::getRoles, parseRole(query.getRole()).name());
        }
        return wrapper;
    }

    private void updateUser(UserUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "用户 id 不能为空");
        }
        UserPO existUser = getExistingUser(request.getId());

        UserPO updateUser = new UserPO();
        updateUser.setId(existUser.getId());
        updateUser.setUsername(request.getUsername());
        updateUser.setEmail(request.getEmail());
        updateUser.setPhone(request.getPhone());
        updateUser.setAvatar(request.getAvatar());
        updateUser.setUpdateTime(LocalDateTime.now());
        if (request.getRoles() != null) {
            updateUser.setRoles(parseRoles(request.getRoles()));
        }

        int affectedRows = userMapper.updateById(updateUser);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "更新用户信息失败");
        }
    }

    private void removeUser(UserUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "用户 id 不能为空");
        }
        UserPO existUser = getExistingUser(request.getId());
        int affectedRows = userMapper.deleteById(existUser.getId());
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除用户失败");
        }
    }

    private UserPO getExistingUser(String id) {
        UserPO userPO = userMapper.selectById(id);
        if (userPO == null) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return userPO;
    }

    private UserVO toSafeUserVO(UserPO userPO) {
        UserVO userVO = userPO.toUserVO();
        userVO.setPassword(null);
        return userVO;
    }

    private List<String> parseRoles(List<String> roles) {
        if (CollUtil.isEmpty(roles)) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "用户角色不能为空");
        }
        return roles.stream()
                .map(this::parseRole)
                .map(UserRoleEnum::name)
                .distinct()
                .toList();
    }

    private UserRoleEnum parseRole(String role) {
        if (StrUtil.isBlank(role)) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "用户角色不能为空");
        }
        try {
            return UserRoleEnum.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "用户角色错误");
        }
    }

    private UserStatusEnum parseStatus(String status) {
        if (StrUtil.isBlank(status)) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "用户状态不能为空");
        }
        try {
            return UserStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "用户状态错误");
        }
    }

}
