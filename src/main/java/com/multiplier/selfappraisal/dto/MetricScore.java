package com.multiplier.selfappraisal.dto;

/**
 * A single evaluated metric: its key, display name, LLM-assigned score, and the
 * narrative justification for that score.
 *
 * @param key       metric key from static config
 * @param name      metric display name
 * @param score     score on a 1-5 scale
 * @param narrative first-person justification grounded in the described projects
 */
public record MetricScore(
        String key,
        String name,
        double score,
        String narrative
) {
}

