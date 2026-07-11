package com.multiplier.selfappraisal.dto.openai;

import java.util.List;

/**
 * Minimal representation of the OpenAI Chat Completions request body.
 */
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature
) {

    public record Message(String role, String content) {
    }
}

