package pe.edu.vallegrande.agrimarket.config;

import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
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
public class AdministrativeDivisionSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministrativeDivisionSeeder.class);

    @Bean
    public CommandLineRunner seedAdministrativeDivisions(ReactiveMongoTemplate mongoTemplate) {
        return args -> {
            List<AdministrativeDivision> catalog = defaultAdministrativeDivisions();
            List<Long> ids = catalog.stream().map(AdministrativeDivision::getId).toList();
            mongoTemplate.remove(Query.query(Criteria.where("_id").nin(ids)), AdministrativeDivision.class)
                    .thenMany(Flux.fromIterable(catalog).concatMap(mongoTemplate::save))
                    .then()
                    .doOnSuccess(ignored -> LOGGER.info("AdministrativeDivision catalog seeded successfully"))
                    .doOnError(error -> LOGGER.error("Failed to seed AdministrativeDivision catalog", error))
                    .subscribe();
        };
    }

    private List<AdministrativeDivision> defaultAdministrativeDivisions() {
        return List.of(
                administrativeDivision(10L, "Lima", "Cañete", "Asia"),
                administrativeDivision(11L, "Lima", "Cañete", "Calango"),
                administrativeDivision(12L, "Lima", "Cañete", "Cerro Azul"),
                administrativeDivision(13L, "Lima", "Cañete", "Chilca"),
                administrativeDivision(14L, "Lima", "Cañete", "Coayllo"),
                administrativeDivision(5L, "Lima", "Cañete", "Imperial"),
                administrativeDivision(9L, "Lima", "Cañete", "Lunahuaná"),
                administrativeDivision(15L, "Lima", "Cañete", "Mala"),
                administrativeDivision(6L, "Lima", "Cañete", "Nuevo Imperial"),
                administrativeDivision(16L, "Lima", "Cañete", "Pacarán"),
                administrativeDivision(7L, "Lima", "Cañete", "Quilmaná"),
                administrativeDivision(17L, "Lima", "Cañete", "San Antonio"),
                administrativeDivision(8L, "Lima", "Cañete", "San Luis"),
                administrativeDivision(4L, "Lima", "Cañete", "San Vicente de Cañete"),
                administrativeDivision(18L, "Lima", "Cañete", "Santa Cruz de Flores"),
                administrativeDivision(19L, "Lima", "Cañete", "Zúñiga"));
    }

    private AdministrativeDivision administrativeDivision(Long id, String department, String province, String district) {
        return AdministrativeDivision.builder()
                .id(id)
                .department(department)
                .province(province)
                .district(district)
                .build();
    }
}
