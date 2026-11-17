package pe.edu.vallegrande.agrimarket.repository.mongo;

import pe.edu.vallegrande.agrimarket.model.mongo.AuthSession;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AuthSessionRepository extends ReactiveMongoRepository<AuthSession, String> {

    Mono<AuthSession> findByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    Mono<Void> deleteByTokenHash(String tokenHash);
}
