package com.object.ai.auth.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户状态更新请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusUpdateDTO {

    /**
     * 用户 id
     */
    private String id;

    /**
     * 用户状态：ACTIVE、BAN
     */
    private String status;

}
