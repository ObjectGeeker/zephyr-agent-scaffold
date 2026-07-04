package com.object.ai.file.config;

import cn.hutool.core.util.StrUtil;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {FileStorageConfigProperties.class})
public class FileStorageConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "file.storage.config", name = "storage-type", havingValue = "minio")
    public MinioClient minioClient(FileStorageConfigProperties properties) {
        FileStorageConfigProperties.Minio minio = properties.getMinio();
        if (minio == null || StrUtil.hasBlank(minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey())) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "MinIO 配置不能为空");
        }
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }
}
