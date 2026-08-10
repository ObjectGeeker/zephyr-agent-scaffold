package com.object.ai.common.base;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础VO类
 */
@Data
public class BaseVO {

    private String id;

    private String createUser;

    private LocalDateTime createTime;

    private String updateUser;

    private LocalDateTime updateTime;

    private Boolean isDelete;

}
