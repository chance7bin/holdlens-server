package com.echoamoy.holdlens.server.domain.processing.model.valobj;

import java.util.Set;

public enum ProcessingTaskStatusEnumVO {

    CREATED("created", false),
    DISPATCHED("dispatched", false),
    RUNNING("running", false),
    SUCCEEDED("succeeded", true),
    PARTIAL_FAILED("partial_failed", true),
    FAILED("failed", true),
    DISPATCH_FAILED("dispatch_failed", true),
    CALLBACK_FAILED("callback_failed", true);

    private static final Set<ProcessingTaskStatusEnumVO> DISPATCHABLE = Set.of(CREATED);
    private static final Set<ProcessingTaskStatusEnumVO> RUNNABLE = Set.of(CREATED, DISPATCHED);
    private static final Set<ProcessingTaskStatusEnumVO> TERMINABLE = Set.of(CREATED, DISPATCHED, RUNNING);

    private final String code;
    private final boolean terminal;

    ProcessingTaskStatusEnumVO(String code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public String getCode() {
        return code;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean canTransitTo(ProcessingTaskStatusEnumVO target) {
        if (target == null || this == target) {
            return true;
        }
        if (terminal) {
            return false;
        }
        return switch (target) {
            case DISPATCHED -> DISPATCHABLE.contains(this);
            case RUNNING -> RUNNABLE.contains(this);
            case SUCCEEDED, PARTIAL_FAILED, FAILED, CALLBACK_FAILED -> TERMINABLE.contains(this);
            case DISPATCH_FAILED -> this == CREATED;
            case CREATED -> false;
        };
    }

    public static ProcessingTaskStatusEnumVO fromCode(String code) {
        for (ProcessingTaskStatusEnumVO value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported processing task status: " + code);
    }

}
