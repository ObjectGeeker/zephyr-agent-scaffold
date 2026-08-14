package com.object.ai.security.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.security.user.model.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户表数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
