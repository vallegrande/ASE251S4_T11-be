package pe.edu.vallegrande.agrimarket.repository.mongo;

import pe.edu.vallegrande.agrimarket.model.mongo.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, Long> {

    Flux<Product> findByIsActive(Boolean isActive);
}
