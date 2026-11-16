package pe.edu.vallegrande.vgmsagrimarket.repository.mongo;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.StaffUser;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StaffUserRepository extends ReactiveMongoRepository<StaffUser, String> {

    Mono<StaffUser> findByEmailIgnoreCase(String email);

    Mono<Boolean> existsByEmailIgnoreCase(String email);
}
