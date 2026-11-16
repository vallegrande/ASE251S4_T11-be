package pe.edu.vallegrande.vgmsagrimarket.repository.mongo;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.Cart;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends ReactiveMongoRepository<Cart, String> {

    Mono<Cart> findByCustomerId(String customerId);

    Mono<Cart> findBySessionToken(String sessionToken);
}
