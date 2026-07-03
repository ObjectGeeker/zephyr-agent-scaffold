package com.object.ai.file.client.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
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
import com.object.ai.file.utils.FileValidationUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocalFileStorageClient implements FileStorageClient {

    private static final String LOCAL_BUCKET = "local";

    private static final String DEFAULT_BIZ_TYPE = "common";

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageConfigProperties properties;

    @Override
    public String storageType() {
        return FileStorageEnum.local.name();
    }

    @Override
    public FileVO put(MultipartFile file, FileUploadRequestVO request) {
        String originName = normalizeFileName(file == null ? null : file.getOriginalFilename());
        try {
            String contentType = FileValidationUtil.validate(file, originName, properties);
            String objectKey = buildObjectKey(request, originName);
            Path targetPath = resolveObjectPath(objectKey);
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
            return buildFileVO(request, objectKey, targetPath, originName, file.getSize(), contentType);
        } catch (IOException e) {
            log.error("上传本地文件失败，originName={}", originName, e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "上传文件失败");
        }
    }

    @Override
    public FileVO put(File file, FileUploadRequestVO request) {
        String originName = normalizeFileName(file == null ? null : file.getName());
        try {
            String contentType = FileValidationUtil.validate(file, properties);
            String objectKey = buildObjectKey(request, originName);
            Path targetPath = resolveObjectPath(objectKey);
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.toPath(), targetPath);
            return buildFileVO(request, objectKey, targetPath, originName, file.length(), contentType);
        } catch (IOException e) {
            log.error("上传本地文件失败，file={}", file.getAbsolutePath(), e);
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

    private void removeInternal(String bucket, String objectKey) {
        try {
            Files.deleteIfExists(resolveObjectPath(objectKey));
        } catch (IOException e) {
            log.error("删除本地文件失败，objectKey={}", objectKey, e);
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "删除文件失败");
        }
    }

    @Override
    public void writeToResponse(FilePO filePO, HttpServletResponse response) {
        Path targetPath = resolveObjectPath(filePO.getObjectKey());
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new BusinessException(BizErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        String contentType = StrUtil.blankToDefault(filePO.getContentType(), "application/octet-stream");
        String filename = StrUtil.blankToDefault(filePO.getOriginName(), filePO.getFileName());
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFilename);
        try {
            response.setContentLengthLong(Files.size(targetPath));
            Files.copy(targetPath, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new BusinessException(BizErrorCode.OPERATION_ERROR, "读取文件失败");
        }
    }

    private FileVO buildFileVO(FileUploadRequestVO request, String objectKey, Path targetPath, String originName,
                              long fileSize, String contentType) throws IOException {
        return FileVO.builder()
                .storageType(storageType())
                .bucket(LOCAL_BUCKET)
                .objectKey(objectKey)
                .fileName(targetPath.getFileName().toString())
                .originName(originName)
                .fileSize(fileSize)
                .contentType(contentType)
                .fileHash(calculateSha256(targetPath))
                .userId(StpUtil.isLogin() ? StpUtil.getLoginIdAsString() : null)
                .bizType(request == null ? null : request.getBizType())
                .bizId(request == null ? null : request.getBizId())
                .status(FileStatusEnum.SUCCESS)
                .isTemp(false)
                .build();
    }

    private String buildObjectKey(FileUploadRequestVO request, String originName) {
        String bizType = request == null ? null : request.getBizType();
        String normalizedBizType = normalizePathSegment(StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE));
        String datePath = DATE_PATH_FORMATTER.format(LocalDate.now());
        String extension = getExtension(originName);
        return normalizedBizType + "/" + datePath + "/" + IdUtil.fastSimpleUUID() + extension;
    }

    private Path resolveObjectPath(String objectKey) {
        Path storageRoot = resolveStorageRoot();
        Path targetPath = storageRoot.resolve(objectKey).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "文件路径非法");
        }
        return targetPath;
    }

    private String normalizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "unknown";
        }
        return Paths.get(fileName).getFileName().toString();
    }

    private String normalizePathSegment(String segment) {
        String normalized = segment.replaceAll("[^a-zA-Z0-9_-]", "_");
        return StrUtil.blankToDefault(normalized, DEFAULT_BIZ_TYPE);
    }

    private String getExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex);
    }

    private Path resolveStorageRoot() {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        return userDir.resolve(properties.getLocal().getRootPath()).normalize();
    }

    private String calculateSha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
