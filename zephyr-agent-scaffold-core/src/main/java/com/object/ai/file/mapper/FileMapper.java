package com.object.ai.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.object.ai.file.model.po.FilePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FilePO> {
}
