package com.object.ai.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.memory.model.po.SessionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<SessionPO> {
}
