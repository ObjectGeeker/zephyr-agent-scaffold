package com.object.ai.memory.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.object.ai.common.vo.BaseVO;
import com.object.ai.memory.model.enums.SessionStatusEnum;
import com.object.ai.memory.model.po.SessionPO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 会话
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agent 会话")
public class SessionVO extends BaseVO {

    @Schema(description = "会话名称")
    private String sessionName;
    @Schema(description = "所属用户 ID")
    private String userId;
    @Schema(description = "智能体 ID")
    private String agentId;
    @Schema(description = "会话级长期记忆总结")
    private String longTimeMemory;
    @Schema(description = "记忆总结次数")
    private Integer summaryCount;
    @Schema(description = "最后一条消息时间")
    private LocalDateTime lastMessageAt;
    @Schema(description = "会话状态")
    private SessionStatusEnum status;

    public SessionPO toSessionPO() {
        SessionPO sessionPO = BeanUtil.copyProperties(this, SessionPO.class, "status");
        sessionPO.setStatus(this.status.name());
        return sessionPO;
    }

}
