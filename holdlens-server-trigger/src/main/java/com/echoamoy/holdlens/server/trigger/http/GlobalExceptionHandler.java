package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentCallbackHttpException.class)
    public ResponseEntity<Response<Void>> handleAgentCallbackHttpException(AgentCallbackHttpException exception) {
        log.warn("agent 回调处理失败，httpStatus={}，code={}，info={}",
                exception.getHttpStatus().value(), exception.getCode(), exception.getInfo());
        return ResponseEntity.status(exception.getHttpStatus())
                .body(Response.fail(exception.getCode(), exception.getInfo()));
    }

    @ExceptionHandler(AppException.class)
    public Response<Void> handleAppException(AppException exception) {
        log.error("业务异常，code={}，info={}", exception.getCode(), exception.getInfo(), exception);
        return Response.fail(exception.getCode(), exception.getInfo());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("非法参数异常", exception);
        return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        log.warn("参数校验异常", exception);
        return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
    }
}
