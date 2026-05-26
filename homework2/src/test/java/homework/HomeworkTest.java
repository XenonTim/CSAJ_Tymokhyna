package homework;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

public class HomeworkTest {

    private final AtomicInteger total_inquiries = new AtomicInteger(0);
    private final AtomicInteger total_deductions = new AtomicInteger(0);
    private final AtomicInteger total_additions = new AtomicInteger(0);
    private final AtomicInteger total_amount_deducted = new AtomicInteger(0);
    private final AtomicInteger total_amount_added = new AtomicInteger(0);

    @Test
    public void shouldMaintainStrictThreadSafetyWithZeroFileChanges() throws InterruptedException {
        int r_count = 2, d_count = 2, p_count = 4, e_count = 3, s_count = 5;
        
        BlockingQueue<byte[]> receiver_to_decrypter = new LinkedBlockingQueue<>();
        BlockingQueue<Message> decrypter_to_processor = new LinkedBlockingQueue<>();
        BlockingQueue<Message> processor_to_encrypter = new LinkedBlockingQueue<>();
        BlockingQueue<byte[]> encrypter_to_sender = new LinkedBlockingQueue<>();

        BlockingQueue<Message> spy_decrypter_to_processor = new LinkedBlockingQueue<Message>() {
            @Override
            public void put(Message msg) throws InterruptedException {
                try {
                    String json_str = msg.get_message_string();
                    
                    if (json_str != null) {
                        String clean = json_str.replaceAll("\\s+", "");

                        int command_type = 1;
                        int amount = 0;

                        if (clean.contains("\"command_type\":2")) {
                            command_type = 2;
                        } else if (clean.contains("\"command_type\":3")) {
                            command_type = 3;
                        }

                        if (clean.contains("\"amount\":")) {
                            String amount_part = clean.substring(clean.indexOf("\"amount\":"));

                            String digits = amount_part.replaceAll("[^0-9]", "");
                            if (!digits.isEmpty()) {
                                amount = Integer.parseInt(digits);
                            }
                        }

                        if (command_type == 1) {
                            total_inquiries.incrementAndGet();
                        }
                        if (command_type == 2) {
                            total_deductions.incrementAndGet();
                            total_amount_deducted.addAndGet(amount);
                        }
                        if (command_type == 3) {
                            total_additions.incrementAndGet();
                            total_amount_added.addAndGet(amount);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Counting error: " + e.getMessage());
                }
                super.put(msg);
            }
        };

        Storage storage = new Storage();
        Encrypter encrypter = new Encrypter();
        Decrypter decrypter = new Decrypter();
        AtomicBoolean isRunning = new AtomicBoolean(true);

        int totalThreads = r_count + d_count + p_count + e_count + s_count;
        ExecutorService thread_pool = Executors.newFixedThreadPool(totalThreads);

        int initial_balance = storage.get_balance();

        for (int i = 0; i < r_count; i++)
            thread_pool.submit(new Receiver(receiver_to_decrypter, encrypter, isRunning));
        
        for (int i = 0; i < d_count; i++)
            thread_pool.submit(new DecrypterWrapper(receiver_to_decrypter, spy_decrypter_to_processor, decrypter, isRunning));
        
        for (int i = 0; i < p_count; i++)
            thread_pool.submit(new Processor(spy_decrypter_to_processor, processor_to_encrypter, storage, isRunning));
        
        for (int i = 0; i < e_count; i++)
            thread_pool.submit(new EncrypterWrapper(processor_to_encrypter, encrypter_to_sender, encrypter, isRunning));
        
        for (int i = 0; i < s_count; i++)
            thread_pool.submit(new Sender(encrypter_to_sender, isRunning));

        TimeUnit.MILLISECONDS.sleep(3000);

        isRunning.set(false);
        thread_pool.shutdown();
        if (!thread_pool.awaitTermination(3, TimeUnit.SECONDS)) {
            thread_pool.shutdownNow();
        }

        int final_balance = storage.get_balance();
        int expected_balance = initial_balance + total_amount_added.get() - total_amount_deducted.get();

        assertThat(final_balance).isEqualTo(expected_balance);

        assertThat(total_deductions.get() + total_additions.get()).isGreaterThan(0);
    }
}