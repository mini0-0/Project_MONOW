package com.monow.external.kis.realtime.event;

public record KisWebSocketConnectionEvent(
        ConnectionStatus status
) {

    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
    }
}
