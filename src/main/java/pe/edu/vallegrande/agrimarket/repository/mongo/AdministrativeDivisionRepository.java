package pe.edu.vallegrande.agrimarket.repository.mongo;

import pe.edu.vallegrande.agrimarket.model.mongo.AdministrativeDivision;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AdministrativeDivisionRepository extends ReactiveMongoRepository<AdministrativeDivision, Long> {

    Flux<AdministrativeDivision> findByDepartment(String department);

    Flux<AdministrativeDivision> findByDepartmentAndProvince(String department, String province);
}
