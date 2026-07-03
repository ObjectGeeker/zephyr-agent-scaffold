package com.object.ai.common.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据容器
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataContainer<T> {

    /**
     * 新增数据
     */
    private List<T> addedData = new ArrayList<>();
    /**
     * 修改数据
     */
    private List<T> modifyData = new ArrayList<>();
    /**
     * 删除数据
     */
    private List<T> removeData = new ArrayList<>();

}
