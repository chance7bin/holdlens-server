package com.echoamoy.holdlens.server.domain.marketdetail.model.valobj;

public final class MarketDetailDispatchResultVO {
    private final boolean accepted;
    private final String errorSummary;

    public MarketDetailDispatchResultVO(boolean accepted, String errorSummary) {
        this.accepted = accepted;
        this.errorSummary = errorSummary;
    }

    public boolean isAccepted() { return accepted; }
    public String getErrorSummary() { return errorSummary; }
}
