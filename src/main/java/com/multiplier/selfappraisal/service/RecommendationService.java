package com.multiplier.selfappraisal.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.multiplier.selfappraisal.config.AppraisalProperties.Metric;
import com.multiplier.selfappraisal.dto.RecommendRequest;
import com.multiplier.selfappraisal.dto.RecommendResponse;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates a self-appraisal recommendation: resolves the metrics from static
 * config, builds the prompt from the user inputs, and calls OpenAI.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant that helps employees write a strong, honest, and \
            professional self-appraisal. Given the employee's role and the projects \
            they describe, produce a self-appraisal recommendation organized by the \
            evaluation metrics provided. For each metric, write 2-4 sentences in the \
            first person, grounded strictly in the projects the employee described. \
            Do not invent achievements. Keep the tone confident but factual.""";

    private final MetricCatalog metricCatalog;
    private final OpenAiClient openAiClient;

    public RecommendResponse recommend(RecommendRequest request) {
        List<Metric> metrics = metricCatalog.resolve(request.track(), request.metricKeys());

        String userPrompt = buildUserPrompt(request, metrics);
        String recommendation = openAiClient.complete(SYSTEM_PROMPT, userPrompt);

        List<String> evaluatedKeys = metrics.stream().map(Metric::getKey).toList();
        return new RecommendResponse(
                request.name(),
                request.title(),
                request.track(),
                evaluatedKeys,
                recommendation
        );
    }

    private String buildUserPrompt(RecommendRequest request, List<Metric> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee name: ").append(request.name()).append('\n');
        sb.append("Title / designation: ").append(request.title()).append('\n');
        sb.append("Track: ").append(request.track()).append("\n\n");

        sb.append("Evaluation metrics:\n");
        for (Metric metric : metrics) {
            sb.append("- ").append(metric.getName());
            if (metric.getDescription() != null && !metric.getDescription().isBlank()) {
                sb.append(": ").append(metric.getDescription());
            }
            sb.append('\n');
        }

        sb.append("\nProjects worked on (free text provided by the employee):\n");
        sb.append(request.projects().trim()).append('\n');

        sb.append("\nWrite the self-appraisal recommendation, with one clearly labeled ");
        sb.append("section per metric above.");
        return sb.toString();
    }
}

