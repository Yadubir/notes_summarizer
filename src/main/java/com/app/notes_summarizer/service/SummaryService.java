package com.app.notes_summarizer.service;

import com.app.notes_summarizer.model.Summary;
import com.app.notes_summarizer.repository.SummaryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final JavaMailSender mailSender;
    private final WebClient webClient;
    private final String geminiApiKey;
    private final ObjectMapper objectMapper;
    private final String emailFrom;

    @Autowired
    public SummaryService(SummaryRepository summaryRepository,
                          JavaMailSender mailSender,
                          WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.key}") String geminiApiKey,
                          @Value("${gemini.api.url}") String geminiApiUrl, ObjectMapper objectMapper,
                           @Value("${spring.mail.username}") String emailFrom) {

        this.summaryRepository = summaryRepository;
        this.mailSender = mailSender;
        this.webClient = webClientBuilder.baseUrl(geminiApiUrl).build();
        this.geminiApiKey = geminiApiKey;
        this.objectMapper = objectMapper;
        this.emailFrom = emailFrom;
    }

    public Summary generateAndSaveSummary(String transcript, String prompt) {
        String generatedSummary = callAIGenerationService(transcript, prompt);

        Summary summary = new Summary();
        summary.setOriginalTranscript(transcript);
        summary.setPrompt(prompt);
        summary.setGeneratedSummary(generatedSummary);
        summary.setEditedSummary(generatedSummary);
        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());

        return summaryRepository.save(summary);
    }

    private String callAIGenerationService(String transcript, String prompt) {
        String fullPrompt =
                "You are an expert summarizer. Read the following transcript carefully and produce a structured, concise summary.\n\n"
                        + "Follow these rules:\n"
                        + "1. Focus on **key decisions** and **action items** only.\n"
                        + "2. Format the summary in **Markdown**.\n"
                        + "3. Use bullet points where the **leading text is bold**, followed by a clear and concise description. Example:\n"
                        + "   - **Decision:** The team agreed to migrate the backend to AWS.\n"
                        + "   - **Action Item:** Sarah will prepare the migration plan by Friday.\n"
                        + "4. If no key decisions or action items exist, clearly state: *\"No key decisions or action items were identified.\"*\n"
                        + "5. Remove filler content, greetings, or irrelevant talk.\n"
                        + "6. If the user provides a custom instruction, adapt the summary to match while still keeping Markdown formatting.\n\n"
                        + "User Instruction (if any): " + prompt + "\n\n"
                        + "Transcript:\n" + transcript;

        Map<String, Object> requestPayload = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", fullPrompt)
                                }
                        )
                }
        );

        try {
            String responseBody = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build())
                    .body(BodyInserters.fromValue(requestPayload))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                JsonNode candidate = root.get("candidates").get(0);
                if (candidate.has("content") && candidate.get("content").has("parts") && candidate.get("content").get("parts").isArray() && candidate.get("content").get("parts").size() > 0) {
                    JsonNode part = candidate.get("content").get("parts").get(0);
                    if (part.has("text")) {
                        return part.get("text").asText();
                    }
                }
            }

            return "Failed to generate summary: Invalid API response format.";
        } catch (Exception e) {
            System.err.println("Error calling Gemini API or parsing response: " + e.getMessage());
            return "Failed to generate summary: " + e.getMessage();
        }
    }

    public Optional<Summary> findById(String id) {
        return summaryRepository.findById(id);
    }

    public Summary updateSummary(String id, String editedSummary) {
        return summaryRepository.findById(id).map(summary -> {
            summary.setEditedSummary(editedSummary);
            summary.setUpdatedAt(LocalDateTime.now());
            return summaryRepository.save(summary);
        }).orElseThrow(() -> new RuntimeException("Summary not found with id " + id));
    }

    public void shareSummary(String id, String recipientEmail) {
        Optional<Summary> summaryOpt = summaryRepository.findById(id);
        if (summaryOpt.isPresent()) {
            Summary summary = summaryOpt.get();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(recipientEmail);
            message.setSubject("Summary from AI Notes App");
            message.setText("Hello,\n\nHere is the summary of the meeting notes:\n\n" +
                    summary.getEditedSummary() +
                    "\n\nBest regards,\nAI Notes App");
            mailSender.send(message);
        } else {
            throw new RuntimeException("Summary not found with id " + id);
        }
    }
}
