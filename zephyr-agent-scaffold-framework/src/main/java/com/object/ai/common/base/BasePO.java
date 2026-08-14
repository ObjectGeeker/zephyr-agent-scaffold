package com.object.ai.common.base;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BasePO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(value = "CREATE_USER", fill = FieldFill.INSERT)
    private String createUser;

    @TableField(value = "CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "UPDATE_USER", fill = FieldFill.INSERT_UPDATE)
    private String updateUser;

    @TableField(value = "UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(delval = "1", value = "0")
    private Boolean isDelete;

}
