package com.object.ai.common.base;

/**
 * 返回工具类
 */
public class ResultUtil {

    public <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), true, data, message);
    }

    public <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), true, data);
    }

    public <T> BaseResponse<T> fail(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), false, message);
    }

    public <T> BaseResponse<T> fail(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(), false, errorCode.getMessage());
    }

}
