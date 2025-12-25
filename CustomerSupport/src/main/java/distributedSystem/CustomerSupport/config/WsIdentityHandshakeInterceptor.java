package distributedSystem.CustomerSupport.config;


import org.springframework.http.server.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class WsIdentityHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        var uri = request.getURI();
        var params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String userId = params.getFirst("userId");   // required for routing
        String role = params.getFirst("role");       // USER / ADMIN (optional, for admin send)

        // No security: allow even if missing, but routing will fail if userId is null.
        attributes.put("userId", userId == null ? "anonymous" : userId);
        attributes.put("role", role == null ? "USER" : role.toUpperCase());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
