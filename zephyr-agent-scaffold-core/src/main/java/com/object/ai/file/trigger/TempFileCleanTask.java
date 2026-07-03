package com.object.ai.file.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.object.ai.file.client.FileStorageClient;
import com.object.ai.file.factory.FileStorageClientFactory;
import com.object.ai.file.mapper.FileMapper;
import com.object.ai.file.model.po.FilePO;
import com.object.ai.file.model.properties.FileStorageConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class TempFileCleanTask {

    private static final int DEFAULT_EXPIRE_DAYS = 1;

    private final FileMapper fileMapper;

    private final FileStorageClientFactory fileStorageClientFactory;

    private final FileStorageConfigProperties properties;

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanTempFiles() {
        FileStorageConfigProperties.TempClean tempClean = properties.getTempClean();
        if (tempClean == null || !Boolean.TRUE.equals(tempClean.getEnable())) {
            log.debug("临时文件清理任务未启用");
            return;
        }

        int expireDays = tempClean.getExpireDays() == null || tempClean.getExpireDays() < 1
                ? DEFAULT_EXPIRE_DAYS
                : tempClean.getExpireDays();
        LocalDateTime expireTime = LocalDateTime.now().minusDays(expireDays);
        List<FilePO> tempFiles = queryExpiredTempFiles(expireTime);
        if (tempFiles.isEmpty()) {
            log.info("临时文件清理完成，无过期文件，expireDays={}", expireDays);
            return;
        }

        int successCount = 0;
        for (FilePO filePO : tempFiles) {
            if (cleanTempFile(filePO)) {
                successCount++;
            }
        }
        log.info("临时文件清理完成，total={}, success={}", tempFiles.size(), successCount);
    }

    private List<FilePO> queryExpiredTempFiles(LocalDateTime expireTime) {
        LambdaQueryWrapper<FilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FilePO::getIsTemp, true)
                .le(FilePO::getCreateTime, expireTime);
        return fileMapper.selectList(wrapper);
    }

    private boolean cleanTempFile(FilePO filePO) {
        try {
            FileStorageClient client = fileStorageClientFactory.getClient(filePO.getStorageType());
            client.remove(filePO.getBucket(), filePO.getObjectKey());
            int affectedRows = fileMapper.deleteById(filePO.getId());
            if (affectedRows <= 0) {
                log.warn("删除临时文件元数据失败，fileId={}", filePO.getId());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("清理临时文件失败，fileId={}, objectKey={}", filePO.getId(), filePO.getObjectKey(), e);
            return false;
        }
    }
}
