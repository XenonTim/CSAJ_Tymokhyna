package practice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerUDP {
    private final int port;
    private final AtomicBoolean isRunning;
    private final ExecutorService threadPool;

    private final BlockingQueue<byte[]> receiver_to_decrypter = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> decrypter_to_processor = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> processor_to_encrypter = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> encrypter_to_sender = new LinkedBlockingQueue<>();

    public StoreServerUDP(int port) {
        this.port = port;
        this.isRunning = new AtomicBoolean(false);
        this.threadPool = Executors.newFixedThreadPool(6);
    }

    public void start() {
        if (isRunning.getAndSet(true)) return;

        Storage storage = new Storage();
        Encrypter encrypter = new Encrypter();
        Decrypter decrypter = new Decrypter();

        threadPool.submit(new DecrypterWrapper(receiver_to_decrypter, decrypter_to_processor, decrypter, isRunning));
        threadPool.submit(new Processor(decrypter_to_processor, processor_to_encrypter, storage, isRunning));
        threadPool.submit(new EncrypterWrapper(processor_to_encrypter, encrypter_to_sender, encrypter, isRunning));

        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            System.out.println("[UDP Server Pipeline] Started on port " + port);
            threadPool.submit(() -> {
                byte[] sendBuffer;
                while (isRunning.get()) {
                    try {
                        byte[] readyPacket = encrypter_to_sender.poll(100, TimeUnit.MILLISECONDS);
                        if (readyPacket != null) {
                            DatagramPacket sendPacket = new DatagramPacket(readyPacket, readyPacket.length,
                                    InetAddress.getByName("localhost"), 9998);
                            serverSocket.send(sendPacket);
                        }
                    } catch (Exception ignored) {}
                }
            });

            byte[] receiveBuffer = new byte[1024];
            while (isRunning.get()) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                byte[] data = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, data, 0, data.length);

                receiver_to_decrypter.put(data);
            }
        } catch (IOException | InterruptedException e) {
            if (isRunning.get()) System.err.println("[UDP Server] Error: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning.set(false);
        threadPool.shutdownNow();
        System.out.println("[UDP Server Pipeline] Stopped.");
    }
}
