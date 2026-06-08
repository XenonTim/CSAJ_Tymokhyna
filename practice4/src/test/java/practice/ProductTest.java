package practice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ProductTest {
    private Storage service;

    @BeforeEach
    public void setUp() {
        service = new Storage();
        service.create(new Product("1", "Phone", "Electronics", 50, 999.0));
        service.create(new Product("2", "Headphones", "Electronics", 15, 350.0));
        service.create(new Product("3", "Milk", "Food", 200, 1.5));
        service.create(new Product("4", "Chocolate", "Food", 120, 2.5));
        service.create(new Product("5", "Bread", "Food", 80, 1.2));
    }

    @Test
    public void shouldCreateAndFindProduct() {
        Product newProduct = new Product("6", "Laptop", "Electronics", 10, 1200.0);
        service.create(newProduct);

        var found = service.find_by_id("6");
        assertThat(found).isPresent();
        assertThat(found.get().get_name()).isEqualTo("Laptop");
    }

    @Test
    public void shouldFilterByCategory() {
        ProductSearchQuery query = new ProductSearchQuery();
        query.category = "Food";

        Page<Product> result = service.search(query);

        assertThat(result.get_total_elements()).isEqualTo(3);
        assertThat(result.get_content()).allMatch(p -> p.get_category().equals("Food"));
    }

    @Test
    public void shouldFilterByNameAndPriceRange() {
        ProductSearchQuery query = new ProductSearchQuery();
        query.name = "Phone";
        query.min_price = 500.0;
        query.max_price = 1500.0;

        Page<Product> result = service.search(query);

        assertThat(result.get_total_elements()).isEqualTo(1);
        assertThat(result.get_content().get(0).get_name()).contains("Phone");
    }

    @Test
    public void shouldFilterByMinPrice() {
        ProductSearchQuery query = new ProductSearchQuery();
        query.min_price = 900.0;

        Page<Product> result = service.search(query);

        assertThat(result.get_total_elements()).isEqualTo(1);
        assertThat(result.get_content().get(0).get_name()).isEqualTo("Phone");
    }

    @Test
    public void shouldPaginateResults() {
        ProductSearchQuery query = new ProductSearchQuery();
        query.category = "Food";
        query.page = 1;
        query.size = 2;

        Page<Product> firstPage = service.search(query);
        assertThat(firstPage.get_content().size()).isEqualTo(2);
        assertThat(firstPage.get_total_pages()).isEqualTo(2);

        query.page = 2;
        Page<Product> secondPage = service.search(query);
        assertThat(secondPage.get_content().size()).isEqualTo(1);
    }
}
