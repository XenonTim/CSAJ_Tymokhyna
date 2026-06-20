package practice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerTCP {
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean isRunning;
    private final ExecutorService threadPool;
    private final Storage storage;
    private final ObjectMapper objectMapper;

    public StoreServerTCP(int port) {
        this.port = port;
        this.isRunning = new AtomicBoolean(false);
        this.storage = new Storage();
        this.threadPool = Executors.newCachedThreadPool();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void start() {
        if (isRunning.getAndSet(true)) return;

        System.out.println("[TCP Server Pipeline] Started on port " + port);

        try {
            serverSocket = new ServerSocket(port);

            while (isRunning.get()) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClientTraffic(clientSocket));
            }
        } catch (IOException e) {
            if (isRunning.get()) {
                System.err.println("[TCP Server] Socket error: " + e.getMessage());
            }
        }
    }

    private void handleClientTraffic(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(1500);

            int length = in.readInt();
            if (length > 0) {
                byte[] packetBytes = new byte[length];
                in.readFully(packetBytes);

                Decrypter decrypter = new Decrypter();
                Message incomingMsg = decrypter.decrypt(packetBytes);
                String jsonStr = incomingMsg.get_message_string();

                int commandId = incomingMsg.get_command_id();
                String statusText = "OK";
                String responsePayload = "";

                switch (commandId) {
                    case 1:
                        Product newProduct = objectMapper.readValue(jsonStr, Product.class);
                        Product saved = storage.create(newProduct);
                        responsePayload = "Created ID: " + saved.get_id();
                        break;

                    case 2:
                        String idToFind = jsonStr.replaceAll("[{}\"\\s]", "").replace("id:", "");
                        responsePayload = storage.find_by_id(idToFind)
                                .map(p -> "Found: " + p.get_name() + ", Price: " + p.get_price())
                                .orElse("Product Not Found");
                        break;

                    case 3:
                        Product productToUpdate = objectMapper.readValue(jsonStr, Product.class);
                        storage.update(productToUpdate);
                        responsePayload = "Updated successfully";
                        break;

                    case 4:
                        String idToDelete = jsonStr.replaceAll("[{}\"\\s]", "").replace("id:", "");
                        boolean deleted = storage.delete(idToDelete);
                        responsePayload = deleted ? "Deleted successfully" : "Delete failed: Not found";
                        break;

                    case 5:
                        ProductSearchQuery query = objectMapper.readValue(jsonStr, ProductSearchQuery.class);
                        Page<Product> resultPage = storage.search(query);
                        responsePayload = String.format("Found total: %d items. Page %d of %d.",
                                resultPage.get_total_elements(), resultPage.get_page_number(), resultPage.get_total_pages());
                        break;

                    default:
                        statusText = "Unknown CRUD operation";
                }

                Message responseMsg = new Message(
                        incomingMsg.get_unique_identefier(),
                        incomingMsg.get_message_number(),
                        incomingMsg.get_command_id(),
                        incomingMsg.get_user_id(),
                        String.format("%s. Result: %s", statusText, responsePayload)
                );

                Encrypter encrypter = new Encrypter();
                byte[] readyPacket = encrypter.encrypt(responseMsg);

                out.writeInt(readyPacket.length);
                out.write(readyPacket);
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("[TCP Server] Error handling client: " + e.getMessage());
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    public void stop() {
        isRunning.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        threadPool.shutdownNow();
        System.out.println("[TCP Server Pipeline] Stopped.");
    }
}