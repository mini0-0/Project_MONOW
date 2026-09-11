package com.monow.api.external.kis.event;

public record KisWebSocketConnectionEvent(
        ConnectionStatus status
) {

    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
    }
}
