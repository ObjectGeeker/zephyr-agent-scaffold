package com.object.ai.common.base;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用返回累
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
public class BaseResponse<T> {

    private Integer code;

    private boolean success;

    private T data;

    private String message;

    public BaseResponse(Integer code, boolean success, T data, String message) {
        this.code = code;
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(Integer code, boolean success, T data) {
        this.success = success;
        this.data = data;
        this.code = code;
    }

    public BaseResponse(Integer code, boolean success, String message) {
        this.success = success;
        this.message = message;
        this.code = code;
    }
}
