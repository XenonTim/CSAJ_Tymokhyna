package practice;

public class Product {
    private String id;
    private String name;
    private String category;
    private int quantity;
    private double price;

    public Product() {}

    public Product(String id, String name, String category, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
    }

    public String get_id() { return id; }
    public void set_id(String id) { this.id = id; }
    public String get_name() { return name; }
    public void set_name(String name) { this.name = name; }
    public String get_category() { return category; }
    public void set_category(String category) { this.category = category; }
    public int get_quantity() { return quantity; }
    public void set_quantity(int quantity) { this.quantity = quantity; }
    public double get_price() { return price; }
    public void set_price(double price) { this.price = price; }
}
