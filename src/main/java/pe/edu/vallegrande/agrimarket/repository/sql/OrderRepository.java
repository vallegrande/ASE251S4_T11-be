package pe.edu.vallegrande.agrimarket.repository.sql;

import pe.edu.vallegrande.agrimarket.model.sql.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<Order, Integer> {

    Mono<Order> findByCheckoutKey(String checkoutKey);

    Flux<Order> findByCustomerId(String customerId);
}
