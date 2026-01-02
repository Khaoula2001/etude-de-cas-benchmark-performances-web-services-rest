package ma.projet.jersey.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ItemDto {
    private Long id; // lecture seule
    @NotBlank
    private String sku;
    @NotBlank
    private String name;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;
    private int stock;
    @NotNull
    private Long categoryId;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
