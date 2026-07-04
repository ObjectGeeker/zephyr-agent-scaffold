package com.object.ai.file.support;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.object.ai.auth.user.model.enums.UserRoleEnum;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.mapper.FileMapper;
import com.object.ai.file.model.po.FilePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilePermissionChecker {

    private final FileMapper fileMapper;

    public void checkDeletePermission(String bucket, String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件路径不能为空");
        }
        StpUtil.checkLogin();
        FilePO filePO = findFile(bucket, objectKey);
        if (filePO == null) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        String currentUserId = StpUtil.getLoginIdAsString();
        boolean isOwner = StrUtil.equals(currentUserId, filePO.getUserId());
        boolean isAdmin = StpUtil.hasRole(UserRoleEnum.ADMIN.name());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(BizErrorCode.NO_PERMISSION_ERROR, "无权限删除该文件");
        }
    }

    public void checkReadPermission(FilePO filePO) {
        if (filePO == null) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        StpUtil.checkLogin();
        String currentUserId = StpUtil.getLoginIdAsString();
        boolean isOwner = StrUtil.equals(currentUserId, filePO.getUserId());
        boolean isAdmin = StpUtil.hasRole(UserRoleEnum.ADMIN.name());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(BizErrorCode.NO_PERMISSION_ERROR, "无权限访问该文件");
        }
    }

    private FilePO findFile(String bucket, String objectKey) {
        LambdaQueryWrapper<FilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FilePO::getObjectKey, objectKey);
        wrapper.eq(StrUtil.isNotBlank(bucket), FilePO::getBucket, bucket);
        wrapper.last("limit 1");
        return fileMapper.selectOne(wrapper);
    }
}
