package ma.projet.restcontroller.web;

import ma.projet.restcontroller.domain.Category;
import ma.projet.restcontroller.domain.Item;
import ma.projet.restcontroller.repository.CategoryRepository;
import ma.projet.restcontroller.repository.ItemRepository;
import ma.projet.restcontroller.web.dto.CategoryDto;
import ma.projet.restcontroller.web.dto.ItemDto;
import ma.projet.restcontroller.web.dto.PageResponse;
import ma.projet.restcontroller.web.mapper.DtoMappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public CategoryController(CategoryRepository categoryRepository, ItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public PageResponse<CategoryDto> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> p = categoryRepository.findAll(pageable);
        return PageResponse.from(p, DtoMappers::toDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> get(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(c -> ResponseEntity.ok(DtoMappers.toDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CategoryDto> create(@Validated @RequestBody CategoryDto dto) {
        Category c = new Category();
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        Category saved = categoryRepository.save(c);
        return ResponseEntity.created(URI.create("/categories/" + saved.getId())).body(DtoMappers.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @Validated @RequestBody CategoryDto dto) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Category c = opt.get();
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        return ResponseEntity.ok(DtoMappers.toDto(categoryRepository.save(c)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) return ResponseEntity.notFound().build();
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // relation: /categories/{id}/items
    @GetMapping("/{id}/items")
    public ResponseEntity<PageResponse<ItemDto>> itemsOfCategory(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        if (!categoryRepository.existsById(id)) return ResponseEntity.notFound().build();
        Pageable pageable = PageRequest.of(page, size);
        Page<Item> p = itemRepository.findByCategory_Id(id, pageable);
        return ResponseEntity.ok(PageResponse.from(p, DtoMappers::toDto));
    }
}
