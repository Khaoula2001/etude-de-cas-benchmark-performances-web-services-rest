package ma.projet.springdatarest.web.projection;

import ma.projet.springdatarest.domain.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.math.BigDecimal;

@Projection(name = "itemView", types = Item.class)
public interface ItemView {
    Long getId();
    String getSku();
    String getName();
    BigDecimal getPrice();
    int getStock();
    String getDescription();

    @Value("#{target.category.id}")
    Long getCategoryId();
}
