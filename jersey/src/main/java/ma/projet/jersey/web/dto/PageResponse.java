package ma.projet.jersey.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        PageResponse<D> pr = new PageResponse<>();
        pr.content = page.getContent().stream().map(mapper).toList();
        pr.page = page.getNumber();
        pr.size = page.getSize();
        pr.totalElements = page.getTotalElements();
        pr.totalPages = page.getTotalPages();
        return pr;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
