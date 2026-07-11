package com.multiplier.selfappraisal.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Payload the frontend sends to {@code POST /recommend}.
 *
 * @param name          employee name
 * @param title         designation/title (e.g. "Senior Software Engineer")
 * @param track         metric track key from static config (e.g. "engineering")
 * @param metricKeys    optional subset of metric keys to focus on; empty means all
 *                      metrics configured for the track
 * @param projects      free-text description of projects the employee worked on
 */
public record RecommendRequest(
        @NotBlank String name,
        @NotBlank String title,
        @NotBlank String track,
        List<String> metricKeys,
        @NotEmpty String projects
) {
}

