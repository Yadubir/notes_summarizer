package com.app.notes_summarizer.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "summaries")
@Getter
@Setter
public class Summary {

    @Id
    private String id;

    private String originalTranscript;
    private String prompt;
    private String generatedSummary;
    private String editedSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
