package ma.projet.springdatarest.web.projection;

import ma.projet.springdatarest.domain.Category;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "categoryView", types = Category.class)
public interface CategoryView {
    Long getId();
    String getCode();
    String getName();
}
