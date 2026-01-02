package ma.projet.restcontroller.web;

import ma.projet.restcontroller.domain.Category;
import ma.projet.restcontroller.domain.Item;
import ma.projet.restcontroller.repository.CategoryRepository;
import ma.projet.restcontroller.repository.ItemRepository;
import ma.projet.restcontroller.web.dto.ItemDto;
import ma.projet.restcontroller.web.dto.PageResponse;
import ma.projet.restcontroller.web.mapper.DtoMappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/items")
public class ItemController {
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.items.join-fetch.enabled:false}")
    private boolean joinFetchEnabled;

    public ItemController(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public PageResponse<ItemDto> list(@RequestParam(required = false) Long categoryId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Item> p;
        if (categoryId != null) {
            p = joinFetchEnabled
                    ? itemRepository.findByCategoryIdJoinFetch(categoryId, pageable)
                    : itemRepository.findByCategory_Id(categoryId, pageable);
        } else {
            p = itemRepository.findAll(pageable);
        }
        return PageResponse.from(p, DtoMappers::toDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> get(@PathVariable Long id) {
        return itemRepository.findById(id)
                .map(i -> ResponseEntity.ok(DtoMappers.toDto(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ItemDto> create(@Validated @RequestBody ItemDto dto) {
        Optional<Category> category = categoryRepository.findById(dto.getCategoryId());
        if (category.isEmpty()) return ResponseEntity.badRequest().build();
        Item i = new Item();
        copy(dto, i, category.get());
        Item saved = itemRepository.save(i);
        return ResponseEntity.created(URI.create("/items/" + saved.getId())).body(DtoMappers.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> update(@PathVariable Long id, @Validated @RequestBody ItemDto dto) {
        Optional<Item> opt = itemRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Optional<Category> category = categoryRepository.findById(dto.getCategoryId());
        if (category.isEmpty()) return ResponseEntity.badRequest().build();
        Item i = opt.get();
        copy(dto, i, category.get());
        return ResponseEntity.ok(DtoMappers.toDto(itemRepository.save(i)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) return ResponseEntity.notFound().build();
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static void copy(ItemDto dto, Item i, Category category) {
        i.setSku(dto.getSku());
        i.setName(dto.getName());
        i.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        i.setStock(dto.getStock());
        i.setCategory(category);
        i.setDescription(dto.getDescription());
    }
}
