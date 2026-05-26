package homework;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<Message> input_queue;
    private final BlockingQueue<Message> output_queue;
    private final Storage storage;
    private final AtomicBoolean isRunning;
    private final ObjectMapper object_mapper = new ObjectMapper();

    public Processor(BlockingQueue<Message> input_queue, BlockingQueue<Message> output_queue, Storage storage, AtomicBoolean isRunning) {
        this.input_queue = input_queue;
        this.output_queue = output_queue;
        this.storage = storage;
        this.isRunning = isRunning;
    }

    @Override
    public void run() {
        while (isRunning.get() || !input_queue.isEmpty()) {
            try {
                Message msg = input_queue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    String json_Str = msg.get_message_string();
                    Command command = object_mapper.readValue(json_Str, Command.class);
                    
                    int new_balance = 0;
                    String status_text = "OK";

                    switch (command.get_command_type()) {
                        case 1:
                            new_balance = storage.get_balance();
                            status_text = "Inquiry success";
                            break;
                        case 2:
                            new_balance = storage.deduct_product(command.get_amount());
                            status_text = "Deduction success";
                            break;
                        case 3:
                            new_balance = storage.add_product(command.get_amount());
                            status_text = "Addition success";
                            break;
                        default:
                            status_text = "Unknown command";
                    }

                    Message response = new Message(
                        msg.get_unique_identefier(),
                        msg.get_message_number(),
                        msg.get_command_id(),
                        msg.get_user_id(),
                        String.format("%s. Current balance: %d", status_text, new_balance)
                    );
                    
                    output_queue.put(response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Processor error: " + e.getMessage());
            }
        }
    }
}