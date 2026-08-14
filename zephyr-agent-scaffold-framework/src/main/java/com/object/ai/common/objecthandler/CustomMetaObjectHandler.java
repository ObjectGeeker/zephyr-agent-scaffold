package com.object.ai.common.objecthandler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class CustomMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        fillUserId(metaObject, true);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        fillUserId(metaObject, false);
    }

    private void fillUserId(MetaObject metaObject, boolean insert) {
        try {
            String loginUserId = StpUtil.getLoginIdAsString();
            this.strictInsertFill(metaObject, "updateUser", String.class, loginUserId);
            if (insert) {
                this.strictInsertFill(metaObject, "createUser", String.class, loginUserId);
            }
        } catch (Exception e) {
            log.error("fill userId error", e);
        }
    }
}
