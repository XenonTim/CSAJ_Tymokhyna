package practice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class ServerHttp {
    private final int port;
    private HttpServer server;
    private final Storage storage;
    private final JwtUtil jwt_util;
    private final ObjectMapper object_mapper;

    public ServerHttp(int port, Storage storage) {
        this.port = port;
        this.storage = storage;
        this.jwt_util = new JwtUtil();
        this.object_mapper = new ObjectMapper();
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/login", this::handle_login);

            HttpContext products_context = server.createContext("/products", this::handle_products);
            products_context.setAuthenticator(new JwtAuthenticator(jwt_util));

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("[HTTP Server] Started on port " + port);
        } catch (IOException e) {
            System.err.println("[HTTP Server] Start failed: " + e.getMessage());
        }
    }

    private void handle_login(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send_response(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            Map<?, ?> body = object_mapper.readValue(exchange.getRequestBody(), Map.class);
            String username = (String) body.get("login");
            String password = (String) body.get("password");

            if ("admin".equals(username) && "admin123".equals(password)) {
                String token = jwt_util.generateToken(username);
                String json_response = String.format("{\"token\":\"%s\"}", token);
                send_response(exchange, 200, json_response);
            } else {
                send_response(exchange, 403, "Invalid login or password");
            }
        } catch (Exception e) {
            send_response(exchange, 400, "Bad Request: " + e.getMessage());
        }
    }

    private void handle_products(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] path_parts = path.split("/");

        try {
            if ("GET".equalsIgnoreCase(method) && path_parts.length == 3) {
                String id = path_parts[2];
                Optional<Product> product = storage.find_by_id(id);
                if (product.isPresent()) {
                    send_response(exchange, 200, object_mapper.writeValueAsString(product.get()));
                } else {
                    send_response(exchange, 404, "Product Not Found");
                }
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Product product = object_mapper.readValue(exchange.getRequestBody(), Product.class);

                boolean nameExists = storage.search(new ProductSearchQuery() {{ name = product.get_name(); }} )
                        .get_total_elements() > 0;

                if (nameExists) {
                    send_response(exchange, 409, "Conflict: Product name already exists");
                    return;
                }

                Product created = storage.create(product);
                send_response(exchange, 201, object_mapper.writeValueAsString(created));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && path_parts.length == 3) {
                String id = path_parts[2];
                Product product = object_mapper.readValue(exchange.getRequestBody(), Product.class);
                product.set_id(id);

                try {
                    Product updated = storage.update(product);
                    send_response(exchange, 200, object_mapper.writeValueAsString(updated));
                } catch (IllegalArgumentException e) {
                    send_response(exchange, 404, e.getMessage());
                }
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && path_parts.length == 3) {
                String id = path_parts[2];
                boolean deleted = storage.delete(id);
                if (deleted) {
                    send_response(exchange, 200, "Deleted successfully");
                } else {
                    send_response(exchange, 404, "Product Not Found");
                }
                return;
            }

            send_response(exchange, 405, "Method Not Allowed or Invalid Path");
        } catch (Exception e) {
            send_response(exchange, 500, "Server Error: " + e.getMessage());
        }
    }

    private void send_response(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[HTTP Server] Stopped.");
        }
    }
}