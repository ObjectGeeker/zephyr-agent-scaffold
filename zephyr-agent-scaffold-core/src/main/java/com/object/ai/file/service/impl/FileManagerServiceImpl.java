package com.object.ai.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;
import com.object.ai.file.client.FileStorageClient;
import com.object.ai.file.factory.FileStorageClientFactory;
import com.object.ai.file.mapper.FileMapper;
import com.object.ai.file.model.enums.FileStatusEnum;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import com.object.ai.file.model.request.FileQueryDTO;
import com.object.ai.file.model.request.FileUpdateDTO;
import com.object.ai.file.model.request.FileUploadRequestVO;
import com.object.ai.file.model.vo.FileVO;
import com.object.ai.file.service.FileManagerService;
import com.object.ai.file.support.FilePermissionChecker;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FileManagerServiceImpl implements FileManagerService {

    private final FileMapper fileMapper;

    private final FileStorageClientFactory fileStorageClientFactory;

    private final FileStorageConfigProperties properties;

    private final FilePermissionChecker filePermissionChecker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(MultipartFile file, FileUploadRequestVO request) {
        return upload(file, request, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(MultipartFile file, FileUploadRequestVO request, Boolean isTemp) {
        FileStorageClient client = fileStorageClientFactory.getClient(properties.getStorageType());
        FileVO fileVO = client.put(file, request);
        FilePO filePO = fileVO.toFilePO();
        LocalDateTime now = LocalDateTime.now();
        String currentUserId = StpUtil.getLoginIdAsString();

        filePO.setId(IdUtil.fastSimpleUUID());
        filePO.setStorageType(client.storageType());
        filePO.setStatus(FileStatusEnum.SUCCESS.name());
        filePO.setIsTemp(Boolean.TRUE.equals(isTemp));
        filePO.setUserId(currentUserId);
        filePO.setCreateBy(currentUserId);
        filePO.setUpdateBy(currentUserId);
        filePO.setCreateTime(now);
        filePO.setUpdateTime(now);
        filePO.setDeleted(false);

        int affectedRows = fileMapper.insert(filePO);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "保存文件记录失败");
        }
        return filePO.getId();
    }

    @Override
    public Page<FileVO> findAllFile(PageRequest<FileQueryDTO> pageRequest) {
        if (pageRequest == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "分页参数不能为空");
        }
        if (pageRequest.getPageIndex() < 1 || pageRequest.getPageSize() < 1) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "分页参数错误");
        }

        LambdaQueryWrapper<FilePO> wrapper = buildQueryWrapper(pageRequest.getFilterInfo());
        Page<FilePO> filePage = fileMapper.selectPage(new Page<>(pageRequest.getPageIndex(), pageRequest.getPageSize()), wrapper);
        Page<FileVO> resultPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        resultPage.setRecords(filePage.getRecords().stream().map(FilePO::toFileVO).toList());
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSave(DataContainer<FileUpdateDTO> dataContainer) {
        if (dataContainer == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件数据不能为空");
        }
        if (CollUtil.isNotEmpty(dataContainer.getAddedData())) {
            for (FileUpdateDTO request : dataContainer.getAddedData()) {
                createFileInfo(request);
            }
        }
        if (CollUtil.isNotEmpty(dataContainer.getModifyData())) {
            for (FileUpdateDTO request : dataContainer.getModifyData()) {
                updateFileInfo(request);
            }
        }
        if (CollUtil.isNotEmpty(dataContainer.getRemoveData())) {
            for (FileUpdateDTO request : dataContainer.getRemoveData()) {
                removeFile(request);
            }
        }
        return true;
    }

    @Override
    public void writeFileToResponse(String fileId, HttpServletResponse response) {
        if (StrUtil.isBlank(fileId)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件 id 不能为空");
        }
        FilePO filePO = getExistingFile(fileId);
        FileStorageClient client = fileStorageClientFactory.getClient(filePO.getStorageType());
        client.writeToResponse(filePO, response);
    }

    private LambdaQueryWrapper<FilePO> buildQueryWrapper(FileQueryDTO query) {
        LambdaQueryWrapper<FilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FilePO::getCreateTime);
        if (query == null) {
            return wrapper;
        }

        wrapper.eq(StrUtil.isNotBlank(query.getStorageType()), FilePO::getStorageType, query.getStorageType());
        wrapper.eq(StrUtil.isNotBlank(query.getBucket()), FilePO::getBucket, query.getBucket());
        wrapper.like(StrUtil.isNotBlank(query.getObjectKey()), FilePO::getObjectKey, query.getObjectKey());
        wrapper.like(StrUtil.isNotBlank(query.getOriginName()), FilePO::getOriginName, query.getOriginName());
        wrapper.eq(StrUtil.isNotBlank(query.getUserId()), FilePO::getUserId, query.getUserId());
        wrapper.eq(StrUtil.isNotBlank(query.getBizType()), FilePO::getBizType, query.getBizType());
        wrapper.eq(StrUtil.isNotBlank(query.getBizId()), FilePO::getBizId, query.getBizId());
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(FilePO::getStatus, parseStatus(query.getStatus()).name());
        }
        wrapper.eq(query.getIsTemp() != null, FilePO::getIsTemp, query.getIsTemp());
        return wrapper;
    }

    private void createFileInfo(FileUpdateDTO request) {
        String fileId = getRequiredFileId(request);
        FilePO filePO = getExistingFile(fileId);
        checkTempFile(filePO);
        updateFileInfoFields(filePO, request);
    }

    private void updateFileInfo(FileUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件 id 不能为空");
        }
        FilePO existFile = getExistingFile(request.getId());
        if (StrUtil.isBlank(request.getFileId())) {
            updateFileInfoFields(existFile, request);
            return;
        }
        if (StrUtil.equals(request.getId(), request.getFileId())) {
            updateFileInfoFields(existFile, request);
            return;
        }

        FilePO newFile = getExistingFile(request.getFileId());
        checkTempFile(newFile);
        updateFileInfoFields(newFile, request);
        removePhysicalFile(existFile);
        int affectedRows = fileMapper.deleteById(existFile.getId());
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除旧文件记录失败");
        }
    }

    private void removeFile(FileUpdateDTO request) {
        if (request == null || StrUtil.isBlank(request.getId())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件 id 不能为空");
        }
        FilePO filePO = getExistingFile(request.getId());
        filePermissionChecker.checkDeletePermission(filePO.getBucket(), filePO.getObjectKey());
        removePhysicalFile(filePO);
        int affectedRows = fileMapper.deleteById(filePO.getId());
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除文件记录失败");
        }
    }

    private void updateFileInfoFields(FilePO existFile, FileUpdateDTO request) {
        FilePO updateFile = new FilePO();
        updateFile.setId(existFile.getId());
        updateFile.setOriginName(request.getOriginName());
        updateFile.setUserId(StpUtil.getLoginIdAsString());
        updateFile.setBizType(request.getBizType());
        updateFile.setIsTemp(false);
        updateFile.setUpdateBy(StpUtil.getLoginIdAsString());
        updateFile.setUpdateTime(LocalDateTime.now());

        int affectedRows = fileMapper.updateById(updateFile);
        if (affectedRows <= 0) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "更新文件信息失败");
        }
    }

    private void removePhysicalFile(FilePO filePO) {
        FileStorageClient client = fileStorageClientFactory.getClient(filePO.getStorageType());
        client.remove(filePO.getBucket(), filePO.getObjectKey());
    }

    private FilePO getExistingFile(String id) {
        FilePO filePO = fileMapper.selectById(id);
        if (filePO == null) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        return filePO;
    }

    private void checkTempFile(FilePO filePO) {
        if (!Boolean.TRUE.equals(filePO.getIsTemp())) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "文件不是临时文件");
        }
    }

    private String getRequiredFileId(FileUpdateDTO request) {
        if (request == null) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件 id 不能为空");
        }
        String fileId = StrUtil.blankToDefault(request.getFileId(), request.getId());
        if (StrUtil.isBlank(fileId)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件 id 不能为空");
        }
        return fileId;
    }

    private FileStatusEnum parseStatus(String status) {
        try {
            return FileStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "文件状态错误");
        }
    }
}
