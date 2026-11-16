package pe.edu.vallegrande.vgmsagrimarket.config;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Ubigeo;
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
public class UbigeoSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UbigeoSeeder.class);

    @Bean
    public CommandLineRunner seedUbigeos(ReactiveMongoTemplate mongoTemplate) {
        return args -> {
            List<Ubigeo> catalog = defaultUbigeos();
            List<Long> ids = catalog.stream().map(Ubigeo::getId).toList();
            mongoTemplate.remove(Query.query(Criteria.where("_id").nin(ids)), Ubigeo.class)
                    .thenMany(Flux.fromIterable(catalog).concatMap(mongoTemplate::save))
                    .then()
                    .doOnSuccess(ignored -> LOGGER.info("Ubigeo catalog seeded successfully"))
                    .doOnError(error -> LOGGER.error("Failed to seed Ubigeo catalog", error))
                    .subscribe();
        };
    }

    private List<Ubigeo> defaultUbigeos() {
        return List.of(
                ubigeo(10L, "Lima", "Cañete", "Asia"),
                ubigeo(11L, "Lima", "Cañete", "Calango"),
                ubigeo(12L, "Lima", "Cañete", "Cerro Azul"),
                ubigeo(13L, "Lima", "Cañete", "Chilca"),
                ubigeo(14L, "Lima", "Cañete", "Coayllo"),
                ubigeo(5L, "Lima", "Cañete", "Imperial"),
                ubigeo(9L, "Lima", "Cañete", "Lunahuaná"),
                ubigeo(15L, "Lima", "Cañete", "Mala"),
                ubigeo(6L, "Lima", "Cañete", "Nuevo Imperial"),
                ubigeo(16L, "Lima", "Cañete", "Pacarán"),
                ubigeo(7L, "Lima", "Cañete", "Quilmaná"),
                ubigeo(17L, "Lima", "Cañete", "San Antonio"),
                ubigeo(8L, "Lima", "Cañete", "San Luis"),
                ubigeo(4L, "Lima", "Cañete", "San Vicente de Cañete"),
                ubigeo(18L, "Lima", "Cañete", "Santa Cruz de Flores"),
                ubigeo(19L, "Lima", "Cañete", "Zúñiga"));
    }

    private Ubigeo ubigeo(Long id, String department, String province, String district) {
        return Ubigeo.builder()
                .id(id)
                .department(department)
                .province(province)
                .district(district)
                .build();
    }
}
