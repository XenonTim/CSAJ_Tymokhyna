package practice;
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
        this.object_mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run() {
        while (isRunning.get() || !input_queue.isEmpty()) {
            Message msg = null;
            try {
                msg = input_queue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) {
                    String json_Str = msg.get_message_string();

                    int command_id = msg.get_command_id();
                    String status_text = "OK";
                    String response_payload = "";

                    switch (command_id) {
                        case 1:
                            Product new_product = object_mapper.readValue(json_Str, Product.class);
                            Product saved = storage.create(new_product);
                            response_payload = "Created ID: " + saved.get_id();
                            break;

                        case 2:
                            String id_to_find = json_Str.replaceAll("[{}\"\\s]", "").replace("id:", "");
                            response_payload = storage.find_by_id(id_to_find)
                                    .map(p -> "Found: " + p.get_name() + ", Price: " + p.get_price())
                                    .orElse("Product not found");
                            break;

                        case 3:
                            Product product_to_update = object_mapper.readValue(json_Str, Product.class);
                            storage.update(product_to_update);
                            response_payload = "Updated successfully";
                            break;

                        case 4:
                            String idToDelete = json_Str.replaceAll("[{}\"\\s]", "").replace("id:", "");
                            boolean deleted = storage.delete(idToDelete);
                            response_payload = deleted ? "Deleted successfully" : "Delete failed: Not found";
                            break;

                        case 5:
                            ProductSearchQuery query = object_mapper.readValue(json_Str, ProductSearchQuery.class);
                            Page<Product> result_page = storage.search(query);
                            response_payload = String.format("Found total: %d items. Page %d of %d. Items in this page: %d",
                                    result_page.get_total_elements(), result_page.get_page_number(), result_page.get_total_pages(), result_page.get_content().size());
                            break;

                        default:
                            status_text = "Unknown CRUD operation";
                    }

                    Message response = new Message(
                            msg.get_unique_identefier(), msg.get_message_number(),
                            msg.get_command_id(), msg.get_user_id(),
                            String.format("%s. Result: %s", status_text, response_payload)
                    );
                    output_queue.put(response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Processor CRUD error: " + e.getMessage());
                if (msg != null) {
                    try {
                        output_queue.put(new Message(msg.get_unique_identefier(), msg.get_message_number(), msg.get_command_id(), msg.get_user_id(), "ERROR: " + e.getMessage()));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}