package com.object.ai.file.client.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.client.FileStorageClient;
import com.object.ai.file.model.enums.FileStorageEnum;
import com.object.ai.file.model.enums.FileStatusEnum;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import com.object.ai.file.model.request.FileUploadRequestVO;
import com.object.ai.file.model.vo.FileVO;
import com.object.ai.file.utils.FileStorageUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "file.storage.config", name = "storage-type", havingValue = "minio")
public class MinioFileStorageClient implements FileStorageClient {

    private final FileStorageConfigProperties properties;

    private final MinioClient minioClient;

    @Override
    public String storageType() {
        return FileStorageEnum.minio.name();
    }

    @Override
    public FileVO put(MultipartFile file, FileUploadRequestVO request) {
        String originName = FileStorageUtil.normalizeFileName(file == null ? null : file.getOriginalFilename());
        try {
            String contentType = FileStorageUtil.validate(file, originName, properties);
            String bucketName = getBucketName();
            ensureBucketExists(bucketName);
            String objectKey = FileStorageUtil.buildObjectKey(request, originName);
            String fileHash;
            try (InputStream inputStream = file.getInputStream()) {
                fileHash = calculateSha256(inputStream);
            }
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType)
                        .build());
            }
            return buildFileVO(request, bucketName, objectKey, originName, file.getSize(), contentType, fileHash);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传 MinIO 文件失败，originName={}", originName, e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "上传文件失败");
        }
    }

    @Override
    public FileVO put(File file, FileUploadRequestVO request) {
        String originName = FileStorageUtil.normalizeFileName(file == null ? null : file.getName());
        try {
            String contentType = FileStorageUtil.validate(file, properties);
            String bucketName = getBucketName();
            ensureBucketExists(bucketName);
            String objectKey = FileStorageUtil.buildObjectKey(request, originName);
            String fileHash;
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                fileHash = calculateSha256(inputStream);
            }
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .stream(inputStream, file.length(), -1)
                        .contentType(contentType)
                        .build());
            }
            return buildFileVO(request, bucketName, objectKey, originName, file.length(), contentType, fileHash);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传 MinIO 文件失败，file={}", file == null ? null : file.getAbsolutePath(), e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "上传文件失败");
        }
    }

    @Override
    public void remove(String bucket, String objectKey) {
        removeInternal(bucket, objectKey);
    }

    @Override
    public void remove(String objectKey) {
        removeInternal(null, objectKey);
    }

    @Override
    public void writeToResponse(FilePO filePO, HttpServletResponse response) {
        if (filePO == null || StrUtil.isBlank(filePO.getObjectKey())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件信息不能为空");
        }
        String bucketName = resolveBucketName(filePO.getBucket());
        String objectKey = filePO.getObjectKey();
        try {
            StatObjectResponse statObject = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            FileStorageUtil.prepareInlineResponse(filePO, response, statObject.size());
            try (GetObjectResponse objectResponse = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build())) {
                objectResponse.transferTo(response.getOutputStream());
            }
            response.flushBuffer();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            if (isObjectNotFound(e)) {
                throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }
            log.error("读取 MinIO 文件失败，bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "读取文件失败");
        }
    }

    private void removeInternal(String bucket, String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "文件路径不能为空");
        }
        String bucketName = resolveBucketName(bucket);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除 MinIO 文件失败，bucket={}, objectKey={}", bucketName, objectKey, e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除文件失败");
        }
    }

    private FileVO buildFileVO(FileUploadRequestVO request, String bucketName, String objectKey, String originName,
                               long fileSize, String contentType, String fileHash) {
        return FileVO.builder()
                .storageType(storageType())
                .bucket(bucketName)
                .objectKey(objectKey)
                .fileName(getStoredFileName(objectKey))
                .originName(originName)
                .fileSize(fileSize)
                .contentType(contentType)
                .fileHash(fileHash)
                .userId(StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null)
                .bizType(request == null ? null : request.getBizType())
                .bizId(request == null ? null : request.getBizId())
                .status(FileStatusEnum.SUCCESS)
                .isTemp(false)
                .build();
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private String getBucketName() {
        FileStorageConfigProperties.Minio minio = properties.getMinio();
        if (minio == null || StrUtil.isBlank(minio.getBucketName())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "MinIO 存储桶不能为空");
        }
        return minio.getBucketName();
    }

    private String resolveBucketName(String bucket) {
        if (StrUtil.isNotBlank(bucket)) {
            return bucket;
        }
        return getBucketName();
    }

    private String getStoredFileName(String objectKey) {
        int fileNameIndex = objectKey.lastIndexOf('/');
        if (fileNameIndex < 0 || fileNameIndex == objectKey.length() - 1) {
            return objectKey;
        }
        return objectKey.substring(fileNameIndex + 1);
    }

    private String calculateSha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private boolean isObjectNotFound(Exception e) {
        return e instanceof ErrorResponseException errorResponseException
                && "NoSuchKey".equals(errorResponseException.errorResponse().code());
    }
}
