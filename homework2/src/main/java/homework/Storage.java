package homework;
import java.util.concurrent.atomic.AtomicInteger;

public class Storage {
    private final AtomicInteger product_amount = new AtomicInteger(1000);

    public int get_balance() {
        return product_amount.get();
    }

    public int deduct_product(int count) {
        return product_amount.addAndGet(-count);
    }

    public int add_product(int count) {
        return product_amount.addAndGet(count);
    }
}
