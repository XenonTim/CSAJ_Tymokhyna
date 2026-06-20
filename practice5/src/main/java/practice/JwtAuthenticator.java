package practice;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

public class JwtAuthenticator extends Authenticator{
    private final JwtUtil jwt_util;

    public JwtAuthenticator(JwtUtil jwtService) {
        this.jwt_util = jwtService;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        String auth_header = exchange.getRequestHeaders().getFirst("Authorization");

        if (auth_header == null || !auth_header.startsWith("Bearer ")) {
            return new Failure(401);
        }

        String token = auth_header.substring(7);
        String username = jwt_util.validateTokenAndGetUsername(token);

        if (username == null) {
            return new Failure(401);
        }

        return new Success(new HttpPrincipal(username, "realm"));
    }
}
