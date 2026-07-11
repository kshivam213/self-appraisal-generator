package com.multiplier.selfappraisal.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Binds the {@code appraisal.*} configuration, including OpenAI connection
 * settings and the static, per-designation metric catalog.
 */
@Data
@ConfigurationProperties(prefix = "appraisal")
public class AppraisalProperties {

    private OpenAi openai = new OpenAi();

    /**
     * Static metric catalog keyed by designation/track (e.g. "engineering").
     */
    private Map<String, List<Metric>> metrics = Map.of();

    @Data
    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private long timeoutMs = 30000;
        private String defaultModel = "gpt-4o-mini";
    }

    @Data
    public static class Metric {
        private String key;
        private String name;
        private String description;
    }
}

