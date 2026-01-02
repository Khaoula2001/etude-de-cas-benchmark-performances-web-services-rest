package ma.projet.springdatarest.repository;

import ma.projet.springdatarest.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ma.projet.springdatarest.web.projection.ItemView;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(collectionResourceRel = "items", path = "items", excerptProjection = ItemView.class)
public interface ItemRepository extends JpaRepository<Item, Long> {

    @RestResource(path = "byCategoryId", rel = "byCategoryId")
    Page<Item> findByCategory_Id(@Param("categoryId") Long categoryId, Pageable pageable);

    @RestResource(path = "byCategoryJoin", rel = "byCategoryJoin")
    @Query(value = "select i from Item i join fetch i.category c where c.id = :cid",
           countQuery = "select count(i) from Item i where i.category.id = :cid")
    Page<Item> findByCategoryIdJoinFetch(@Param("cid") Long categoryId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {})
    Page<Item> findAll(Pageable pageable);
}
