package com.object.ai.common.utils;

import com.object.ai.common.exception.BizErrorCode;
import com.object.ai.common.vo.BaseResponse;

public class ResultUtil {

    public static <T> BaseResponse<T> ok(T data) {

        return new BaseResponse<>(BizErrorCode.SUCCESS.getCode(), BizErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> BaseResponse<T> error(Integer code, String message) {
        return new BaseResponse<>(code, message);
    }

}
