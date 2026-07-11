package com.multiplier.selfappraisal.dto.openai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal representation of the OpenAI Chat Completions request body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature,
        @JsonProperty("response_format") Map<String, String> responseFormat
) {

    /** Convenience factory that requests a JSON object response. */
    public static ChatCompletionRequest json(String model, List<Message> messages, Double temperature) {
        return new ChatCompletionRequest(model, messages, temperature, Map.of("type", "json_object"));
    }

    public record Message(String role, String content) {
    }
}

