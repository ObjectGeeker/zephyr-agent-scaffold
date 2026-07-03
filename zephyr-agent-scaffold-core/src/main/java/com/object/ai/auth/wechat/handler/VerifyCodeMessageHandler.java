package com.object.ai.auth.wechat.handler;

import com.object.ai.auth.wechat.service.WeChatLoginTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * 处理用户回复验证码的文本消息
 */
@Component
@RequiredArgsConstructor
public class VerifyCodeMessageHandler {

    private final WeChatLoginTicketService loginTicketService;

    public WxMpXmlOutMessage handle(WxMpXmlMessage message) {
        String content = message.getContent();
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String verifyCode = content.trim();
        if (!verifyCode.matches("\\d{4}")) {
            return null;
        }
        boolean success = loginTicketService.verifyCode(message.getFromUser(), verifyCode);
        if (success) {
            return buildTextReply(message, "验证成功，请返回网页继续操作");
        }
        return buildTextReply(message, "验证码错误或已失效，请返回网页重新扫码");
    }

    private WxMpXmlOutMessage buildTextReply(WxMpXmlMessage message, String content) {
        return WxMpXmlOutMessage.TEXT()
                .content(content)
                .fromUser(message.getToUser())
                .toUser(message.getFromUser())
                .build();
    }

}
