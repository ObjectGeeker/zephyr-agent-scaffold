package com.object.ai.file.config;

import com.object.ai.file.model.properties.FileStorageConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {FileStorageConfigProperties.class})
public class FileStorageConfiguration {
}
