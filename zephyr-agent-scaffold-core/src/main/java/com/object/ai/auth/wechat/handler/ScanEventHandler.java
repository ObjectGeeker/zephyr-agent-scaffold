package com.object.ai.auth.wechat.handler;

import com.object.ai.auth.wechat.service.WeChatLoginTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 处理微信扫码 / 关注事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScanEventHandler {

    private final WeChatLoginTicketService loginTicketService;

    public WxMpXmlOutMessage handle(WxMpXmlMessage message) {
        String sceneStr = resolveSceneStr(message.getEventKey());
        if (!StringUtils.hasText(sceneStr)) {
            return null;
        }
        String verifyCode = loginTicketService.onScanned(sceneStr, message.getFromUser());
        if (!StringUtils.hasText(verifyCode)) {
            return buildTextReply(message, "二维码已失效，请返回网页重新获取");
        }
        return buildTextReply(message, "请回复数字 " + verifyCode + " 完成登录");
    }

    private String resolveSceneStr(String eventKey) {
        if (!StringUtils.hasText(eventKey)) {
            return null;
        }
        if (eventKey.startsWith("qrscene_")) {
            return eventKey.substring("qrscene_".length());
        }
        return eventKey;
    }

    private WxMpXmlOutMessage buildTextReply(WxMpXmlMessage message, String content) {
        return WxMpXmlOutMessage.TEXT()
                .content(content)
                .fromUser(message.getToUser())
                .toUser(message.getFromUser())
                .build();
    }

}
