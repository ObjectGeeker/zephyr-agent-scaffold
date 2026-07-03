package com.object.ai.common.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求体
 *
 * @param <T> 请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequest<T> {

    /**
     * 查询条件
     */
    private T filterInfo;
    /**
     * 页面序号
     */
    private int pageIndex;
    /**
     * 页面大小
     */
    private int pageSize;

}
