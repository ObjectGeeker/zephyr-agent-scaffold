package com.object.ai.memory.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.object.ai.common.vo.BaseVO;
import com.object.ai.memory.model.enums.MessageRoleEnum;
import com.object.ai.memory.model.po.MessagePO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent 消息记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent 消息记录")
public class MessageVO extends BaseVO {

    @Schema(description = "所属会话 ID")
    private String sessionId;
    @Schema(description = "消息角色")
    private MessageRoleEnum role;
    @Schema(description = "消息内容")
    private String messageContent;
    @Schema(description = "附件 fileId 列表")
    private List<String> attachment;
    @Schema(description = "工具调用等扩展信息")
    private Map<String, Object> metadata;
    @Schema(description = "会话内消息序号")
    private Integer messageIndex;

    public MessagePO toMessagePO() {
        MessagePO messagePO = BeanUtil.copyProperties(this, MessagePO.class, "role");
        messagePO.setRole(this.role.name());
        return messagePO;
    }

}
