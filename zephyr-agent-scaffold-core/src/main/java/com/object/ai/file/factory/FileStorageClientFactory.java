package com.object.ai.file.factory;

import cn.hutool.core.util.StrUtil;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import com.object.ai.file.client.FileStorageClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileStorageClientFactory {

    private final Map<String, FileStorageClient> clientMap = new HashMap<>();

    public FileStorageClientFactory(List<FileStorageClient> clients) {
        for (FileStorageClient client : clients) {
            clientMap.put(client.storageType(), client);
        }
    }

    public FileStorageClient getClient(String storageType) {
        if (StrUtil.isBlank(storageType)) {
            throw new BusinessException(BizErrorCode.PARAMS_MISSING_ERROR, "存储类型不能为空");
        }
        FileStorageClient client = clientMap.get(storageType);
        if (client == null) {
            throw new BusinessException(BizErrorCode.PARAMS_ERROR, "不支持的存储类型");
        }
        return client;
    }
}
