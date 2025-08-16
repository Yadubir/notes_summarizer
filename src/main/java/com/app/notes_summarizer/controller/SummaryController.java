package com.app.notes_summarizer.controller;

import com.app.notes_summarizer.DTOs.CreateSummaryRequest;
import com.app.notes_summarizer.DTOs.UpdateSummaryRequest;
import com.app.notes_summarizer.model.Summary;
import com.app.notes_summarizer.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/summaries")
@CrossOrigin(origins = "http://localhost:5173")
public class SummaryController {

    private final SummaryService summaryService;

    @Autowired
    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    // (post) generate a summary.
    @PostMapping
    public ResponseEntity<Summary> generateSummary(@RequestBody CreateSummaryRequest request) {
        Summary summary = summaryService.generateAndSaveSummary(request.transcript, request.prompt);
        return ResponseEntity.ok(summary);
    }

    // (get) retrieve a summary by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<Summary> getSummary(@PathVariable String id) {
        return summaryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // (PUT) update a summary.
    @PutMapping("/{id}")
    public ResponseEntity<Summary> updateSummary(@PathVariable String id, @RequestBody UpdateSummaryRequest request) {
        Summary updatedSummary = summaryService.updateSummary(id, request.editedSummary);
        return ResponseEntity.ok(updatedSummary);
    }

    // (POST) share a summary via email.
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareSummary(@PathVariable String id, @RequestBody Map<String, String> body) {
        String recipientEmail = body.get("recipientEmail");
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Recipient email is required.");
        }
        try {
            summaryService.shareSummary(id, recipientEmail);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send email: " + e.getMessage());
        }
    }
}
