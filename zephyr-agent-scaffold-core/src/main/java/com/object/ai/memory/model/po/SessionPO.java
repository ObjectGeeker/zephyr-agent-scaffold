package com.object.ai.memory.model.po;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.object.ai.common.po.BasePO;
import com.object.ai.memory.model.enums.SessionStatusEnum;
import com.object.ai.memory.model.vo.SessionVO;
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
@TableName(value = "tb_session")
public class SessionPO extends BasePO {

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
    @Schema(description = "会话状态：active-活跃 archived-归档")
    private String status;

    public SessionVO toSessionVO() {
        SessionVO sessionVO = BeanUtil.copyProperties(this, SessionVO.class, "status");
        sessionVO.setStatus(SessionStatusEnum.valueOf(this.status));
        return sessionVO;
    }

}
