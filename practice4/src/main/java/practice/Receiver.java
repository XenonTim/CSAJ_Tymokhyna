package practice;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class Receiver implements ReceiverInterface {
    private final BlockingQueue<byte[]> output_queue;
    private final Encrypter encrypter;
    private final AtomicBoolean isRunning;

    public Receiver(BlockingQueue<byte[]> output_queue, Encrypter encrypter, AtomicBoolean isRunning) {
        this.output_queue = output_queue;
        this.encrypter = encrypter;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                int command = ThreadLocalRandom.current().nextInt(3) + 1;
                int amount = ThreadLocalRandom.current().nextInt(10) + 1;
                
                String mock_json = String.format("{\"command_type\":%d,\"amount\":%d}", command, amount);
                Message mock_message = new Message((byte) 0x12, 1L, 1, 78, mock_json);
                
                byte[] network_packet = encrypter.encrypt(mock_message);
                
                output_queue.put(network_packet);
                
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
