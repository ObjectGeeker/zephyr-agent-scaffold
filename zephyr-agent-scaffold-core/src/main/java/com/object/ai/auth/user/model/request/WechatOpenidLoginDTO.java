package com.object.ai.auth.user.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信 openid 登录请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WechatOpenidLoginDTO {

    /**
     * 微信 openid
     */
    private String wxOpenid;

}
