package com.agrimarket.demo.service;

import com.agrimarket.demo.model.mongo.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final ReactiveMongoOperations mongoOperations;

    public Mono<Long> generateSequence(String seqName) {
        Query query = new Query(Criteria.where("_id").is(seqName));
        Update update = new Update().inc("seq", 1);

        return mongoOperations.findAndModify(query, update, options().returnNew(true).upsert(true), Counter.class)
                .map(counter -> counter.getSeq() != null ? counter.getSeq() : 1L)
                .switchIfEmpty(createInitial(seqName));
    }

    private Mono<Long> createInitial(String seqName) {
        Counter initial = Counter.builder().id(seqName).seq(1L).build();
        return mongoOperations.insert(initial).map(counter -> counter.getSeq());
    }
}
