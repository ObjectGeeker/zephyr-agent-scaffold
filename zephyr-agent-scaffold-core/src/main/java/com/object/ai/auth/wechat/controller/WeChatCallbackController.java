package com.object.ai.auth.wechat.controller;

import com.object.ai.auth.wechat.handler.ScanEventHandler;
import com.object.ai.auth.wechat.handler.VerifyCodeMessageHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众号服务器回调
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
@Tag(name = "微信回调")
public class WeChatCallbackController {

    private final WxMpService wxMpService;

    private final ScanEventHandler scanEventHandler;

    private final VerifyCodeMessageHandler verifyCodeMessageHandler;

    @GetMapping("/callback")
    public String verify(@RequestParam(name = "signature") String signature,
                         @RequestParam(name = "timestamp") String timestamp,
                         @RequestParam(name = "nonce") String nonce,
                         @RequestParam(name = "echostr") String echostr) {
        if (wxMpService.checkSignature(timestamp, nonce, signature)) {
            return echostr;
        }
        log.warn("微信回调验签失败");
        return "非法请求";
    }

    @PostMapping(value = "/callback", produces = "application/xml; charset=UTF-8")
    public String callback(@RequestBody String requestBody,
                           @RequestParam(name = "signature") String signature,
                           @RequestParam(name = "timestamp") String timestamp,
                           @RequestParam(name = "nonce") String nonce,
                           @RequestParam(name = "openid", required = false) String openid,
                           @RequestParam(name = "encrypt_type", required = false) String encryptType,
                           @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            log.warn("微信消息验签失败, openid={}", openid);
            return "";
        }
        WxMpXmlMessage inMessage = parseMessage(requestBody, timestamp, nonce, msgSignature, encryptType);
        if (inMessage == null) {
            return "";
        }
        WxMpXmlOutMessage outMessage = routeMessage(inMessage);
        if (outMessage == null) {
            return "";
        }
        return outMessage.toXml();
    }

    private WxMpXmlMessage parseMessage(String requestBody,
                                        String timestamp,
                                        String nonce,
                                        String msgSignature,
                                        String encryptType) {
        if ("aes".equalsIgnoreCase(encryptType)) {
            return WxMpXmlMessage.fromEncryptedXml(requestBody,
                    wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
        }
        return WxMpXmlMessage.fromXml(requestBody);
    }

    private WxMpXmlOutMessage routeMessage(WxMpXmlMessage message) {
        if ("event".equalsIgnoreCase(message.getMsgType())) {
            String event = message.getEvent();
            if ("SCAN".equalsIgnoreCase(event) || "subscribe".equalsIgnoreCase(event)) {
                return scanEventHandler.handle(message);
            }
            return null;
        }
        if ("text".equalsIgnoreCase(message.getMsgType()) && StringUtils.hasText(message.getContent())) {
            return verifyCodeMessageHandler.handle(message);
        }
        return null;
    }

}
