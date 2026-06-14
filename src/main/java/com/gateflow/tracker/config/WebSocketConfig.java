package com.gateflow.tracker.config;

import com.gateflow.tracker.websocket.DebugWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DebugWebSocketHandler debugHandler;

    public WebSocketConfig(DebugWebSocketHandler debugHandler) {
        this.debugHandler = debugHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(debugHandler, "/ws/debug/view/*", "/ws/debug/sdk/*")
                .setAllowedOrigins("*");
    }
}
