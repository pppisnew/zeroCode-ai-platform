package com.zerocode.deploy.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HttpGithubActionsClient implements GithubActionsClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpGithubActionsClient(ObjectMapper objectMapper) {
        this(objectMapper, HttpClient.newHttpClient());
    }

    HttpGithubActionsClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public GithubActionsDispatchResult dispatch(
            GithubActionsDispatchCommand command) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of(
                "ref", command.ref(),
                "inputs", command.inputs()));
        HttpRequest request = HttpRequest.newBuilder(endpoint(command))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + command.token())
                .header("Content-Type", "application/json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new GithubActionsDispatchResult(response.statusCode(), response.body());
    }

    private URI endpoint(GithubActionsDispatchCommand command) {
        String baseUrl = trimTrailingSlash(command.apiBaseUrl());
        return URI.create(baseUrl
                + "/repos/"
                + encode(command.owner())
                + "/"
                + encode(command.repo())
                + "/actions/workflows/"
                + encode(command.workflowId())
                + "/dispatches");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
