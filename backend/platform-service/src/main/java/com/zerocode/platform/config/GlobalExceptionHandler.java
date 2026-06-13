package com.zerocode.platform.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.zerocode.platform.exception.BusinessException;
import com.zerocode.platform.exception.ErrorCode;
import com.zerocode.platform.vo.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleNotLogin() {
        return ApiResponse.fail(401, ErrorCode.AUTH_UNAUTHORIZED.defaultMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleNotPermission() {
        return ApiResponse.fail(403, ErrorCode.AUTH_FORBIDDEN.defaultMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusiness(BusinessException exception) {
        log.warn("Business exception: {} - {}", exception.getErrorCode(), exception.getMessage());
        return ApiResponse.fail(400, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument() {
        return ApiResponse.fail(400, ErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation() {
        return ApiResponse.fail(400, ErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadableMessage() {
        return ApiResponse.fail(400, ErrorCode.VALIDATION_ERROR.defaultMessage());
    }

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiResponse<Void> handleRestClient(RestClientException exception) {
        log.warn("Upstream service request failed", exception);
        return ApiResponse.fail(502, ErrorCode.UPSTREAM_UNAVAILABLE.defaultMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unhandled platform exception", exception);
        return ApiResponse.fail(500, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }
}
