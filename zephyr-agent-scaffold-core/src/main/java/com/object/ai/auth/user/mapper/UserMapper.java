package com.object.ai.auth.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.auth.user.model.po.UserPO;
import com.object.ai.auth.user.model.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {

    default UserVO findUserByOpenid(String openid) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getWxOpenid, openid);
        UserPO userPO = this.selectOne(wrapper);
        return userPO == null ? null : userPO.toUserVO();
    }

    default UserVO findUserByAccount(String account) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUserAccount, account);
        UserPO userPO = this.selectOne(wrapper);
        return userPO == null ? null : userPO.toUserVO();
    }

}
