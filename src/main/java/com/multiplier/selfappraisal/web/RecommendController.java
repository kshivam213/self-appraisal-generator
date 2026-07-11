package com.multiplier.selfappraisal.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.multiplier.selfappraisal.config.AppraisalProperties.Metric;
import com.multiplier.selfappraisal.dto.RecommendRequest;
import com.multiplier.selfappraisal.dto.RecommendResponse;
import com.multiplier.selfappraisal.service.MetricCatalog;
import com.multiplier.selfappraisal.service.RecommendationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints the frontend calls to fetch the metric catalog and to generate a
 * self-appraisal recommendation.
 */
@RestController
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendationService recommendationService;
    private final MetricCatalog metricCatalog;

    /**
     * Generates an AI self-appraisal recommendation from the supplied inputs.
     */
    @PostMapping("/recommend")
    public RecommendResponse recommend(@Valid @RequestBody RecommendRequest request) {
        return recommendationService.recommend(request);
    }

    /**
     * Exposes the full static metric catalog (all tracks).
     */
    @GetMapping("/metrics")
    public Map<String, List<Metric>> metrics() {
        return metricCatalog.allTracks();
    }

    /**
     * Exposes the static metrics for a single track (e.g. "engineering").
     */
    @GetMapping("/metrics/{track}")
    public List<Metric> metricsForTrack(@PathVariable String track) {
        return metricCatalog.metricsForTrack(track);
    }
}

