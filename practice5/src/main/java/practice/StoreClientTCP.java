package practice;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class StoreClientTCP {
    private final String host;
    private final int port;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isConnected = false;

    public StoreClientTCP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        while (!isConnected) {
            try {
                this.socket = new Socket(host, port);
                this.in = new DataInputStream(socket.getInputStream());
                this.out = new DataOutputStream(socket.getOutputStream());
                this.isConnected = true;
                System.out.println("[TCP Client] Connected to store server.");
            } catch (IOException e) {
                System.out.println("[TCP Client] Server offline. Reconnecting in 2 seconds...");
                try { Thread.sleep(2000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public synchronized byte[] sendCommand(byte[] encryptedPacket) {
        try (Socket temporarySocket = new Socket(host, port);
             DataOutputStream temporaryOut = new DataOutputStream(temporarySocket.getOutputStream());
             DataInputStream temporaryIn = new DataInputStream(temporarySocket.getInputStream())) {

            temporarySocket.setSoTimeout(1500);

            temporaryOut.writeInt(encryptedPacket.length);
            temporaryOut.write(encryptedPacket);
            temporaryOut.flush();

            int respLength = temporaryIn.readInt();
            byte[] respBytes = new byte[respLength];
            temporaryIn.readFully(respBytes);

            return respBytes;
        } catch (IOException e) {
            System.err.println("[TCP Client] Error during transmission: " + e.getMessage());
            return new byte[0];
        }
    }

    private void closeResources() {
        isConnected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}