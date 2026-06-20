package practice;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerPipeline {
    private final BlockingQueue<byte[]> receiver_to_decryptor = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> decrypter_to_processor = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> processor_to_encryptor = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> encrypter_to_sender = new LinkedBlockingQueue<>();

    private final Storage storage = new Storage();
    private final Encrypter encrypter = new Encrypter();
    private final Decrypter decrypter = new Decrypter();
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ExecutorService thread_pool;

    private final int r_count, d_count, p_count, e_count, s_count;

    public ServerPipeline(int r_count, int d_count, int p_count, int e_count, int s_count) {
        this.r_count = r_count;
        this.d_count = d_count;
        this.p_count = p_count;
        this.e_count = e_count;
        this.s_count = s_count;
    }

    public void start() {
        if (isRunning.getAndSet(true)) return;

        int total_threads = r_count + d_count + p_count + e_count + s_count;
        thread_pool = Executors.newFixedThreadPool(total_threads);

        for (int i = 0; i < r_count; i++) thread_pool.submit(new Receiver(receiver_to_decryptor, encrypter, isRunning));
        for (int i = 0; i < d_count; i++) thread_pool.submit(new DecrypterWrapper(receiver_to_decryptor, decrypter_to_processor, decrypter, isRunning));
        for (int i = 0; i < p_count; i++) thread_pool.submit(new Processor(decrypter_to_processor, processor_to_encryptor, storage, isRunning));
        for (int i = 0; i < e_count; i++) thread_pool.submit(new EncrypterWrapper(processor_to_encryptor, encrypter_to_sender, encrypter, isRunning));
        for (int i = 0; i < s_count; i++) thread_pool.submit(new Sender(encrypter_to_sender, isRunning));
    }

    public void stop() {
        isRunning.set(false);
        if (thread_pool != null) {
            thread_pool.shutdown();
            try {
                if (!thread_pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    thread_pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                thread_pool.shutdownNow();
            }
        }
    }

    public Storage get_storage() { return storage; }
}
