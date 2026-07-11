package com.multiplier.selfappraisal.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

/**
 * Wires the {@link WebClient} used to talk to the OpenAI API with the base URL,
 * auth header, and timeouts sourced from {@link AppraisalProperties}.
 */
@Configuration
@EnableConfigurationProperties(AppraisalProperties.class)
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(AppraisalProperties properties) {
        AppraisalProperties.OpenAi openai = properties.getOpenai();

        int timeoutMs = (int) openai.getTimeoutMs();
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutMs / 1000)));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(openai.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient));

        if (openai.getApiKey() != null && !openai.getApiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openai.getApiKey());
        }

        return builder.build();
    }
}

