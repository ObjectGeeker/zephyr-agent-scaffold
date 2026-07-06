package com.object.ai.memory.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.memory.model.po.MessagePO;
import org.apache.ibatis.annotations.Mapper;

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
}