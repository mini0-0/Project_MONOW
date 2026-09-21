package com.monow.external.kis.event;

public record KisWebSocketConnectionEvent(
        ConnectionStatus status
) {

    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
    }
}
