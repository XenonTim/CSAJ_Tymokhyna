package practice;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Storage {
    private final Map<String, Product> database = new ConcurrentHashMap<>();

    public Product create(Product product) {
        if (product.get_id() == null || product.get_id().isEmpty()) {
            product.set_id(UUID.randomUUID().toString());
        }
        database.put(product.get_id(), product);
        return product;
    }

    public Optional<Product> find_by_id(String id) {
        return Optional.ofNullable(database.get(id));
    }

    public Product update(Product product) {
        if (!database.containsKey(product.get_id())) {
            throw new IllegalArgumentException("No product found with ID " + product.get_id());
        }
        database.put(product.get_id(), product);
        return product;
    }

    public boolean delete(String id) {
        return database.remove(id) != null;
    }

    public Page<Product> search(ProductSearchQuery query) {
        List<Product> filtered = database.values().stream()
                .filter(p -> query.name == null || p.get_name().toLowerCase().contains(query.name.toLowerCase()))
                .filter(p -> query.category == null || p.get_category().equalsIgnoreCase(query.category))
                .filter(p -> query.min_quantity == null || p.get_quantity() >= query.min_quantity)
                .filter(p -> query.max_quantity == null || p.get_quantity() <= query.max_quantity)
                .filter(p -> query.min_price == null || p.get_price() >= query.min_price)
                .filter(p -> query.max_price == null || p.get_price() <= query.max_price)
                .collect(Collectors.toList());

        int total_elements = filtered.size();
        int from_index = (query.page - 1) * query.size;

        if (from_index >= total_elements) {
            return new Page<>(Collections.emptyList(), query.page, query.size, total_elements);
        }

        int to_index = Math.min(from_index + query.size, total_elements);
        List<Product> pageContent = filtered.subList(from_index, to_index);

        return new Page<>(pageContent, query.page, query.size, total_elements);
    }
}
