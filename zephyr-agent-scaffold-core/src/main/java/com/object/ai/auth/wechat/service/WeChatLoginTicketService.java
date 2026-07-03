package com.object.ai.auth.wechat.service;

import com.object.ai.auth.wechat.model.vo.WeChatLoginStatusVO;
import com.object.ai.auth.wechat.model.vo.WeChatQrCodeVO;

/**
 * 微信扫码登录票据服务
 */
public interface WeChatLoginTicketService {

    /**
     * 创建登录票据并生成临时二维码
     */
    WeChatQrCodeVO createQrCode();

    /**
     * 查询登录票据状态
     */
    WeChatLoginStatusVO getStatus(String ticketId);

    /**
     * 用户扫码后绑定 openid 并生成验证码
     *
     * @return 待回复的验证码，票据无效时返回 null
     */
    String onScanned(String sceneStr, String openid);

    /**
     * 校验用户回复的验证码
     *
     * @return 是否验证成功
     */
    boolean verifyCode(String openid, String verifyCode);

}
