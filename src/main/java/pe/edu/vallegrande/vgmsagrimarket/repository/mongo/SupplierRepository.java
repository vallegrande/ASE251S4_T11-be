package pe.edu.vallegrande.vgmsagrimarket.repository.mongo;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Supplier;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SupplierRepository extends ReactiveMongoRepository<Supplier, Long> {

    Flux<Supplier> findByIsActive(Boolean isActive);
}
