package com.object.ai.auth.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.object.ai.auth.user.model.request.UserQueryDTO;
import com.object.ai.auth.user.model.request.UserStatusUpdateDTO;
import com.object.ai.auth.user.model.request.UserUpdateDTO;
import com.object.ai.auth.user.model.vo.UserVO;
import com.object.ai.common.request.DataContainer;
import com.object.ai.common.request.PageRequest;

/**
 * 用户管理服务
 */
public interface UserManagerService {

    Page<UserVO> findAllUser(PageRequest<UserQueryDTO> pageRequest);

    Boolean batchSave(DataContainer<UserUpdateDTO> dataContainer);

    Boolean changeStatus(UserStatusUpdateDTO request);

}
