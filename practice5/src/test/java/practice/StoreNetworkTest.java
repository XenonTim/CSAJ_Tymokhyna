package practice;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

public class StoreNetworkTest {

    @Test
    public void shouldHandleConcurrentTCPClients() throws InterruptedException {
        int port = 8555;
        StoreServerTCP server = new StoreServerTCP(port);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        TimeUnit.MILLISECONDS.sleep(500);

        ExecutorService clientPool = Executors.newFixedThreadPool(3);
        Encrypter encrypter = new Encrypter();

        for (int i = 0; i < 3; i++) {
            final int clientId = i;
            clientPool.submit(() -> {
                StoreClientTCP client = new StoreClientTCP("localhost", port);
                client.connect();

                String json = String.format("{\"command_type\":3,\"amount\":%d}", (clientId + 1) * 5);
                Message msg = new Message((byte)0x13, clientId, 1, 200 + clientId, json);

                byte[] responseBytes = client.sendCommand(encrypter.encrypt(msg));
                assertThat(responseBytes).isNotEmpty();
            });
        }

        clientPool.shutdown();
        clientPool.awaitTermination(4, TimeUnit.SECONDS);
        server.stop();
    }

    @Test
    public void shouldHandleHighVolumeTCPConnections() throws InterruptedException {
        int port = 8558;
        StoreServerTCP server = new StoreServerTCP(port);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        TimeUnit.MILLISECONDS.sleep(500);

        int concurrentClients = 15;
        ExecutorService heavyPool = Executors.newFixedThreadPool(concurrentClients);
        Encrypter encrypter = new Encrypter();

        for (int i = 0; i < concurrentClients; i++) {
            final int id = i;
            heavyPool.submit(() -> {
                StoreClientTCP client = new StoreClientTCP("localhost", port);
                client.connect();

                String json = String.format("{\"command_type\":2,\"amount\":%d}", id + 1);
                Message msg = new Message((byte)0x13, id, 1, 500 + id, json);

                byte[] responseBytes = client.sendCommand(encrypter.encrypt(msg));
                assertThat(responseBytes).isNotEmpty();
            });
        }

        heavyPool.shutdown();
        boolean finishedCleanly = heavyPool.awaitTermination(7, TimeUnit.SECONDS);

        assertThat(finishedCleanly).isTrue();
        server.stop();
    }

    @Test
    public void shouldHandleConcurrentUDPClients() throws InterruptedException {
        int port = 9667;
        StoreServerUDP server = new StoreServerUDP(port);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        TimeUnit.MILLISECONDS.sleep(500);

        int clientCount = 3;
        ExecutorService clientPool = Executors.newFixedThreadPool(clientCount);
        Encrypter encrypter = new Encrypter();

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clientPool.submit(() -> {
                StoreClientUDP client = new StoreClientUDP("localhost", port);

                String json = String.format("{\"command_type\":3,\"amount\":%d}", (clientId + 1) * 10);
                Message msg = new Message((byte)0x13, clientId, 1, 300 + clientId, json);

                byte[] response = client.sendCommandWithRetry(encrypter.encrypt(msg));

                assertThat(response).isNotEmpty();
            });
        }

        clientPool.shutdown();
        boolean finishedCleanly = clientPool.awaitTermination(4, TimeUnit.SECONDS);

        assertThat(finishedCleanly).isTrue();
        server.stop();
    }

    @Test
    public void shouldHandleHighVolumeUDPConnections() throws InterruptedException {
        int port = 9668;
        StoreServerUDP server = new StoreServerUDP(port);

        Thread serverThread = new Thread(server::start);
        serverThread.start();
        TimeUnit.MILLISECONDS.sleep(500);

        int heavyLoadClients = 15;
        ExecutorService heavyPool = Executors.newFixedThreadPool(heavyLoadClients);
        Encrypter encrypter = new Encrypter();

        for (int i = 0; i < heavyLoadClients; i++) {
            final int id = i;
            heavyPool.submit(() -> {
                StoreClientUDP client = new StoreClientUDP("localhost", port);

                String json = "{\"command_type\":2,\"amount\":2}";
                Message msg = new Message((byte)0x13, id, 1, 400 + id, json);

                byte[] response = client.sendCommandWithRetry(encrypter.encrypt(msg));
                assertThat(response).isNotEmpty();
            });
        }

        heavyPool.shutdown();

        boolean finishedCleanly = heavyPool.awaitTermination(7, TimeUnit.SECONDS);

        assertThat(finishedCleanly).isTrue();
        server.stop();
    }
}
