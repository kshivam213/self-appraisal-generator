package com.multiplier.selfappraisal.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multiplier.selfappraisal.config.AppraisalProperties.Metric;
import com.multiplier.selfappraisal.dto.MetricScore;
import com.multiplier.selfappraisal.dto.RecommendRequest;
import com.multiplier.selfappraisal.dto.RecommendResponse;
import com.multiplier.selfappraisal.dto.openai.AppraisalResult;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrates a self-appraisal recommendation: resolves the metrics from static
 * config, builds the prompt from the user inputs, calls OpenAI in JSON mode, and
 * maps the structured result (scores + narratives + overall rating).
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant that helps employees write a strong, honest, and \
            professional self-appraisal AND evaluate their performance. Ground \
            everything strictly in the projects the employee describes; do not \
            invent achievements. For each evaluation metric provided, write a \
            first-person narrative of 2-4 sentences and assign a score from 1 to 5 \
            (1 = poor, 3 = meets expectations, 5 = outstanding), using one decimal \
            place. Then compute an overall score (average of the metric scores) and \
            an overall rating chosen from exactly one of: "Needs Improvement", \
            "Meets Expectations", "Exceeds Expectations", "Outstanding". Also write \
            a 1-2 sentence overall summary.

            Respond ONLY with a JSON object of this exact shape:
            {
              "metrics": [{"key": "<metric key>", "score": <number>, "narrative": "<text>"}],
              "overallScore": <number>,
              "overallRating": "<one of the four ratings>",
              "summary": "<text>"
            }""";

    private final MetricCatalog metricCatalog;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public RecommendResponse recommend(RecommendRequest request) {
        List<Metric> metrics = metricCatalog.resolve(request.track(), request.metricKeys());

        String userPrompt = buildUserPrompt(request, metrics);
        String json = openAiClient.completeJson(SYSTEM_PROMPT, userPrompt);

        AppraisalResult result = parse(json);
        return mapToResponse(request, metrics, result);
    }

    private AppraisalResult parse(String json) {
        try {
            return objectMapper.readValue(json, AppraisalResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse the model's JSON response", e);
        }
    }

    private RecommendResponse mapToResponse(RecommendRequest request, List<Metric> metrics,
                                            AppraisalResult result) {
        Map<String, AppraisalResult.MetricEval> byKey = new LinkedHashMap<>();
        if (result.metrics() != null) {
            for (AppraisalResult.MetricEval m : result.metrics()) {
                if (m != null && m.key() != null) {
                    byKey.put(m.key(), m);
                }
            }
        }

        List<MetricScore> scores = metrics.stream().map(metric -> {
            AppraisalResult.MetricEval eval = byKey.get(metric.getKey());
            double score = eval != null && eval.score() != null ? eval.score() : 0d;
            String narrative = eval != null && eval.narrative() != null ? eval.narrative() : "";
            return new MetricScore(metric.getKey(), metric.getName(), score, narrative);
        }).toList();

        double overall = result.overallScore() != null
                ? result.overallScore()
                : scores.stream().mapToDouble(MetricScore::score).average().orElse(0d);

        String recommendation = scores.stream()
                .map(s -> "**" + s.name() + ":**\n" + s.narrative())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("");

        return new RecommendResponse(
                request.name(),
                request.title(),
                request.track(),
                metrics.stream().map(Metric::getKey).toList(),
                scores,
                Math.round(overall * 10.0) / 10.0,
                result.overallRating(),
                result.summary(),
                recommendation
        );
    }

    private String buildUserPrompt(RecommendRequest request, List<Metric> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee name: ").append(request.name()).append('\n');
        sb.append("Title / designation: ").append(request.title()).append('\n');
        sb.append("Track: ").append(request.track()).append("\n\n");

        sb.append("Evaluation metrics (use the exact key for each in your JSON):\n");
        for (Metric metric : metrics) {
            sb.append("- key: ").append(metric.getKey())
                    .append(" | name: ").append(metric.getName());
            if (metric.getDescription() != null && !metric.getDescription().isBlank()) {
                sb.append(" | ").append(metric.getDescription());
            }
            sb.append('\n');
        }

        sb.append("\nProjects worked on (free text provided by the employee):\n");
        sb.append(request.projects().trim()).append('\n');

        sb.append("\nScore and write a narrative for every metric above, then provide the ");
        sb.append("overall score, overall rating, and summary. Return only the JSON object.");
        return sb.toString();
    }
}

