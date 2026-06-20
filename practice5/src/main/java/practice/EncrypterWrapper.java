package practice;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EncrypterWrapper implements Runnable {
    private final BlockingQueue<Message> input_queue;
    private final BlockingQueue<byte[]> output_queue;
    private final Encrypter encrypter;
    private final AtomicBoolean isRunning;

    public EncrypterWrapper(BlockingQueue<Message> input_queue, BlockingQueue<byte[]> output_queue, Encrypter encrypter, AtomicBoolean isRunning) {
        this.input_queue = input_queue;
        this.output_queue = output_queue;
        this.encrypter = encrypter;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get() || !input_queue.isEmpty()) {
            try {
                Message responseMsg = input_queue.poll(100, TimeUnit.MILLISECONDS);
                if (responseMsg != null) {
                    byte[] encryptedBytes = encrypter.encrypt(responseMsg);
                    output_queue.put(encryptedBytes);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Encryption error: " + e.getMessage());
            }
        }
    }
}
