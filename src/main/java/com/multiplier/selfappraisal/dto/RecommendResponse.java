package com.multiplier.selfappraisal.dto;

import java.util.List;

/**
 * Response returned by {@code POST /recommend}.
 *
 * @param name          echo of the employee name
 * @param title         echo of the designation/title
 * @param track         metric track used
 * @param metrics       the metric keys that were evaluated
 * @param recommendation the AI-generated self-appraisal recommendation text
 */
public record RecommendResponse(
        String name,
        String title,
        String track,
        List<String> metrics,
        String recommendation
) {
}

