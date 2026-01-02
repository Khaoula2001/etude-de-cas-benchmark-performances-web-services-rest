package ma.projet.springdatarest.repository;

import ma.projet.springdatarest.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import ma.projet.springdatarest.web.projection.CategoryView;

@RepositoryRestResource(collectionResourceRel = "categories", path = "categories", excerptProjection = CategoryView.class)
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
