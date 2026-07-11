package com.multiplier.selfappraisal.dto.openai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured result the LLM returns as a JSON object. Parsed from the assistant
 * message content when JSON mode is used.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppraisalResult(
        List<MetricEval> metrics,
        Double overallScore,
        String overallRating,
        String summary
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricEval(
            String key,
            Double score,
            String narrative
    ) {
    }
}

