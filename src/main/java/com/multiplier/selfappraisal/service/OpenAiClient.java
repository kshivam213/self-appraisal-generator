package com.multiplier.selfappraisal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.multiplier.selfappraisal.config.AppraisalProperties;
import com.multiplier.selfappraisal.dto.openai.ChatCompletionRequest;
import com.multiplier.selfappraisal.dto.openai.ChatCompletionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around the OpenAI Chat Completions endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final AppraisalProperties properties;

    /**
     * Sends the system + user prompt to OpenAI and returns the assistant reply.
     */
    public String complete(String systemPrompt, String userPrompt) {
        return send(systemPrompt, userPrompt, false);
    }

    /**
     * Same as {@link #complete} but instructs the model to reply with a JSON object.
     */
    public String completeJson(String systemPrompt, String userPrompt) {
        return send(systemPrompt, userPrompt, true);
    }

    private String send(String systemPrompt, String userPrompt, boolean json) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set the OPENAI_API_KEY environment variable.");
        }

        List<ChatCompletionRequest.Message> messages = List.of(
                new ChatCompletionRequest.Message("system", systemPrompt),
                new ChatCompletionRequest.Message("user", userPrompt)
        );
        String model = properties.getOpenai().getDefaultModel();
        ChatCompletionRequest request = json
                ? ChatCompletionRequest.json(model, messages, 0.7)
                : new ChatCompletionRequest(model, messages, 0.7, null);

        try {
            ChatCompletionResponse response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("OpenAI returned an empty response");
            }
            return response.choices().get(0).message().content().trim();
        } catch (WebClientResponseException ex) {
            log.error("OpenAI request failed: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new IllegalStateException(
                    "OpenAI request failed with status " + ex.getStatusCode(), ex);
        }
    }
}

