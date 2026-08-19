package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.auth.AuthenticationFailedException;
import com.echoamoy.holdlens.server.cases.auth.UsernameOccupiedException;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentCallbackHttpException.class)
    public ResponseEntity<Response<Void>> handleAgentCallbackHttpException(AgentCallbackHttpException exception) {
        log.error("agent 回调处理失败，httpStatus={}，code={}，info={}",
                exception.getHttpStatus().value(), exception.getCode(), exception.getInfo(), exception);
        return ResponseEntity.status(exception.getHttpStatus())
                .body(Response.fail(exception.getCode(), exception.getInfo()));
    }

    @ExceptionHandler(AppException.class)
    public Response<Void> handleAppException(AppException exception) {
        log.error("业务异常，code={}，info={}", exception.getCode(), exception.getInfo(), exception);
        return Response.fail(exception.getCode(), exception.getInfo());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Response.fail("0403", "无权访问该资源"));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Response<Void>> handleAuthenticationFailedException(AuthenticationFailedException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Response.fail("0401", "认证失败"));
    }

    @ExceptionHandler(UsernameOccupiedException.class)
    public ResponseEntity<Response<Void>> handleUsernameOccupiedException(UsernameOccupiedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Response.fail("0409", "用户名已占用"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.error("非法参数异常", exception);
        return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        // 绑定异常可能携带 rejectedValue；资产名称、金额和备注不能随异常对象进入日志。
        log.warn("参数校验异常，errorCount={}", exception.getBindingResult().getErrorCount());
        return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
    }

    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Response.fail(ResponseCode.UN_ERROR.getCode(), ResponseCode.UN_ERROR.getInfo());
    }
}
