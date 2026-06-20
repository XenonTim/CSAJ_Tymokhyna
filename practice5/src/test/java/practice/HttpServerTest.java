package practice;

import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpServerTest {
    private static ServerHttp server;
    private static Storage storage;
    private static HttpClient client;
    private static String jwtToken;
    private static final int PORT = 8085;
    private static final String BASE_URL = "http://localhost:" + PORT;

    @BeforeAll
    public static void start_server() throws Exception {
        storage = new Storage();
        server = new ServerHttp(PORT, storage);
        server.start();

        client = HttpClient.newHttpClient();

        String login_json = "{\"login\":\"admin\",\"password\":\"admin123\"}";
        HttpRequest login_request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(login_json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(login_request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        jwtToken = response.body()
                .split("\"token\":\"")[1]
                .split("\"")[0];
    }

    @AfterAll
    public static void stop_server() {
        server.stop();
    }

    @BeforeEach
    public void clean_database() {
        storage.delete("test-id");
    }

    @Test
    public void shouldReturn401WhenNoToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/test-id"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    public void shouldCreateProduct() throws Exception {
        String productJson = "{\"_id\":\"test-id\",\"_name\":\"Wireless Mouse\",\"_category\":\"Electronics\",\"_quantity\":10,\"_price\":25.5}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString(productJson))
                .header("Authorization", "Bearer " + jwtToken)
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("Wireless Mouse");
    }

    @Test
    public void shouldReturn409WhenProductNameExists() throws Exception {
        storage.create(new Product("test-id", "Bread", "Food", 5, 1.0));

        String duplicateJson = "{\"_id\":\"test-id\",\"_name\":\"Bread\",\"_category\":\"_Food\",\"_quantity\":1,\"_price\":1.0}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products"))
                .PUT(HttpRequest.BodyPublishers.ofString(duplicateJson))
                .header("Authorization", "Bearer " + jwtToken)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(409);
    }

    @Test
    public void shouldGetProduct() throws Exception {
        storage.create(new Product("test-id", "Keyboard", "Electronics", 5, 45.0));
        HttpRequest request200 = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/test-id"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request200, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Keyboard");
    }

    @Test
    public void shouldReturn404WhenProductNotFound() throws Exception {
        HttpRequest request404 = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/unknown"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();
        assertThat(client.send(request404, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
    }
}
