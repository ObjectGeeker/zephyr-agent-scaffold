package com.object.ai.file.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "file.storage.config")
@Data
public class FileStorageConfigProperties {

    /**
     * 存储类型
     */
    private String storageType;

    /**
     * 最大支持的上传文件大小
     */
    private Long maxSize = 10 * 1024 * 1024L;

    /**
     * 支持上传的mime类型
     */
    private List<String> allowedMimeTypes = new ArrayList<>();

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    private Minio minio = new Minio();

    /**
     * 临时文件策略配置
     */
    private TempClean tempClean = new TempClean();

    @Data
    public static class Local {
        /**
         * 本地存储根目录，相对项目根目录
         */
        private String rootPath = "data/files";
    }

    @Data
    public static class Minio {
        /**
         * Minio地址
         */
        private String endpoint;
        /**
         * 账号
         */
        private String accessKey;
        /**
         * 密码
         */
        private String secretKey;
        /**
         * 存储桶
         */
        private String bucketName;
    }

    @Data
    public static class TempClean {
        /**
         * 是否启用临时文件清理任务
         */
        private Boolean enable = false;
        /**
         * 临时文件过期天数
         */
        private Integer expireDays;
    }

}
