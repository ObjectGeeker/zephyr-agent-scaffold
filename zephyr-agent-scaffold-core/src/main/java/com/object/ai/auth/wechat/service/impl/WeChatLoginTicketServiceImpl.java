package com.object.ai.auth.wechat.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.object.ai.auth.wechat.cache.WeChatLoginTicketCache;
import com.object.ai.auth.wechat.config.WeChatMpProperties;
import com.object.ai.auth.wechat.model.WeChatLoginTicket;
import com.object.ai.auth.wechat.model.enums.WeChatLoginTicketStatusEnum;
import com.object.ai.auth.wechat.model.vo.WeChatLoginStatusVO;
import com.object.ai.auth.wechat.model.vo.WeChatQrCodeVO;
import com.object.ai.auth.wechat.service.WeChatLoginTicketService;
import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 微信扫码登录票据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatLoginTicketServiceImpl implements WeChatLoginTicketService {

    private final WeChatLoginTicketCache ticketCache;

    private final WxMpService wxMpService;

    private final WeChatMpProperties weChatMpProperties;

    @Override
    public WeChatQrCodeVO createQrCode() {
        String ticketId = IdUtil.fastSimpleUUID();
        WeChatLoginTicket ticket = WeChatLoginTicket.builder()
                .ticketId(ticketId)
                .sceneStr(ticketId)
                .status(WeChatLoginTicketStatusEnum.PENDING)
                .createTime(LocalDateTime.now())
                .build();
        ticketCache.put(ticket);

        try {
            WxMpQrCodeTicket qrCodeTicket = wxMpService.getQrcodeService()
                    .qrCodeCreateTmpTicket(ticket.getSceneStr(), weChatMpProperties.getQrcodeExpireSeconds());
            String qrCodeUrl = wxMpService.getQrcodeService().qrCodePictureUrl(qrCodeTicket.getTicket());
            return WeChatQrCodeVO.builder()
                    .ticketId(ticketId)
                    .qrCodeUrl(qrCodeUrl)
                    .expireSeconds(weChatMpProperties.getQrcodeExpireSeconds())
                    .build();
        } catch (WxErrorException e) {
            log.error("创建微信临时二维码失败, ticketId={}", ticketId, e);
            throw new BusinessException(BizErrorCode.REMOTE_CALL_ERROR, "创建微信二维码失败：" + e.getError().getErrorMsg());
        }
    }

    @Override
    public WeChatLoginStatusVO getStatus(String ticketId) {
        WeChatLoginTicket ticket = ticketCache.getByTicketId(ticketId);
        if (ticket == null) {
            return WeChatLoginStatusVO.builder()
                    .ticketId(ticketId)
                    .status(WeChatLoginTicketStatusEnum.EXPIRED)
                    .build();
        }
        return WeChatLoginStatusVO.builder()
                .ticketId(ticketId)
                .status(ticket.getStatus())
                .wxOpenid(ticket.getStatus() == WeChatLoginTicketStatusEnum.CONFIRMED ? ticket.getOpenid() : null)
                .build();
    }

    @Override
    public String onScanned(String sceneStr, String openid) {
        WeChatLoginTicket ticket = ticketCache.getBySceneStr(sceneStr);
        if (ticket == null || ticket.getStatus() != WeChatLoginTicketStatusEnum.PENDING) {
            log.warn("扫码票据无效, sceneStr={}, openid={}", sceneStr, openid);
            return null;
        }
        String verifyCode = RandomUtil.randomNumbers(4);
        ticket.setOpenid(openid);
        ticket.setVerifyCode(verifyCode);
        ticket.setStatus(WeChatLoginTicketStatusEnum.WAITING_CODE);
        ticketCache.bindOpenid(openid, ticket.getTicketId());
        log.info("用户扫码成功, ticketId={}, openid={}", ticket.getTicketId(), openid);
        return verifyCode;
    }

    @Override
    public boolean verifyCode(String openid, String verifyCode) {
        WeChatLoginTicket ticket = ticketCache.getByOpenid(openid);
        if (ticket == null || ticket.getStatus() != WeChatLoginTicketStatusEnum.WAITING_CODE) {
            return false;
        }
        if (!Objects.equals(ticket.getVerifyCode(), verifyCode)) {
            return false;
        }
        ticket.setStatus(WeChatLoginTicketStatusEnum.CONFIRMED);
        ticket.setConfirmTime(LocalDateTime.now());
        ticketCache.removeOpenidIndex(openid);
        log.info("验证码确认成功, ticketId={}, openid={}", ticket.getTicketId(), openid);
        return true;
    }

}
