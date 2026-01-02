package ma.projet.springdatarest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;

@Configuration
public class RestRepositoryConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        // Expose IDs for entities to make HAL responses include ids
        config.exposeIdsFor(ma.projet.springdatarest.domain.Category.class,
                ma.projet.springdatarest.domain.Item.class);
        // Keep default base path '/'
        config.setDefaultPageSize(20);
        config.setMaxPageSize(200);
    }
}
