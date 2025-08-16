package com.app.notes_summarizer.repository;

import com.app.notes_summarizer.model.Summary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SummaryRepository extends MongoRepository<Summary, String> {
}
