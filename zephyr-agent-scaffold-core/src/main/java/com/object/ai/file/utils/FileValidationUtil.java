package com.object.ai.file.utils;

import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileValidationUtil {

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
}
