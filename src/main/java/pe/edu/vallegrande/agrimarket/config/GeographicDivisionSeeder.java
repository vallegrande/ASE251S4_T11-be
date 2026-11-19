package pe.edu.vallegrande.agrimarket.config;

import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class GeographicDivisionSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeographicDivisionSeeder.class);

    @Bean
    public CommandLineRunner seedGeographicDivisions(ReactiveMongoTemplate mongoTemplate) {
        return args -> {
            List<GeographicDivision> catalog = defaultGeographicDivisions();
            List<Long> ids = catalog.stream().map(GeographicDivision::getId).toList();
            mongoTemplate.remove(Query.query(Criteria.where("_id").nin(ids)), GeographicDivision.class)
                    .thenMany(Flux.fromIterable(catalog).concatMap(mongoTemplate::save))
                    .then()
                    .doOnSuccess(ignored -> LOGGER.info("GeographicDivision catalog seeded successfully"))
                    .doOnError(error -> LOGGER.error("Failed to seed GeographicDivision catalog", error))
                    .subscribe();
        };
    }

    private List<GeographicDivision> defaultGeographicDivisions() {
        return List.of(
                geographicDivision(10L, "Lima", "Cañete", "Asia"),
                geographicDivision(11L, "Lima", "Cañete", "Calango"),
                geographicDivision(12L, "Lima", "Cañete", "Cerro Azul"),
                geographicDivision(13L, "Lima", "Cañete", "Chilca"),
                geographicDivision(14L, "Lima", "Cañete", "Coayllo"),
                geographicDivision(5L, "Lima", "Cañete", "Imperial"),
                geographicDivision(9L, "Lima", "Cañete", "Lunahuaná"),
                geographicDivision(15L, "Lima", "Cañete", "Mala"),
                geographicDivision(6L, "Lima", "Cañete", "Nuevo Imperial"),
                geographicDivision(16L, "Lima", "Cañete", "Pacarán"),
                geographicDivision(7L, "Lima", "Cañete", "Quilmaná"),
                geographicDivision(17L, "Lima", "Cañete", "San Antonio"),
                geographicDivision(8L, "Lima", "Cañete", "San Luis"),
                geographicDivision(4L, "Lima", "Cañete", "San Vicente de Cañete"),
                geographicDivision(18L, "Lima", "Cañete", "Santa Cruz de Flores"),
                geographicDivision(19L, "Lima", "Cañete", "Zúñiga"));
    }

    private GeographicDivision geographicDivision(Long id, String department, String province, String district) {
        return GeographicDivision.builder()
                .id(id)
                .department(department)
                .province(province)
                .district(district)
                .build();
    }
}
