package com.object.ai.auth.wechat.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.object.ai.auth.wechat.config.WeChatMpProperties;
import com.object.ai.auth.wechat.model.WeChatLoginTicket;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 微信扫码登录凭据 Guava 内存缓存
 */
@Component
public class WeChatLoginTicketCache {

    private final Cache<String, WeChatLoginTicket> ticketCache;

    private final Cache<String, String> sceneIndexCache;

    private final Cache<String, String> openidIndexCache;

    public WeChatLoginTicketCache(WeChatMpProperties properties) {
        long expireMinutes = Math.max(1, (properties.getQrcodeExpireSeconds() + 59) / 60);
        this.ticketCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.sceneIndexCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        this.openidIndexCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public void put(WeChatLoginTicket ticket) {
        ticketCache.put(ticket.getTicketId(), ticket);
        sceneIndexCache.put(ticket.getSceneStr(), ticket.getTicketId());
    }

    public WeChatLoginTicket getByTicketId(String ticketId) {
        return ticketCache.getIfPresent(ticketId);
    }

    public WeChatLoginTicket getBySceneStr(String sceneStr) {
        String ticketId = sceneIndexCache.getIfPresent(sceneStr);
        if (ticketId == null) {
            return null;
        }
        return ticketCache.getIfPresent(ticketId);
    }

    public WeChatLoginTicket getByOpenid(String openid) {
        String ticketId = openidIndexCache.getIfPresent(openid);
        if (ticketId == null) {
            return null;
        }
        return ticketCache.getIfPresent(ticketId);
    }

    public void bindOpenid(String openid, String ticketId) {
        openidIndexCache.put(openid, ticketId);
    }

    public void removeOpenidIndex(String openid) {
        openidIndexCache.invalidate(openid);
    }

}
