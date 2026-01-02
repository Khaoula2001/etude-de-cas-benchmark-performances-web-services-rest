package ma.projet.jersey.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // Scan resources in this package
        packages("ma.projet.jersey.web");
    }
}
