package com.object.ai.memory.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.memory.model.po.MessagePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<MessagePO> {

    default Integer selectMaxMessageIndex(String sessionId) {
        LambdaQueryWrapper<MessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessagePO::getSessionId, sessionId)
                .select(MessagePO::getMessageIndex)
                .orderByDesc(MessagePO::getMessageIndex)
                .last("LIMIT 1");
        MessagePO messagePO = this.selectOne(wrapper);
        return messagePO == null ? null : messagePO.getMessageIndex();
    }

    default void upsertBySessionAndIndex(MessagePO messagePO) {
        LambdaQueryWrapper<MessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessagePO::getSessionId, messagePO.getSessionId())
                .eq(MessagePO::getMessageIndex, messagePO.getMessageIndex());
        MessagePO existing = this.selectOne(wrapper);
        if (existing != null) {
            messagePO.setId(existing.getId());
            this.updateById(messagePO);
            return;
        }
        this.insert(messagePO);
    }

    /**
     * 按消息序号倒序取最近 N 条，再翻转为时间正序返回。
     *
     * @param beforeIndex 仅返回序号小于该值的记录，null 表示从最新开始
     */
    default List<MessagePO> selectHistoryMessages(String sessionId, Integer beforeIndex, int limit) {
        LambdaQueryWrapper<MessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessagePO::getSessionId, sessionId);
        if (beforeIndex != null) {
            wrapper.lt(MessagePO::getMessageIndex, beforeIndex);
        }
        wrapper.orderByDesc(MessagePO::getMessageIndex)
                .last("LIMIT " + limit);
        List<MessagePO> messages = selectList(wrapper);
        Collections.reverse(messages);
        return messages;
    }
}