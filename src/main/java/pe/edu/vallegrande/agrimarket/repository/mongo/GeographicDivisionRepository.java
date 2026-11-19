package pe.edu.vallegrande.agrimarket.repository.mongo;

import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface GeographicDivisionRepository extends ReactiveMongoRepository<GeographicDivision, Long> {

    Flux<GeographicDivision> findByDepartment(String department);

    Flux<GeographicDivision> findByDepartmentAndProvince(String department, String province);
}
