package com.multiplier.selfappraisal.dto;

import java.util.List;

/**
 * Response returned by {@code POST /recommend}.
 *
 * @param name           echo of the employee name
 * @param title          echo of the designation/title
 * @param track          metric track used
 * @param metrics        the metric keys that were evaluated
 * @param metricScores   per-metric score + narrative
 * @param overallScore   overall score on a 1-5 scale
 * @param overallRating  qualitative overall rating (e.g. "Exceeds Expectations")
 * @param summary        short overall summary of the appraisal
 * @param recommendation the full self-appraisal text (narratives combined)
 */
public record RecommendResponse(
        String name,
        String title,
        String track,
        List<String> metrics,
        List<MetricScore> metricScores,
        double overallScore,
        String overallRating,
        String summary,
        String recommendation
) {
}

