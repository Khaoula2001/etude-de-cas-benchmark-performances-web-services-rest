package ma.projet.restcontroller.web.mapper;

import ma.projet.restcontroller.domain.Category;
import ma.projet.restcontroller.domain.Item;
import ma.projet.restcontroller.web.dto.CategoryDto;
import ma.projet.restcontroller.web.dto.ItemDto;

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
