package com.object.ai.file.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.client.FileStorageClient;
import com.object.ai.file.factory.FileStorageClientFactory;
import com.object.ai.file.mapper.FileMapper;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import com.object.ai.file.support.FilePermissionChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 多模态内容装配服务：将文件 id 列表转换为可供大模型访问的 {@link Media} 列表。
 *
 * @author object
 */
@Service
@RequiredArgsConstructor
public class MultiModalMediaService {

    private final FileMapper fileMapper;

    private final FileStorageClientFactory fileStorageClientFactory;

    private final FilePermissionChecker filePermissionChecker;

    private final FileStorageConfigProperties properties;

    /**
     * 根据文件 id 列表加载多模态内容。
     * <p>
     * 会校验当前登录用户对文件的访问权限，并按存储类型将文件解析为 {@link Media}。
     *
     * @param fileIds 文件 id 列表
     * @return 多模态内容列表；入参为空时返回空列表
     */
    public List<Media> loadMediaByFileIds(List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return List.of();
        }
        List<Media> mediaList = new ArrayList<>(fileIds.size());
        for (String fileId : fileIds) {
            if (StrUtil.isBlank(fileId)) {
                continue;
            }
            FilePO filePO = fileMapper.selectById(fileId);
            if (filePO == null) {
                throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }
            filePermissionChecker.checkReadPermission(filePO);
            validateMimeType(filePO);
            FileStorageClient client = fileStorageClientFactory.getClient(filePO.getStorageType());
            mediaList.add(client.resolveMedia(filePO));
        }
        return mediaList;
    }

    private void validateMimeType(FilePO filePO) {
        List<String> allowedMimeTypes = properties.getAllowedMimeTypes();
        if (CollUtil.isEmpty(allowedMimeTypes)) {
            return;
        }
        if (!allowedMimeTypes.contains(filePO.getContentType())) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "不支持的多模态文件类型");
        }
    }
}
