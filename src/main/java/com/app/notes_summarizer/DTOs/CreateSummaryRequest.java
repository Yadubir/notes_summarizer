package com.app.notes_summarizer.DTOs;

import lombok.Data;

@Data
public class CreateSummaryRequest {
    public String transcript;
    public String prompt;
}
