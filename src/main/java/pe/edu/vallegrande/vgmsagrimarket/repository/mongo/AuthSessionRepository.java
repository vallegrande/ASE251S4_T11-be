package pe.edu.vallegrande.vgmsagrimarket.repository.mongo;

import pe.edu.vallegrande.vgmsagrimarket.model.mongo.AuthSession;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AuthSessionRepository extends ReactiveMongoRepository<AuthSession, String> {

    Mono<AuthSession> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    Mono<Void> deleteByTokenHash(String tokenHash);
}
