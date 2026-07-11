package com.multiplier.selfappraisal.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.multiplier.selfappraisal.config.AppraisalProperties;
import com.multiplier.selfappraisal.config.AppraisalProperties.Metric;

import lombok.RequiredArgsConstructor;

/**
 * Resolves metrics from the static configuration for a given track, optionally
 * filtered to a requested subset of metric keys.
 */
@Service
@RequiredArgsConstructor
public class MetricCatalog {

    private final AppraisalProperties properties;

    /**
     * @return all metrics configured for the track, or empty if the track is unknown.
     */
    public List<Metric> metricsForTrack(String track) {
        return properties.getMetrics().getOrDefault(track, List.of());
    }

    /**
     * Resolves the metrics to evaluate. When {@code requestedKeys} is null/empty
     * all metrics for the track are returned; otherwise only the matching keys.
     */
    public List<Metric> resolve(String track, List<String> requestedKeys) {
        List<Metric> all = metricsForTrack(track);
        if (all.isEmpty()) {
            throw new IllegalArgumentException("Unknown metric track: " + track);
        }
        if (requestedKeys == null || requestedKeys.isEmpty()) {
            return all;
        }
        Set<String> wanted = requestedKeys.stream()
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toSet());
        List<Metric> filtered = all.stream()
                .filter(m -> wanted.contains(m.getKey()))
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    "None of the requested metric keys are configured for track: " + track);
        }
        return filtered;
    }

    public Map<String, List<Metric>> allTracks() {
        return properties.getMetrics();
    }
}

