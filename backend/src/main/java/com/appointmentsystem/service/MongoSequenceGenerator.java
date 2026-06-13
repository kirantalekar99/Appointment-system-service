package com.appointmentsystem.service;

import com.appointmentsystem.model.MongoSequence;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class MongoSequenceGenerator {

    private final MongoOperations mongoOperations;

    public MongoSequenceGenerator(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long nextId(String sequenceName) {
        MongoSequence sequence = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(sequenceName)),
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                MongoSequence.class
        );

        return sequence == null ? 1L : sequence.getSequence();
    }
}
