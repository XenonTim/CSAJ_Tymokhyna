package practice;

import java.util.List;

public class Page<T> {
    private final List<T> content;
    private final int page_number;
    private final int page_size;
    private final int total_elements;

    public Page(List<T> content, int page_number, int page_size, int total_elements) {
        this.content = content;
        this.page_number = page_number;
        this.page_size = page_size;
        this.total_elements = total_elements;
    }

    public List<T> get_content() { return content; }
    public int get_page_number() { return page_number; }
    public int get_page_size() { return page_size; }
    public int get_total_pages() { return (int) Math.ceil((double) total_elements / page_size); }
    public int get_total_elements() { return total_elements; }
}
