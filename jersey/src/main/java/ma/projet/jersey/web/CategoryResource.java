package ma.projet.jersey.web;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ma.projet.jersey.domain.Category;
import ma.projet.jersey.domain.Item;
import ma.projet.jersey.repository.CategoryRepository;
import ma.projet.jersey.repository.ItemRepository;
import ma.projet.jersey.web.dto.CategoryDto;
import ma.projet.jersey.web.dto.ItemDto;
import ma.projet.jersey.web.dto.PageResponse;
import ma.projet.jersey.web.mapper.DtoMappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public CategoryResource(CategoryRepository categoryRepository, ItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @GET
    public PageResponse<CategoryDto> list(@QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> p = categoryRepository.findAll(pageable);
        return PageResponse.from(p, DtoMappers::toDto);
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return categoryRepository.findById(id)
                .map(c -> Response.ok(DtoMappers.toDto(c)))
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    @Transactional
    public Response create(@Valid CategoryDto dto) {
        Category c = new Category();
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        Category saved = categoryRepository.save(c);
        return Response.created(URI.create("/categories/" + saved.getId())).entity(DtoMappers.toDto(saved)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, @Valid CategoryDto dto) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) return Response.status(Response.Status.NOT_FOUND).build();
        Category c = opt.get();
        c.setCode(dto.getCode());
        c.setName(dto.getName());
        Category saved = categoryRepository.save(c);
        return Response.ok(DtoMappers.toDto(saved)).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        if (!categoryRepository.existsById(id)) return Response.status(Response.Status.NOT_FOUND).build();
        categoryRepository.deleteById(id);
        return Response.noContent().build();
    }

    // relation: /categories/{id}/items
    @GET
    @Path("/{id}/items")
    public Response itemsOfCategory(@PathParam("id") Long id,
                                    @QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("20") int size) {
        if (!categoryRepository.existsById(id)) return Response.status(Response.Status.NOT_FOUND).build();
        Pageable pageable = PageRequest.of(page, size);
        Page<Item> items = itemRepository.findByCategory_Id(id, pageable);
        return Response.ok(PageResponse.from(items, DtoMappers::toDto)).build();
    }
}
