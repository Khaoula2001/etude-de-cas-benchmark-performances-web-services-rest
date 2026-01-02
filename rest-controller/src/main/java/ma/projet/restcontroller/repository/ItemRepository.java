package ma.projet.restcontroller.repository;

import ma.projet.restcontroller.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> findByCategory_Id(Long categoryId, Pageable pageable);

    @Query(value = "select i from Item i join fetch i.category c where c.id = :cid",
           countQuery = "select count(i) from Item i where i.category.id = :cid")
    Page<Item> findByCategoryIdJoinFetch(@Param("cid") Long categoryId, Pageable pageable);
}
