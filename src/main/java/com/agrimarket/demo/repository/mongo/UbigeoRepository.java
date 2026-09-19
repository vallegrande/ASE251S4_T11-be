package com.agrimarket.demo.repository.mongo;

import com.agrimarket.demo.model.mongo.Ubigeo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UbigeoRepository extends ReactiveMongoRepository<Ubigeo, Long> {

    Flux<Ubigeo> findByDepartment(String department);

    Flux<Ubigeo> findByDepartmentAndProvince(String department, String province);
}
