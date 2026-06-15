package com.gateflow.tracker.config;

import com.gateflow.tracker.security.WebSocketAuthInterceptor;
import com.gateflow.tracker.websocket.DebugWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DebugWebSocketHandler debugHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    /** 观看端(admin 前端)允许的来源白名单。 */
    @Value("${gateflow.websocket.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] viewerAllowedOrigins;

    public WebSocketConfig(DebugWebSocketHandler debugHandler, WebSocketAuthInterceptor authInterceptor) {
        this.debugHandler = debugHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 观看端:admin UI 发起,必须带 JWT 且来源在白名单内。
        registry.addHandler(debugHandler, "/ws/debug/view/*")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(viewerAllowedOrigins);

        // SDK 端:由被调试的业务页面发起,天然跨域,无法套用 admin 来源白名单/JWT。
        // 这里保留连接能力,SDK 侧调试令牌方案见后续工作项(sessionId 归属校验)。
        registry.addHandler(debugHandler, "/ws/debug/sdk/*")
                .setAllowedOriginPatterns("*");
    }
}
