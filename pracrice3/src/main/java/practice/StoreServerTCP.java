package practice;

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

    public StoreServerTCP(int port) {
        this.port = port;
        this.isRunning = new AtomicBoolean(false);
        this.storage = new Storage();
        this.threadPool = Executors.newCachedThreadPool();
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

                int commandType = 1;
                int amount = 0;

                if (jsonStr != null) {
                    String clean = jsonStr.replaceAll("\\s+", "");
                    if (clean.contains("\"command_type\":2")) commandType = 2;
                    if (clean.contains("\"command_type\":3")) commandType = 3;

                    if (clean.contains("\"amount\":")) {
                        String digits = clean.substring(clean.indexOf("\"amount\":")).replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            amount = Integer.parseInt(digits);
                        }
                    }
                }

                int newBalance = 0;
                String statusText = "OK";
                switch (commandType) {
                    case 1:
                        newBalance = storage.get_balance();
                        statusText = "Inquiry success";
                        break;
                    case 2:
                        newBalance = storage.deduct_product(amount);
                        statusText = "Deduction success";
                        break;
                    case 3:
                        newBalance = storage.add_product(amount);
                        statusText = "Addition success";
                        break;
                }

                Message responseMsg = new Message(
                        incomingMsg.get_unique_identefier(),
                        incomingMsg.get_message_number(),
                        incomingMsg.get_command_id(),
                        incomingMsg.get_user_id(),
                        String.format("%s. Current balance: %d", statusText, newBalance)
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