package practice;

import java.io.IOException;
import java.net.*;

public class StoreClientUDP {
    private final String host;
    private final int port;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 1000;

    public StoreClientUDP(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public byte[] sendCommandWithRetry(byte[] encryptedPacket) {
        try (DatagramSocket clientSocket = new DatagramSocket(9998)) {
            clientSocket.setSoTimeout(TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(host);

            DatagramPacket sendPacket = new DatagramPacket(encryptedPacket, encryptedPacket.length, address, port);
            byte[] responseBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    attempts++;
                    clientSocket.send(sendPacket);

                    clientSocket.receive(receivePacket);

                    byte[] responseData = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(), 0, responseData, 0, responseData.length);
                    return responseData;

                } catch (SocketTimeoutException e) {
                    System.err.println("[UDP Client] Packet lost or timeout reached! Attempt " + attempts + " failed. Retrying...");
                }
            }
            throw new RuntimeException("[UDP Client] severe error: Data packet lost completely after " + MAX_RETRIES + " retries.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
