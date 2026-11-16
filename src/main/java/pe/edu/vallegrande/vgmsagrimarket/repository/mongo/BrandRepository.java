package pe.edu.vallegrande.vgmsagrimarket.repository.mongo;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Brand;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BrandRepository extends ReactiveMongoRepository<Brand, Long> {

    Flux<Brand> findByIsActive(Boolean isActive);
}
