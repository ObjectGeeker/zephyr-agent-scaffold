package com.object.ai.file.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import com.object.ai.file.model.request.FileUploadRequestVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileStorageUtil {

    private static final String DEFAULT_BIZ_TYPE = "common";

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final Tika TIKA = new Tika();

    public static String validate(MultipartFile file, String fileName, FileStorageConfigProperties properties)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "上传文件不能为空");
        }
        String contentType = TIKA.detect(file.getInputStream(), fileName);
        validate(file.getSize(), contentType, properties);
        return contentType;
    }

    public static String validate(File file, FileStorageConfigProperties properties) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "上传文件不能为空");
        }
        String contentType = TIKA.detect(file);
        validate(file.length(), contentType, properties);
        return contentType;
    }

    public static void validate(long fileSize, String contentType, FileStorageConfigProperties properties) {
        if (properties != null && properties.getMaxSize() != null && fileSize > properties.getMaxSize()) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "文件大小超过限制");
        }
        List<String> allowedMimeTypes = properties == null ? null : properties.getAllowedMimeTypes();
        if (allowedMimeTypes != null && !allowedMimeTypes.isEmpty() && !allowedMimeTypes.contains(contentType)) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "不支持的文件类型");
        }
    }

    public static void prepareInlineResponse(FilePO filePO, HttpServletResponse response, long contentLength) {
        String contentType = StrUtil.blankToDefault(filePO.getContentType(), "application/octet-stream");
        String filename = StrUtil.blankToDefault(filePO.getOriginName(), filePO.getFileName());
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFilename);
        response.setContentLengthLong(contentLength);
    }

    public static String buildObjectKey(FileUploadRequestVO request, String originName) {
        String bizType = request == null ? null : request.getBizType();
        String normalizedBizType = normalizePathSegment(StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE));
        String datePath = DATE_PATH_FORMATTER.format(LocalDate.now());
        String extension = getExtension(originName);
        return normalizedBizType + "/" + datePath + "/" + IdUtil.fastSimpleUUID() + extension;
    }

    public static String normalizeFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "unknown";
        }
        return Paths.get(fileName).getFileName().toString();
    }

    private static String normalizePathSegment(String segment) {
        String normalized = segment.replaceAll("[^a-zA-Z0-9_-]", "_");
        return StrUtil.blankToDefault(normalized, DEFAULT_BIZ_TYPE);
    }

    private static String getExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionIndex);
    }
}
