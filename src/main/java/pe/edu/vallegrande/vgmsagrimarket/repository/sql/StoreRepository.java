package pe.edu.vallegrande.vgmsagrimarket.repository.sql;

import pe.edu.vallegrande.vgmsagrimarket.model.sql.Store;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface StoreRepository extends ReactiveCrudRepository<Store, Integer> {

    Mono<Boolean> existsById(Integer storeId);

    Flux<Store> findByIsActiveTrue();

    Mono<Store> findByStoreIdAndIsActiveTrue(Integer storeId);

    Mono<Boolean> existsByStoreIdAndIsActiveTrue(Integer storeId);
}
