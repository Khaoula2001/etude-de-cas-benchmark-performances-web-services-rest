package ma.projet.jersey.web.mapper;

import ma.projet.jersey.domain.Category;
import ma.projet.jersey.domain.Item;
import ma.projet.jersey.web.dto.CategoryDto;
import ma.projet.jersey.web.dto.ItemDto;

public final class DtoMappers {
    private DtoMappers() {}

    public static CategoryDto toDto(Category c) {
        if (c == null) return null;
        CategoryDto dto = new CategoryDto();
        dto.setId(c.getId());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        return dto;
    }

    public static ItemDto toDto(Item i) {
        if (i == null) return null;
        ItemDto dto = new ItemDto();
        dto.setId(i.getId());
        dto.setSku(i.getSku());
        dto.setName(i.getName());
        dto.setPrice(i.getPrice());
        dto.setStock(i.getStock());
        if (i.getCategory() != null) {
            dto.setCategoryId(i.getCategory().getId());
        }
        dto.setDescription(i.getDescription());
        return dto;
    }
}
