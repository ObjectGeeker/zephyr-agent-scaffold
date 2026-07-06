package com.object.ai.memory.service;

import com.object.ai.memory.model.request.MessageQueryDTO;
import com.object.ai.memory.model.request.SessionCreateDTO;
import com.object.ai.memory.model.request.SessionDeleteDTO;
import com.object.ai.memory.model.request.SessionQueryDTO;
import com.object.ai.memory.model.request.SessionUpdateDTO;
import com.object.ai.memory.model.vo.MessageVO;
import com.object.ai.memory.model.vo.SessionVO;

import java.util.List;

public interface AgentMemoryService {

    List<SessionVO> findSessionList(SessionQueryDTO query);

    SessionVO createSession(SessionCreateDTO request);

    Boolean updateSession(SessionUpdateDTO request);

    Boolean deleteSession(SessionDeleteDTO request);

    List<MessageVO> findMessageList(MessageQueryDTO request);
}
