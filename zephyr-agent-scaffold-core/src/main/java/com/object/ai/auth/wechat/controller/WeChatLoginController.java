package com.object.ai.auth.wechat.controller;

import com.object.ai.auth.wechat.model.vo.WeChatLoginStatusVO;
import com.object.ai.auth.wechat.model.vo.WeChatQrCodeVO;
import com.object.ai.auth.wechat.service.WeChatLoginTicketService;
import com.object.ai.common.utils.ResultUtil;
import com.object.ai.common.vo.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信扫码登录接口
 */
@Tag(name = "微信扫码登录", description = "微信公众号扫码登录")
@RestController
@RequestMapping("/auth/wechat")
@RequiredArgsConstructor
public class WeChatLoginController {

    private final WeChatLoginTicketService loginTicketService;

    @Operation(summary = "创建登录二维码", description = "生成临时带参二维码，用于微信扫码登录")
    @PostMapping("/qrcode")
    public BaseResponse<WeChatQrCodeVO> createQrCode() {
        return ResultUtil.ok(loginTicketService.createQrCode());
    }

    @Operation(summary = "查询登录状态", description = "轮询登录票据状态，CONFIRMED 时返回 wxOpenid")
    @GetMapping("/qrcode/{ticketId}")
    public BaseResponse<WeChatLoginStatusVO> getStatus(@PathVariable String ticketId) {
        return ResultUtil.ok(loginTicketService.getStatus(ticketId));
    }

}
