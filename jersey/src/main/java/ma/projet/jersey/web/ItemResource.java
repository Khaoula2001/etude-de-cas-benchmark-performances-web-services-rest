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
import ma.projet.jersey.web.dto.ItemDto;
import ma.projet.jersey.web.dto.PageResponse;
import ma.projet.jersey.web.mapper.DtoMappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

@Component
@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {
    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.items.join-fetch.enabled:false}")
    private boolean joinFetchEnabled;

    public ItemResource(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @GET
    public PageResponse<ItemDto> list(@QueryParam("categoryId") Long categoryId,
                           @QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("20") int size) {
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

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        return itemRepository.findById(id)
                .map(i -> Response.ok(DtoMappers.toDto(i)))
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    @Transactional
    public Response create(@Valid ItemDto dto) {
        Optional<Category> category = categoryRepository.findById(dto.getCategoryId());
        if (category.isEmpty()) return Response.status(Response.Status.BAD_REQUEST).build();
        Item i = new Item();
        copy(dto, i, category.get());
        Item saved = itemRepository.save(i);
        return Response.created(URI.create("/items/" + saved.getId())).entity(DtoMappers.toDto(saved)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, @Valid ItemDto dto) {
        Optional<Item> opt = itemRepository.findById(id);
        if (opt.isEmpty()) return Response.status(Response.Status.NOT_FOUND).build();
        Optional<Category> category = categoryRepository.findById(dto.getCategoryId());
        if (category.isEmpty()) return Response.status(Response.Status.BAD_REQUEST).build();
        Item i = opt.get();
        copy(dto, i, category.get());
        return Response.ok(DtoMappers.toDto(itemRepository.save(i))).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        if (!itemRepository.existsById(id)) return Response.status(Response.Status.NOT_FOUND).build();
        itemRepository.deleteById(id);
        return Response.noContent().build();
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
