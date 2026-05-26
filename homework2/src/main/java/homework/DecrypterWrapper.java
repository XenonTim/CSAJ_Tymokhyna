package homework;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DecrypterWrapper implements Runnable {
    private final BlockingQueue<byte[]> input_queue;
    private final BlockingQueue<Message> output_queue;
    private final Decrypter decrypter;
    private final AtomicBoolean isRunning;

    public DecrypterWrapper(BlockingQueue<byte[]> input_queue, BlockingQueue<Message> output_queue, Decrypter decrypter, AtomicBoolean isRunning) {
        this.input_queue = input_queue;
        this.output_queue = output_queue;
        this.decrypter = decrypter;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get() || !input_queue.isEmpty()) {
            try {
                byte[] packet = input_queue.poll(100, TimeUnit.MILLISECONDS);
                if (packet != null) {
                    Message decrypted = decrypter.decrypt(packet);
                    output_queue.put(decrypted);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Decryption error: " + e.getMessage());
            }
        }
    }
}