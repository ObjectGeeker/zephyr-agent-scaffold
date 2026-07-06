package com.object.ai.memory.model.po;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.object.ai.common.po.BasePO;
import com.object.ai.memory.model.enums.MessageRoleEnum;
import com.object.ai.memory.model.vo.MessageVO;
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
@TableName(value = "tb_message", autoResultMap = true)
public class MessagePO extends BasePO {

    @Schema(description = "所属会话 ID")
    private String sessionId;
    @Schema(description = "消息角色：user/assistant/system/tool")
    private String role;
    @Schema(description = "消息内容")
    private String messageContent;
    @Schema(description = "附件 fileId 列表")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachment;
    @Schema(description = "工具调用等扩展信息")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    @Schema(description = "会话内消息序号")
    private Integer messageIndex;

    public MessageVO toMessageVO() {
        MessageVO messageVO = BeanUtil.copyProperties(this, MessageVO.class, "role");
        messageVO.setRole(MessageRoleEnum.valueOf(this.role));
        return messageVO;
    }

}
