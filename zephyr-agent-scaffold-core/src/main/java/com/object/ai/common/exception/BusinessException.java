package com.object.ai.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    private final String message;

    public BusinessException(BizErrorCode businessErrorCode) {
        this.code = businessErrorCode.getCode();
        this.message = businessErrorCode.getMessage();
    }

    public BusinessException(BizErrorCode businessErrorCode, String message) {
        this.code = businessErrorCode.getCode();
        this.message = message;
    }

}
