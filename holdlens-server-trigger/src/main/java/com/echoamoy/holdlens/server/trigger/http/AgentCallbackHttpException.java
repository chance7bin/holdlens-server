package com.echoamoy.holdlens.server.trigger.http;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 让 agent callback 的失败显式使用非 2xx，避免 agent 把未落库结果误判为投递成功。
 */
@Getter
public class AgentCallbackHttpException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String code;
    private final String info;

    public AgentCallbackHttpException(HttpStatus httpStatus, String code, String info, Throwable cause) {
        super(info, cause);
        this.httpStatus = httpStatus;
        this.code = code;
        this.info = info;
    }
}
