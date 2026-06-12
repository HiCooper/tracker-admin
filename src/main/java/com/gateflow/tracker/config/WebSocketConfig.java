package com.gateflow.tracker.config;

import com.gateflow.tracker.websocket.DebugWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DebugWebSocketHandler debugWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(debugWebSocketHandler, "/ws/debug/sdk/{sessionId}")
                .setAllowedOriginPatterns("*");
        registry.addHandler(debugWebSocketHandler, "/ws/debug/view/{sessionId}")
                .setAllowedOriginPatterns("*");
    }
}
