package homework;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Sender implements SenderInterface {
    private final BlockingQueue<byte[]> input_queue;
    private final AtomicBoolean isRunning;

    public Sender(BlockingQueue<byte[]> input_queue, AtomicBoolean isRunning) {
        this.input_queue = input_queue;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get() || !input_queue.isEmpty()) {
            try {
                byte[] ready_packet = input_queue.poll(100, TimeUnit.MILLISECONDS);
                if (ready_packet != null) {
                    System.out.printf("[Network Sender Thread-%d] Successfully sent encrypted response. Packet size: %d bytes.%n", 
                                    Thread.currentThread().getId(), ready_packet.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}